package com.socket.agent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.socket.agent.model.SocketCopySocket;
import com.socket.agent.util.TransferUtils;

/**
 * Hello world!
 */
public class SocketAgentServer {

    private final static Logger logger = LoggerFactory.getLogger(SocketAgentServer.class);
    private boolean started = false;
    private Properties properties;
    private Thread thread;

    public SocketAgentServer(Properties properties) {
        this.properties = properties;
    }

    public synchronized void start() {
        if (!started) {
            started = true;
            thread = new Thread(new Runnable() {

                public void run() {
                    logger.info("starting socket agent");
                    ServerSocket server = null;
                    try {
                        int port = Integer.parseInt(properties.getProperty("local.port"));
                        server = new ServerSocket(port);
                        int destPort = Integer.parseInt(properties.getProperty("dest.port"));
                        String destIp = properties.getProperty("dest.ip");
                        logger.info("socket agent listening on " + port + " agent for " + destIp + ":" + destPort);
                    } catch (IOException e) {
                        logger.error("starting socket agent failed", e);
                        return;
                    }
                    Socket socket = null;
                    while (true) {
                        try {
                            socket = server.accept();
                            socket.setSoTimeout(60000);
                            logger.info("accepted socket :" + socket.getRemoteSocketAddress());
                            try {
                                Socket forwardToSocket = connectForward();
                                TransferUtils.addSocket(socket, new SocketCopySocket(true, forwardToSocket));
                            } catch (IOException e) {
                                logger.error("connect to forward " + properties.getProperty("dest.ip") + ":" + properties.getProperty("dest.port") + " failed , " + e.getMessage());
                            }
                        } catch (IOException e) {
                            logger.error("accept socket failed", e);
                            return;
                        }
                    }
                }
            });
            thread.start();
        }
    }

    private Socket connectForward() throws NumberFormatException, IOException {
        int destPort = Integer.parseInt(properties.getProperty("dest.port"));
        String destIp = properties.getProperty("dest.ip");
        Socket forwardSocket = new Socket();
        logger.info("connecting to dest " + destIp + ":" + destPort);
        forwardSocket.connect(new InetSocketAddress(destIp, destPort), Integer.parseInt(properties.getProperty("dest.conn.timeout", "5000")));
        logger.info("connected to dest " + forwardSocket.getRemoteSocketAddress());
        int soTimeout = Integer.parseInt(properties.getProperty("so.timeout", "60000"));
        forwardSocket.setSoTimeout(soTimeout);
        return forwardSocket;
    }

}
