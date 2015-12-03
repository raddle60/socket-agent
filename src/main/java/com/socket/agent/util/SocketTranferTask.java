package com.socket.agent.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.socket.agent.model.ToScoket;

public class SocketTranferTask implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(SocketTranferTask.class);
    private Socket srcSocket;
    private SocketCallback callback;
    private Set<ToScoket> toSockets;

    public SocketTranferTask(Socket srcSocket, SocketCallback callback, Set<ToScoket> toSockets) {
        this.srcSocket = srcSocket;
        this.callback = callback;
        this.toSockets = toSockets;
    }

    public void run() {
        try {
            logger.info("start SocketTranferTask +" + srcSocket.getRemoteSocketAddress() + ">" + srcSocket.getLocalPort());
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
                    if (callback != null) {
                        callback.dataReceived(srcSocket, bo.toByteArray());
                    }
                }
                // 复制到另外一个端口
                for (ToScoket socket2 : toSockets) {
                    if (!socket2.getSocket().isClosed()) {
                        if (n > 0) {
                            try {
                                if (!TransferUtils.isDiscardData(srcSocket, socket2.getSocket())) {
                                    logger.info("sending data to " + socket2.getSocket().getRemoteSocketAddress());
                                    socket2.getSocket().getOutputStream().write(buffer, 0, n);
                                    socket2.getSocket().getOutputStream().flush();
                                } else {
                                    logger.info("discard data from " + srcSocket.getRemoteSocketAddress() + " for " + socket2.getSocket().getRemoteSocketAddress());
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
            logger.info("end SocketTranferTask +" + srcSocket.getRemoteSocketAddress() + ">" + srcSocket.getLocalPort());
            IOUtils.closeQuietly(srcSocket);
        } catch (SocketTimeoutException e) {
            logger.info("close socket , time out from " + srcSocket.getRemoteSocketAddress());
            logger.info("end SocketTranferTask +" + srcSocket.getRemoteSocketAddress() + ">" + srcSocket.getLocalPort());
            IOUtils.closeQuietly(srcSocket);
        } catch (IOException e) {
            logger.error("transfer data from " + srcSocket.getRemoteSocketAddress() + " failed , " + e.getMessage());
            logger.info("end SocketTranferTask +" + srcSocket.getRemoteSocketAddress() + ">" + srcSocket.getLocalPort());
            IOUtils.closeQuietly(srcSocket);
        }
    }

    public SocketCallback getCallback() {
        return callback;
    }

    public void setCallback(SocketCallback callback) {
        this.callback = callback;
    }
}
