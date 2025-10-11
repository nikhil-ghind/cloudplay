package com.cloudplay.orchestrator.service;

import com.cloudplay.common.model.GameSession;
import com.cloudplay.common.model.SessionState;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store of sessions the orchestrator owns. In a production deployment
 * this would be backed by the distributed registry (or a database); kept local
 * here to keep the orchestrator self-contained while the registry tracks node
 * capacity authoritatively.
 */
@Repository
public class SessionRepository {

    private final ConcurrentHashMap<String, GameSession> sessions = new ConcurrentHashMap<>();

    public GameSession save(GameSession session) {
        sessions.put(session.getSessionId(), session);
        return session;
    }

    public Optional<GameSession> find(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public Collection<GameSession> findAll() {
        return sessions.values();
    }

    public void delete(String sessionId) {
        sessions.remove(sessionId);
    }

    /** Sessions that currently occupy (or are claiming) capacity. */
    public List<GameSession> activeSessions() {
        return sessions.values().stream()
                .filter(s -> s.getState() != null && s.getState().consumesCapacity())
                .toList();
    }

    public long countByState(SessionState state) {
        return sessions.values().stream().filter(s -> s.getState() == state).count();
    }
}
