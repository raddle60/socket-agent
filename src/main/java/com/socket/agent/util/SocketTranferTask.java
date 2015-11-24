package com.socket.agent.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.socket.agent.model.ToScoket;

public class SocketTranferTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(SocketTranferTask.class);
    private Socket srcSocket;
    private Set<ToScoket> toSockets;
    private boolean discardData;

    public SocketTranferTask(Socket srcSocket, Set<ToScoket> toSockets) {
        this.srcSocket = srcSocket;
        this.toSockets = toSockets;
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
                if (srcSocket.isClosed()) {
                    logger.info(srcSocket.getRemoteSocketAddress() + " is closed");
                    return;
                }
                logger.info("received data from " + srcSocket.getRemoteSocketAddress() + " size : " + n);
                if (n > 0) {
                    ByteArrayOutputStream bo = new ByteArrayOutputStream();
                    bo.write(buffer, 0, n);
                    logger.info("receive data to string :\n" + new String(bo.toByteArray(), "utf-8"));
                }
                // 复制到另外一个端口
                ToScoket primarySocket = getPrimarySocket();
                for (ToScoket socket2 : toSockets) {
                    if (!socket2.getSocket().isClosed()) {
                        if (n > 0) {
                            try {
                                if (primarySocket.equals(socket2)) {
                                    logger.info("sending data to " + socket2.getSocket().getRemoteSocketAddress());
                                    socket2.getSocket().getOutputStream().write(buffer, 0, n);
                                    socket2.getSocket().getOutputStream().flush();
                                } else {
                                    logger.info("discard data for " + socket2.getSocket().getRemoteSocketAddress());
                                }
                            } catch (IOException e) {
                                logger.info(socket2.getSocket().getRemoteSocketAddress() + " error : " + e.getMessage());
                                IOUtils.closeQuietly(socket2.getSocket());
                            }
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
            logger.error("transfer data from " + srcSocket.getRemoteSocketAddress() + " failed , " + e.getMessage());
            IOUtils.closeQuietly(srcSocket);
        }
    }

    private ToScoket getPrimarySocket() {
        // 去除已关闭的
        for (Iterator<ToScoket> iterator = toSockets.iterator(); iterator.hasNext();) {
            ToScoket toScoket = iterator.next();
            if (toScoket.getSocket().isClosed()) {
                iterator.remove();
            }
        }
        boolean hasPrimary = false;
        for (ToScoket socketCopySocket : toSockets) {
            if (socketCopySocket.isPrimary()) {
                hasPrimary = true;
            }
        }
        for (ToScoket socketCopySocket2 : toSockets) {
            if (hasPrimary) {
                if (socketCopySocket2.isPrimary()) {
                    return socketCopySocket2;
                }
            } else {
                if (!socketCopySocket2.getSocket().isClosed()) {
                    return socketCopySocket2;
                }
            }
        }
        return null;
    }

    public boolean isDiscardData() {
        return discardData;
    }

    public void setDiscardData(boolean discardToSocketData) {
        this.discardData = discardToSocketData;
    }
}
