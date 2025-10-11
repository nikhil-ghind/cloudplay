package com.cloudplay.signaling.model;

/**
 * The two roles in a CloudPlay WebRTC session.
 */
public enum PeerRole {

    /** The end user's browser / client receiving the video stream. */
    PLAYER,

    /** The GPU node process producing the encoded game video. */
    STREAMER;

    public PeerRole other() {
        return this == PLAYER ? STREAMER : PLAYER;
    }
}
