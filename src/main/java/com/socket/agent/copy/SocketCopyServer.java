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
import com.socket.agent.util.SocketCallback;
import com.socket.agent.util.TransferUtils;

/**
 * Hello world!
 */
public class SocketCopyServer {
    private int localPort;
    private List<SocketCopy> copyTo = new ArrayList<SocketCopy>();
    private int sourceTimeout;
    private int forwardTimeout;

    private final static Logger logger = LoggerFactory.getLogger(SocketCopyServer.class);

    public synchronized void start() {
        new Thread(new Runnable() {

            public void run() {
                try {
                    ServerSocket server = new ServerSocket(localPort);
                    logger.info("listening on " + localPort);
                    while (true) {
                        final Socket srcSocket = server.accept();
                        logger.info("accepted " + srcSocket);
                        srcSocket.setSoTimeout(sourceTimeout);
                        new Thread(new Runnable() {

                            public void run() {
                                TransferUtils.addSocket(srcSocket, new SocketCallback() {

                                    public void dataReceived(Socket socket, byte[] data) {
                                        if (!socket.equals(srcSocket)) {
                                            // 不是源socket
                                            return;
                                        }
                                        List<SocketCopySocket> toSockets = new ArrayList<SocketCopySocket>();
                                        for (SocketCopy socketCopy : copyTo) {
                                            if (TransferUtils.isToAddrClosed(srcSocket, socketCopy.getCopyTo())) {
                                                try {
                                                    Socket copyToSocket = connect(socketCopy.getCopyTo());
                                                    toSockets.add(new SocketCopySocket(socketCopy.isPrimary(), copyToSocket));
                                                } catch (IOException e) {
                                                    logger.error("connect to " + socketCopy.getCopyTo() + " failed , " + e.getMessage());
                                                    continue;
                                                }
                                            }
                                        }
                                        // 放入接收端的socket
                                        TransferUtils.addSocket(srcSocket, this, toSockets);
                                    }
                                }, (SocketCopySocket) null);
                            }

                            private Socket connect(String copyTo) throws IOException {
                                Socket forwardToSocket;
                                forwardToSocket = new Socket();
                                logger.info("connecting to " + copyTo);
                                forwardToSocket.connect(new InetSocketAddress(copyTo.split(":")[0], Integer.parseInt(copyTo.split(":")[1])), 5000);
                                logger.info("connected to " + forwardToSocket.getRemoteSocketAddress());
                                forwardToSocket.setSoTimeout(forwardTimeout);
                                return forwardToSocket;
                            }
                        }, "SocketCopyServer-connect").start();
                    }
                } catch (IOException e) {
                    logger.error("listen on " + localPort + " failed", e);
                } catch (Throwable e) {
                    logger.error("unknown error " + localPort + ", " + e.getMessage(), e);
                }
            }
        }, "SocketCopyServer-" + localPort).start();
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public List<SocketCopy> getCopyTo() {
        return copyTo;
    }

    public void setCopyTo(List<SocketCopy> copyTo) {
        this.copyTo = copyTo;
    }

    public int getSourceTimeout() {
        return sourceTimeout;
    }

    public void setSourceTimeout(int sourceTimeout) {
        this.sourceTimeout = sourceTimeout;
    }

    public int getForwardTimeout() {
        return forwardTimeout;
    }

    public void setForwardTimeout(int forwardTimeout) {
        this.forwardTimeout = forwardTimeout;
    }

}
