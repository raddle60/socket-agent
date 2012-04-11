package com.socket.agent;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 *
 */
public class SocketAgentMain {
    private final static Logger logger = LoggerFactory.getLogger(SocketAgentServer.class);

    public static void main(String[] args) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(new File("socket-agent.properties")));
        } catch (Exception e) {
            logger.error("load " + new File("socket-agent.properties").getAbsolutePath() + " failed");
            return;
        }
        SocketAgentServer server = new SocketAgentServer(properties);
        server.start();
    }
}
