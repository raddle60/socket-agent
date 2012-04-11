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
 *
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
                            logger.debug("accepted socket :" + socket.getRemoteSocketAddress());
                            accepted = socket.getRemoteSocketAddress() + "";
                        } catch (IOException e) {
                            logger.error("accept socket failed", e);
                            continue;
                        }
                        int count = 0;
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        logger.debug(accepted + ", starting received data ");
                        try {
                            socket.setSoTimeout(200);
                            InputStream input = socket.getInputStream();
                            byte[] buffer = new byte[1024 * 32];
                            int n = 0;
                            // wating data received
                            Thread.sleep(50);
                            while (-1 != (n = input.read(buffer))) {
                                output.write(buffer, 0, n);
                                count += n;
                                // wating data received
                                Thread.sleep(20);
                            }
                        } catch (SocketTimeoutException e) {
                            // ignore time out
                        } catch (IOException e) {
                            logger.error(accepted + ", receive failed", e);
                            continue;
                        } catch (InterruptedException e) {
                            logger.error(accepted + ", receive interrupted", e);
                            continue;
                        }
                        logger.debug(accepted + ", received data size : " + count);
                        logger.debug(accepted + ", received data base64 : \n"
                                + Base64.encodeBase64URLSafeString(output.toByteArray()));
                        logger.debug(accepted + ", received data : \n" + new String(output.toByteArray()));
                        if (count != output.size()) {
                            logger.error(accepted + ", receive excepted " + count + " but " + output.size());
                            continue;
                        }
                        int removtePort = Integer.parseInt(properties.getProperty("remote.port"));
                        int removteSoTimeout = Integer.parseInt(properties.getProperty("remote.so.timeout", "10000"));
                        String removteIp = properties.getProperty("remote.ip");
                        logger.debug(accepted + ", starting connect to " + removteIp + ":" + removtePort);
                        try {
                            forwardSocket = new Socket(removteIp, removtePort);
                            forwardSocket.setSoTimeout(removteSoTimeout);
                        } catch (Exception e) {
                            logger.error(accepted + ", connect to " + removteIp + ":" + removtePort + " failed", e);
                            continue;
                        }
                        logger.debug(accepted + ", send data to " + removteIp + ":" + removtePort);
                        try {
                            forwardSocket.getOutputStream().write(output.toByteArray());
                            forwardSocket.getOutputStream().flush();
                        } catch (IOException e) {
                            logger.error(accepted + ", send to " + removteIp + ":" + removtePort + " failed", e);
                            continue;
                        }
                        int remoteCount = 0;
                        ByteArrayOutputStream remoteOutput = new ByteArrayOutputStream();
                        logger.debug(accepted + ", starting received data from remote");
                        try {
                            InputStream input = forwardSocket.getInputStream();
                            byte[] buffer = new byte[1024 * 32];
                            int n = 0;
                            // wating data received
                            Thread.sleep(200);
                            while (-1 != (n = input.read(buffer))) {
                                remoteOutput.write(buffer, 0, n);
                                remoteCount += n;
                                // wating data received
                                Thread.sleep(20);
                                // wating short time
                                forwardSocket.setSoTimeout(100);
                            }
                        } catch (SocketTimeoutException e) {
                            // ignore time out
                        } catch (IOException e) {
                            logger.error(accepted + ", receive from remote failed", e);
                            continue;
                        } catch (InterruptedException e) {
                            logger.error(accepted + ", receive from remote interrupted", e);
                            continue;
                        }
                        logger.debug(accepted + ", received from remote data size : " + remoteCount);
                        logger.debug(accepted + ", received from remote data base64 : \n"
                                + Base64.encodeBase64URLSafeString(output.toByteArray()));
                        logger.debug(accepted + ", received from remote data : \n"
                                + new String(remoteOutput.toByteArray()));
                        if (remoteCount != remoteOutput.size()) {
                            logger.error(accepted + ", receive from remote excepted " + remoteCount + " but "
                                    + remoteOutput.size());
                            continue;
                        }
                        logger.debug(accepted + ", send data back");
                        try {
                            socket.getOutputStream().write(remoteOutput.toByteArray());
                            socket.getOutputStream().flush();
                        } catch (IOException e) {
                            logger.error(accepted + ", send data back failed", e);
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
