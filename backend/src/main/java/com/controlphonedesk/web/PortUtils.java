package com.controlphonedesk.web;

import java.io.IOException;
import java.net.ServerSocket;

public final class PortUtils {
    private PortUtils() {
    }

    public static int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        }
    }
}
