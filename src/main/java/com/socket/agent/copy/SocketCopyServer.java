package com.socket.agent.copy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.socket.agent.model.SocketCopy;
import com.socket.agent.model.SocketCopySocket;
import com.socket.agent.util.CopyToTask;

/**
 * Hello world!
 */
public class SocketCopyServer {
    private int localPort;
    private List<SocketCopy> copyTo = new ArrayList<SocketCopy>();

    private final static Logger logger = LoggerFactory.getLogger(SocketCopyServer.class);

    public synchronized void start() {
        try {
            ServerSocket server = new ServerSocket(localPort);
            while (true) {
                logger.error("listening on " + localPort);
                final Socket socket = server.accept();
                new Thread(new Runnable() {

                    public void run() {
                        List<SocketCopySocket> toSockets = new ArrayList<SocketCopySocket>();
                        for (SocketCopy socketCopy : copyTo) {
                            try {
                                Socket copyToSocket = connect(socketCopy.getCopyTo());
                                toSockets.add(new SocketCopySocket(socketCopy.isPrimary(), copyToSocket));
                            } catch (IOException e) {
                                logger.error("connect to " + socketCopy.getCopyTo() + " failed , " + e.getMessage(), e);
                                continue;
                            }
                            new CopyToTask(socket, toSockets).run();
                        }
                    }

                    private Socket connect(String copyTo) throws IOException {
                        Socket forwardToSocket;
                        forwardToSocket = new Socket();
                        logger.info("connecting to " + copyTo);
                        forwardToSocket.connect(new InetSocketAddress(copyTo.split(":")[0], Integer.parseInt(copyTo.split(":")[1])), 5000);
                        logger.info("connected to " + copyTo);
                        forwardToSocket.setSoTimeout(60000);
                        return forwardToSocket;
                    }
                }).start();
            }
        } catch (IOException e) {
            logger.error("listen on " + localPort + " failed", e);
        }
    }

}
