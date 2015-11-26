package com.socket.agent;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.socket.agent.copy.SocketCopyServer;
import com.socket.agent.middle.SocketMiddleClient;
import com.socket.agent.middle.SocketMiddleServer;
import com.socket.agent.model.SocketCopy;
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
            if (properties.getProperty("local.port") != null) {
                SocketAgentServer server = new SocketAgentServer(properties);
                server.start();
            }
        } catch (Exception e) {
            logger.error("SocketAgentServer start failed " + e.getMessage(), e);
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
                middleServer.setSoTimeout(Integer.parseInt(properties.getProperty("middleServer.so.timeout", "60000")));
                middleServer.start();
            }
        } catch (Exception e) {
            logger.error("SocketMiddleServer start failed " + e.getMessage(), e);
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
                client.setClientSoTimeout(Integer.parseInt(properties.getProperty("middleClient.forward.so.timeout", "60000")));
                client.setServerSoTimeout(Integer.parseInt(properties.getProperty("middleClient.server.so.timeout", "1800000")));
                client.start();
            }
        } catch (Exception e) {
            logger.error("SocketMiddleClient start failed " + e.getMessage(), e);
        }
        try {
            String socketCopys = properties.getProperty("socketCopys");
            if (socketCopys != null) {
                SocketCopyServer copyServer = new SocketCopyServer();
                String[] split = socketCopys.split("\\|");
                for (String string : split) {
                    String[] split2 = string.split("\\-");
                    int port = Integer.parseInt(split2[0]);
                    copyServer.setLocalPort(port);
                    String[] toaddrs = split2[1].split(",");
                    List<SocketCopy> list = new ArrayList<SocketCopy>();
                    for (String string2 : toaddrs) {
                        list.add(new SocketCopy(string2));
                    }
                    copyServer.setCopyTo(list);
                    copyServer.setSourceTimeout(Integer.parseInt(properties.getProperty("socketCopys.source.so.timeout", "60000")));
                    copyServer.setForwardTimeout(Integer.parseInt(properties.getProperty("socketCopys.forward.so.timeout", "60000")));
                }
                copyServer.start();
            }
        } catch (Exception e) {
            logger.error("SocketCopyServer start failed " + e.getMessage(), e);
        }

    }
}
