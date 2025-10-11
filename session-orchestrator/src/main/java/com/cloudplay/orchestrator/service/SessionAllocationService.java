package com.cloudplay.orchestrator.service;

import com.cloudplay.common.dto.SessionRequest;
import com.cloudplay.common.error.CommonExceptions;
import com.cloudplay.common.model.GameSession;
import com.cloudplay.common.model.GpuNode;
import com.cloudplay.common.model.SessionState;
import com.cloudplay.common.registry.RegistryClient;
import com.cloudplay.common.util.Ids;
import com.cloudplay.orchestrator.config.OrchestratorProperties;
import com.cloudplay.orchestrator.scheduler.NodeScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the full session lifecycle: allocate -> reserve slot -> activate,
 * and terminate -> release slot. Capacity is owned by the registry; this service
 * coordinates the steps and persists session records locally.
 */
@Service
public class SessionAllocationService {

    private static final Logger log = LoggerFactory.getLogger(SessionAllocationService.class);

    private final RegistryClient registry;
    private final NodeScheduler scheduler;
    private final SessionRepository repository;
    private final OrchestratorProperties props;

    public SessionAllocationService(RegistryClient registry,
                                    NodeScheduler scheduler,
                                    SessionRepository repository,
                                    OrchestratorProperties props) {
        this.registry = registry;
        this.scheduler = scheduler;
        this.repository = repository;
        this.props = props;
    }

    /**
     * Allocate a session. Synchronously queries the registry for candidate nodes,
     * ranks them, then tries to atomically reserve a slot — retrying down the
     * ranked list when a CAS race is lost — and activates the session.
     */
    public GameSession allocate(SessionRequest request) {
        Instant now = Instant.now();
        GameSession session = new GameSession();
        session.setSessionId(Ids.sessionId());
        session.setUserId(request.getUserId());
        session.setGameId(request.getGameId());
        session.setRegion(request.getRegion());
        session.setGpuClass(request.getGpuClass());
        session.setState(SessionState.PENDING);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        repository.save(session);

        // Block on the reactive registry calls — allocation is request-scoped and
        // the caller (gateway) expects a synchronous decision.
        List<GpuNode> candidates = registry
                .listSchedulableNodes(request.getRegion(), request.getGpuClass())
                .collectList()
                .block();
        if (candidates == null) {
            candidates = List.of();
        }

        List<GpuNode> ranked = new ArrayList<>(scheduler.rank(request, candidates));
        if (ranked.isEmpty()) {
            return fail(session, "No schedulable node in region="
                    + request.getRegion() + " gpuClass=" + request.getGpuClass());
        }

        int attempts = 0;
        for (GpuNode node : ranked) {
            if (attempts++ >= props.getMaxAllocationAttempts()) {
                break;
            }
            try {
                session.setState(SessionState.ALLOCATING);
                session.setNodeId(node.getNodeId());
                session.setUpdatedAt(Instant.now());
                repository.save(session);

                GpuNode reserved = registry.reserveSlot(node.getNodeId(), session.getSessionId()).block();
                if (reserved == null) {
                    continue;
                }
                return activate(session, reserved);
            } catch (CommonExceptions.SlotConflictException conflict) {
                log.info("Slot conflict on {} for {}, trying next candidate",
                        node.getNodeId(), session.getSessionId());
            } catch (CommonExceptions.NodeNotFoundException gone) {
                log.info("Node {} disappeared during allocation of {}, trying next",
                        node.getNodeId(), session.getSessionId());
            }
        }

        return fail(session, "Could not reserve a slot after " + attempts + " attempt(s)");
    }

    private GameSession activate(GameSession session, GpuNode node) {
        session.setState(SessionState.ACTIVE);
        session.setNodeId(node.getNodeId());
        session.setStreamerEndpoint(node.getStreamerHost() + ":" + node.getStreamerPort());
        session.setStatusReason(null);
        session.setUpdatedAt(Instant.now());
        repository.save(session);
        log.info("Session {} ACTIVE on node {} ({})",
                session.getSessionId(), node.getNodeId(), session.getStreamerEndpoint());
        return session;
    }

    private GameSession fail(GameSession session, String reason) {
        session.setState(SessionState.FAILED);
        session.setStatusReason(reason);
        session.setUpdatedAt(Instant.now());
        repository.save(session);
        log.warn("Session {} FAILED: {}", session.getSessionId(), reason);
        throw new CommonExceptions.NoCapacityException(session.getRegion(),
                String.valueOf(session.getGpuClass()));
    }

    public GameSession get(String sessionId) {
        return repository.find(sessionId)
                .orElseThrow(() -> new CommonExceptions.SessionNotFoundException(sessionId));
    }

    public List<GameSession> list() {
        return new ArrayList<>(repository.findAll());
    }

    /** Gracefully terminate a session and release its slot back to the registry. */
    public GameSession terminate(String sessionId) {
        GameSession session = get(sessionId);
        if (session.getState() != null && session.getState().isTerminal()) {
            return session;
        }
        session.setState(SessionState.TERMINATING);
        session.setUpdatedAt(Instant.now());
        repository.save(session);

        if (session.getNodeId() != null) {
            try {
                registry.releaseSlot(session.getNodeId(), session.getSessionId()).block();
            } catch (CommonExceptions.NodeNotFoundException ignored) {
                // Node already gone; slot is implicitly released.
            }
        }

        session.setState(SessionState.TERMINATED);
        session.setStreamerEndpoint(null);
        session.setUpdatedAt(Instant.now());
        repository.save(session);
        log.info("Session {} TERMINATED (node {})", sessionId, session.getNodeId());
        return session;
    }
}
