package com.cloudplay.signaling;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the signaling-service. Bridges a player browser and the GPU
 * streamer running on a node by relaying WebRTC SDP offers/answers and ICE
 * candidates over a WebSocket.
 */
@SpringBootApplication
public class SignalingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SignalingServiceApplication.class, args);
    }
}
