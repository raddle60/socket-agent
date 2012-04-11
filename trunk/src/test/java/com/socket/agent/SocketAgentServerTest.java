package com.socket.agent;

import java.util.Properties;

import org.junit.Test;

public class SocketAgentServerTest {

    @Test
    public void testStart() {
        Properties properties = new Properties();
        properties.setProperty("local.port", "9000");
        properties.setProperty("remote.ip", "114.80.132.160");
        properties.setProperty("remote.port", "80");
        SocketAgentServer server = new SocketAgentServer(properties);
        server.start();
    }

}
