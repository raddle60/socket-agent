package com.socket.agent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 */
public class SocketAgentServer {

    private final static Logger logger = LoggerFactory.getLogger(SocketAgentServer.class);
    private boolean started = false;
    private Properties properties;
    private Thread thread;

    public SocketAgentServer(Properties properties){
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
                        logger.info("socket agent listening on " + port);
                    } catch (IOException e) {
                        logger.error("starting socket agent failed", e);
                    }
                    Socket forwardSocket = null;
                    Socket socket = null;
                    while (true) {
                        if (socket != null) {
                            closeQuietly(socket);
                        }
                        if (forwardSocket != null) {
                            closeQuietly(forwardSocket);
                        }
                        String accepted = "";
                        try {
                            socket = server.accept();
                            logger.debug("accepted src socket :" + socket.getRemoteSocketAddress());
                            accepted = socket.getRemoteSocketAddress() + "";
                        } catch (IOException e) {
                            logger.error("accept src socket failed", e);
                            continue;
                        }
                        int count = 0;
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        logger.debug(accepted + ", starting received data from src ");
                        int srcSoTimeout = Integer.parseInt(properties.getProperty("src.so.timeout", "1000"));
                        int srcEndSoTimeout = Integer.parseInt(properties.getProperty("src.end.so.timeout", "10"));
                        try {
                            socket.setSoTimeout(srcSoTimeout);
                            InputStream input = socket.getInputStream();
                            byte[] buffer = new byte[1024 * 32];
                            int n = 0;
                            while (-1 != (n = input.read(buffer))) {
                                output.write(buffer, 0, n);
                                count += n;
                                socket.setSoTimeout(srcEndSoTimeout);
                            }
                        } catch (SocketTimeoutException e) {
                            // ignore time out
                        } catch (IOException e) {
                            logger.error(accepted + ", receive src data failed", e);
                            continue;
                        }
                        logger.debug(accepted + ", received src data size : " + count);
                        logger.debug(accepted + ", received src data base64 : \n" + Base64.encodeBase64URLSafeString(output.toByteArray()));
                        logger.debug(accepted + ", received src data : \n" + new String(output.toByteArray()));
                        if (count != output.size()) {
                            logger.error(accepted + ", receive src data failed , excepted " + count + " but " + output.size());
                            continue;
                        }
                        int destPort = Integer.parseInt(properties.getProperty("dest.port"));
                        String removteIp = properties.getProperty("remote.ip");
                        logger.debug(accepted + ", starting connect to dest " + removteIp + ":" + destPort);
                        try {
                            forwardSocket = new Socket(removteIp, destPort);
                        } catch (Exception e) {
                            logger.error(accepted + ", connect to dest " + removteIp + ":" + destPort + " failed", e);
                            continue;
                        }
                        logger.debug(accepted + ", send data to dest " + removteIp + ":" + destPort);
                        try {
                            forwardSocket.getOutputStream().write(output.toByteArray());
                            forwardSocket.getOutputStream().flush();
                        } catch (IOException e) {
                            logger.error(accepted + ", send data to dest " + removteIp + ":" + destPort + " failed", e);
                            continue;
                        }
                        int remoteCount = 0;
                        ByteArrayOutputStream remoteOutput = new ByteArrayOutputStream();
                        logger.debug(accepted + ", starting received data from dest");
                        int destSoTimeout = Integer.parseInt(properties.getProperty("dest.so.timeout", "15000"));
                        int destEndSoTimeout = Integer.parseInt(properties.getProperty("dest.end.so.timeout", "10"));
                        try {
                            forwardSocket.setSoTimeout(destSoTimeout);
                            InputStream input = forwardSocket.getInputStream();
                            byte[] buffer = new byte[1024 * 32];
                            int n = 0;
                            while (-1 != (n = input.read(buffer))) {
                                remoteOutput.write(buffer, 0, n);
                                remoteCount += n;
                                forwardSocket.setSoTimeout(destEndSoTimeout);
                            }
                        } catch (SocketTimeoutException e) {
                            // ignore time out
                        } catch (IOException e) {
                            logger.error(accepted + ", receive data from dest failed", e);
                            continue;
                        }
                        logger.debug(accepted + ", received data from dest size : " + remoteCount);
                        logger.debug(accepted + ", received data from dest  base64 : \n" + Base64.encodeBase64URLSafeString(output.toByteArray()));
                        logger.debug(accepted + ", received data from dest  : \n" + new String(remoteOutput.toByteArray()));
                        if (remoteCount != remoteOutput.size()) {
                            logger.error(accepted + ", receive data from dest failed, excepted " + remoteCount + " but " + remoteOutput.size());
                            continue;
                        }
                        logger.debug(accepted + ", send data back to src");
                        try {
                            socket.getOutputStream().write(remoteOutput.toByteArray());
                            socket.getOutputStream().flush();
                        } catch (IOException e) {
                            logger.error(accepted + ", send data back to src failed", e);
                        }
                        closeQuietly(socket);
                        closeQuietly(forwardSocket);
                    }
                }
            });
            thread.start();
        }
    }

    private void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException e1) {
        }
    }
}
