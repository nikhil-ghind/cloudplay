package com.cloudplay.signaling.handler;

import com.cloudplay.signaling.model.PeerRole;
import com.cloudplay.signaling.model.SignalMessage;
import com.cloudplay.signaling.model.SignalType;
import com.cloudplay.signaling.service.SignalingSession;
import com.cloudplay.signaling.service.SignalingSessionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

/**
 * Core WebRTC signaling relay. Each peer (player browser or GPU streamer) opens a
 * WebSocket to {@code /signaling?sessionId=...} and:
 *
 * <pre>
 *  1. Sends JOIN with its role (PLAYER or STREAMER).
 *  2. Server replies JOINED, telling it whether the counterpart is already here.
 *  3. The STREAMER (offerer) sends OFFER once both peers are present; the server
 *     relays it to the PLAYER, which replies ANSWER (relayed back).
 *  4. Both peers trickle ICE_CANDIDATE messages, each relayed to the counterpart.
 *  5. CONNECTED / LEAVE are relayed; close tears the peer down.
 * </pre>
 *
 * The signaling-service never inspects or modifies the SDP/ICE payloads — it is a
 * pure relay keyed by sessionId, which keeps media off this control path.
 */
@Component
public class SignalingWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SignalingWebSocketHandler.class);

    public static final String ATTR_SESSION_ID = "cp.sessionId";
    public static final String ATTR_ROLE = "cp.role";

    private final SignalingSessionRegistry registry;
    private final ObjectMapper mapper;

    public SignalingWebSocketHandler(SignalingSessionRegistry registry, ObjectMapper mapper) {
        this.registry = registry;
        this.mapper = mapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession ws) {
        Object sessionId = ws.getAttributes().get(ATTR_SESSION_ID);
        if (sessionId == null) {
            send(ws, SignalMessage.error(null, "missing sessionId query parameter"));
            close(ws, CloseStatus.BAD_DATA);
            return;
        }
        log.debug("WS connection established for sessionId={}", sessionId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession ws, TextMessage textMessage) {
        SignalMessage msg;
        try {
            msg = mapper.readValue(textMessage.getPayload(), SignalMessage.class);
        } catch (Exception e) {
            send(ws, SignalMessage.error(currentSessionId(ws), "malformed signaling message"));
            return;
        }

        String sessionId = (String) ws.getAttributes().get(ATTR_SESSION_ID);
        if (msg.getType() == null) {
            send(ws, SignalMessage.error(sessionId, "missing message type"));
            return;
        }

        switch (msg.getType()) {
            case JOIN -> handleJoin(ws, sessionId, msg);
            case OFFER, ANSWER, ICE_CANDIDATE, CONNECTED -> relay(ws, sessionId, msg);
            case LEAVE -> handleLeave(ws, sessionId);
            default -> send(ws, SignalMessage.error(sessionId,
                    "unexpected message type: " + msg.getType()));
        }
    }

    private void handleJoin(WebSocketSession ws, String sessionId, SignalMessage msg) {
        if (msg.getRole() == null) {
            send(ws, SignalMessage.error(sessionId, "JOIN requires a role"));
            close(ws, CloseStatus.BAD_DATA);
            return;
        }
        PeerRole role = msg.getRole();
        ws.getAttributes().put(ATTR_ROLE, role);

        SignalingSession session = registry.getOrCreate(sessionId);
        session.addPeer(role, ws);
        boolean counterpartPresent = session.hasPeer(role.other());

        send(ws, SignalMessage.joined(sessionId, role, counterpartPresent));
        log.info("Peer {} joined session {} (counterpart present={})",
                role, sessionId, counterpartPresent);

        // Tell the already-connected counterpart that this peer arrived, so the
        // offerer (STREAMER) can start the SDP negotiation.
        if (counterpartPresent) {
            WebSocketSession other = session.counterpart(role);
            send(other, SignalMessage.joined(sessionId, role.other(), true));
        }
    }

    /** Relay OFFER/ANSWER/ICE/CONNECTED to the counterpart peer. */
    private void relay(WebSocketSession ws, String sessionId, SignalMessage msg) {
        PeerRole role = (PeerRole) ws.getAttributes().get(ATTR_ROLE);
        if (role == null) {
            send(ws, SignalMessage.error(sessionId, "must JOIN before sending " + msg.getType()));
            return;
        }
        SignalingSession session = registry.get(sessionId);
        if (session == null) {
            send(ws, SignalMessage.error(sessionId, "no such signaling session"));
            return;
        }
        WebSocketSession counterpart = session.counterpart(role);
        if (counterpart == null || !counterpart.isOpen()) {
            send(ws, SignalMessage.error(sessionId, "counterpart not connected"));
            return;
        }
        // Stamp the originating role so the receiver knows who sent it.
        msg.setSessionId(sessionId);
        msg.setRole(role);
        send(counterpart, msg);
        log.debug("Relayed {} from {} to {} in session {}",
                msg.getType(), role, role.other(), sessionId);
    }

    private void handleLeave(WebSocketSession ws, String sessionId) {
        notifyCounterpartLeave(ws, sessionId);
        close(ws, CloseStatus.NORMAL);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession ws, CloseStatus status) {
        String sessionId = (String) ws.getAttributes().get(ATTR_SESSION_ID);
        PeerRole role = (PeerRole) ws.getAttributes().get(ATTR_ROLE);
        if (sessionId == null || role == null) {
            return;
        }
        notifyCounterpartLeave(ws, sessionId);
        SignalingSession session = registry.get(sessionId);
        if (session != null && session.removePeer(role)) {
            registry.remove(sessionId);
            log.info("Session {} fully closed", sessionId);
        }
        log.debug("Peer {} left session {} ({})", role, sessionId, status);
    }

    private void notifyCounterpartLeave(WebSocketSession ws, String sessionId) {
        PeerRole role = (PeerRole) ws.getAttributes().get(ATTR_ROLE);
        if (role == null) {
            return;
        }
        SignalingSession session = registry.get(sessionId);
        if (session == null) {
            return;
        }
        WebSocketSession counterpart = session.counterpart(role);
        if (counterpart != null && counterpart.isOpen()) {
            SignalMessage leave = new SignalMessage();
            leave.setType(SignalType.LEAVE);
            leave.setSessionId(sessionId);
            leave.setRole(role);
            leave.setReason("counterpart disconnected");
            send(counterpart, leave);
        }
    }

    private String currentSessionId(WebSocketSession ws) {
        Object id = ws.getAttributes().get(ATTR_SESSION_ID);
        return id == null ? null : id.toString();
    }

    private void send(WebSocketSession ws, SignalMessage msg) {
        if (ws == null || !ws.isOpen()) {
            return;
        }
        try {
            synchronized (ws) {
                ws.sendMessage(new TextMessage(mapper.writeValueAsString(msg)));
            }
        } catch (IOException e) {
            log.warn("Failed to send {} to peer: {}", msg.getType(), e.getMessage());
        }
    }

    private void close(WebSocketSession ws, CloseStatus status) {
        try {
            ws.close(status);
        } catch (IOException ignored) {
            // best effort
        }
    }
}
