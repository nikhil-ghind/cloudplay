package com.cloudplay.signaling.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Envelope for all signaling traffic. Carries the SDP payload for OFFER/ANSWER
 * or the candidate fields for ICE_CANDIDATE. Unused fields are omitted from the
 * wire form.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SignalMessage {

    private SignalType type;
    private String sessionId;
    private PeerRole role;

    /** SDP blob for OFFER / ANSWER. */
    private String sdp;

    /** ICE candidate fields for ICE_CANDIDATE. */
    private String candidate;
    private String sdpMid;
    private Integer sdpMLineIndex;

    /** Free-form reason for ERROR / LEAVE. */
    private String reason;

    /** Set on JOINED so a peer knows whether to initiate the offer. */
    private Boolean peerPresent;

    public SignalMessage() {
    }

    public static SignalMessage error(String sessionId, String reason) {
        SignalMessage m = new SignalMessage();
        m.type = SignalType.ERROR;
        m.sessionId = sessionId;
        m.reason = reason;
        return m;
    }

    public static SignalMessage joined(String sessionId, PeerRole role, boolean peerPresent) {
        SignalMessage m = new SignalMessage();
        m.type = SignalType.JOINED;
        m.sessionId = sessionId;
        m.role = role;
        m.peerPresent = peerPresent;
        return m;
    }

    public SignalType getType() {
        return type;
    }

    public void setType(SignalType type) {
        this.type = type;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public PeerRole getRole() {
        return role;
    }

    public void setRole(PeerRole role) {
        this.role = role;
    }

    public String getSdp() {
        return sdp;
    }

    public void setSdp(String sdp) {
        this.sdp = sdp;
    }

    public String getCandidate() {
        return candidate;
    }

    public void setCandidate(String candidate) {
        this.candidate = candidate;
    }

    public String getSdpMid() {
        return sdpMid;
    }

    public void setSdpMid(String sdpMid) {
        this.sdpMid = sdpMid;
    }

    public Integer getSdpMLineIndex() {
        return sdpMLineIndex;
    }

    public void setSdpMLineIndex(Integer sdpMLineIndex) {
        this.sdpMLineIndex = sdpMLineIndex;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Boolean getPeerPresent() {
        return peerPresent;
    }

    public void setPeerPresent(Boolean peerPresent) {
        this.peerPresent = peerPresent;
    }
}
