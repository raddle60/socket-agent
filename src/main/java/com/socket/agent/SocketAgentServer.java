package com.socket.agent;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.socket.agent.model.SocketCopySocket;
import com.socket.agent.util.SocketCallback;
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
                    while (true) {
                        try {
                            final Socket srcSocket = server.accept();
                            srcSocket.setSoTimeout(Integer.parseInt(properties.getProperty("so.timeout", "60000")));
                            logger.info("accepted socket :" + srcSocket.getRemoteSocketAddress());
                            TransferUtils.addSocket(srcSocket, new SocketCallback() {

                                public void dataReceived(Socket socket, byte[] data) {
                                    if (!socket.equals(srcSocket)) {
                                        // 不是源socket
                                        return;
                                    }
                                    if (TransferUtils.isToAddrClosed(srcSocket, properties.getProperty("dest.ip") + ":" + properties.getProperty("dest.port"))) {
                                        try {
                                            Socket forwardToSocket = connectForward();
                                            TransferUtils.addSocket(socket, this, new SocketCopySocket(true, forwardToSocket));
                                        } catch (IOException e) {
                                            logger.error("connect to " + properties.getProperty("dest.ip") + ":" + properties.getProperty("dest.port") + " failed , " + e.getMessage());
                                        }
                                    }
                                }
                            }, (SocketCopySocket) null);
                        } catch (IOException e) {
                            logger.error("accept socket failed", e);
                            return;
                        }
                    }
                }
            }, "SocketAgentServer-" + properties.getProperty("local.port"));
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
