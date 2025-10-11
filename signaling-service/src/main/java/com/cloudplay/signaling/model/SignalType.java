package com.cloudplay.signaling.model;

/**
 * WebRTC signaling message types exchanged over the WebSocket.
 */
public enum SignalType {

    /** First message from either peer: identifies the session and role. */
    JOIN,

    /** Server -> peer: confirms join and reports whether the other peer is present. */
    JOINED,

    /** SDP offer (typically streamer -> player or player -> streamer). */
    OFFER,

    /** SDP answer in response to an OFFER. */
    ANSWER,

    /** Trickle ICE candidate. */
    ICE_CANDIDATE,

    /** Either peer signalling the media session is up. */
    CONNECTED,

    /** Graceful leave / teardown. */
    LEAVE,

    /** Server -> peer: error envelope. */
    ERROR
}
