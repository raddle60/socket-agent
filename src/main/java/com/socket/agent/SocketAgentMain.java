package com.socket.agent;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.socket.agent.middle.SocketMiddleClient;
import com.socket.agent.middle.SocketMiddleServer;
import com.socket.agent.model.SocketMiddle;
import com.socket.agent.model.SocketMiddleFoward;

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
        try {
            SocketAgentServer server = new SocketAgentServer(properties);
            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            String middleServers = properties.getProperty("middleServers");
            if (middleServers != null) {
                String[] split = middleServers.split(",");
                SocketMiddleServer middleServer = new SocketMiddleServer();
                for (String string : split) {
                    String[] split2 = string.split(":");
                    middleServer.getMiddles().add(new SocketMiddle(Integer.parseInt(split2[0]), Integer.parseInt(split2[1])));
                }
                middleServer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            String middleClients = properties.getProperty("middleClients");
            if (middleClients != null) {
                String[] split = middleClients.split("\\|");
                SocketMiddleClient client = new SocketMiddleClient();
                for (String string : split) {
                    String[] split2 = string.split("\\-");
                    client.getMiddles().add(new SocketMiddleFoward(split2[0], split2[1]));
                }
                client.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
