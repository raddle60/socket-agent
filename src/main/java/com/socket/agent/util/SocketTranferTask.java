package com.socket.agent.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketTranferTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(SocketTranferTask.class);
    private Socket srcSocket;
    private List<Socket> toSockets;
    private boolean discardData;

    public SocketTranferTask(Socket srcSocket, List<Socket> toSockets) {
        this.srcSocket = srcSocket;
        this.toSockets = toSockets;
    }

    public SocketTranferTask(Socket srcSocket, Socket toSocket) {
        this.srcSocket = srcSocket;
        List<Socket> s = new ArrayList<Socket>();
        s.add(toSocket);
        this.toSockets = s;
    }

    public SocketTranferTask(Socket srcSocket, Socket toSocket, boolean discardToSocketData) {
        this(srcSocket, toSocket);
        this.discardData = discardToSocketData;
    }

    public void run() {
        try {
            byte[] buffer = new byte[1024 * 32];
            logger.info("wating data from " + srcSocket.getRemoteSocketAddress());
            InputStream input = srcSocket.getInputStream();
            if (srcSocket.isClosed()) {
                logger.info(srcSocket.getRemoteSocketAddress() + " is closed");
                return;
            }
            int n = 0;
            while (-1 != (n = input.read(buffer))) {
                logger.info("received data from " + srcSocket.getRemoteSocketAddress() + " size : " + n);
                if (n > 0) {
                    ByteArrayOutputStream bo = new ByteArrayOutputStream();
                    bo.write(buffer, 0, n);
                    logger.info("receive data to string :\n" + new String(bo.toByteArray(), "utf-8"));
                }
                // 复制到另外一个端口
                for (Socket socket2 : toSockets) {
                    if (!socket2.isClosed() && n > 0) {
                        try {
                            if (discardData) {
                                logger.info("discard data for " + socket2.getRemoteSocketAddress());
                            } else {
                                logger.info("sending data to " + socket2.getRemoteSocketAddress());
                                socket2.getOutputStream().write(buffer, 0, n);
                                socket2.getOutputStream().flush();
                            }
                        } catch (IOException e) {
                            logger.info(socket2.getRemoteSocketAddress() + " error :" + e.getMessage(), e);
                            IOUtils.closeQuietly(socket2);
                        }
                    }
                }
                logger.info("wating data from " + srcSocket.getRemoteSocketAddress());
            }
            logger.info("close socket , received -1 from " + srcSocket.getRemoteSocketAddress());
            IOUtils.closeQuietly(srcSocket);
        } catch (SocketTimeoutException e) {
            logger.info("close socket , time out from " + srcSocket.getRemoteSocketAddress());
            IOUtils.closeQuietly(srcSocket);
        } catch (IOException e) {
            logger.error("transfer data from " + srcSocket.getRemoteSocketAddress() + " failed", e);
            IOUtils.closeQuietly(srcSocket);
        }
    }

    public boolean isDiscardData() {
        return discardData;
    }

    public void setDiscardData(boolean discardToSocketData) {
        this.discardData = discardToSocketData;
    }
}
