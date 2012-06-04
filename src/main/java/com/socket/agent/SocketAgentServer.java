package com.socket.agent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Properties;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(0, 50, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());

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
                        int destPort = Integer.parseInt(properties.getProperty("dest.port"));
                        String destIp = properties.getProperty("dest.ip");
                        logger.info("socket agent listening on " + port + " agent for " + destIp + ":" + destPort);
                    } catch (IOException e) {
                        logger.error("starting socket agent failed", e);
                    }
                    Socket socket = null;
                    while (true) {
                        try {
                            socket = server.accept();
                            logger.debug("accepted socket :" + socket.getRemoteSocketAddress());
                        } catch (IOException e) {
                            logger.error("accept socket failed", e);
                            continue;
                        }
                        threadPoolExecutor.execute(new AgentTask(socket));
                    }
                }
            });
            thread.start();
        }
    }

    private class AgentTask implements Runnable {

        private Socket socket;

        public AgentTask(Socket socket){
            this.socket = socket;
        }

        public void run() {
            int destPort = Integer.parseInt(properties.getProperty("dest.port"));
            String destIp = properties.getProperty("dest.ip");
            Socket forwardSocket = null;
            try {
                forwardSocket = new Socket();
                forwardSocket.connect(new InetSocketAddress(destIp, destPort), Integer.parseInt(properties.getProperty("dest.conn.timeout", "5000")));
            } catch (IOException e) {
                logger.error(socket.getRemoteSocketAddress() + ", connect to dest " + destIp + ":" + destPort + " failed", e);
                if (socket != null) {
                    closeQuietly(socket);
                }
                return;
            }
            TransferData sourceTransferData = new TransferData(socket, forwardSocket, true);
            TransferData destTransferData = new TransferData(forwardSocket, socket, false);
            sourceTransferData.related = destTransferData;
            destTransferData.related = sourceTransferData;
            sourceTransferData.start();
            destTransferData.start();
            try {
                sourceTransferData.join();
                destTransferData.join();
            } catch (InterruptedException e) {

            }
            logger.debug("transfer data from " + socket.getRemoteSocketAddress() + " to " + forwardSocket.getRemoteSocketAddress() + " complete");
        }

    }

    private class TransferData extends Thread {

        private Socket sourceSocket;
        private Socket targetSocket;
        private boolean toClose = false;
        private TransferData related;
        private boolean srcToDest;

        public TransferData(Socket sourceSocket, Socket targetSocket, boolean srcToDest){
            this.sourceSocket = sourceSocket;
            this.targetSocket = targetSocket;
            this.srcToDest = srcToDest;
        }

        @Override
        public void run() {
            long start = System.currentTimeMillis();
            int srcTotalTimeout = Integer.parseInt(properties.getProperty("total.timeout", "-1"));
            String accepted = sourceSocket.getRemoteSocketAddress() + "";
            int srcSoTimeout = Integer.parseInt(properties.getProperty("so.timeout", "1000"));
            int srcCheckSoTimeout = Integer.parseInt(properties.getProperty("check.so.timeout", "10"));
            try {
                sourceSocket.setSoTimeout(srcSoTimeout);
                InputStream input = sourceSocket.getInputStream();
                logger.debug("wating data from " + accepted);
                while (!toClose) {
                    int n = 0;
                    int count = 0;
                    byte[] buffer = new byte[1024 * 32];
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    try {
                        // 读取配置源socket
                        while (!sourceSocket.isClosed() && -1 != (n = input.read(buffer))) {
                            output.write(buffer, 0, n);
                            count += n;
                            sourceSocket.setSoTimeout(srcCheckSoTimeout);
                        }
                        if (!srcToDest) {
                            // 从目标接受到结束，说明接收结束，需要关闭socket
                            toClose = true;
                            closeQuietly(sourceSocket);
                        }
                    } catch (SocketTimeoutException e) {
                    }
                    // 发送给目标socket
                    if (count > 0) {
                        logger.debug("received data from " + accepted + " size : " + count);
                        logger.debug("received data from " + accepted + " base64 : \n" + Base64.encodeBase64URLSafeString(output.toByteArray()));
                        logger.debug("received data from " + accepted + " : \n" + new String(output.toByteArray()));
                        if (count != output.size()) {
                            logger.error("receive data from " + accepted + " failed , excepted " + count + " but " + output.size());
                            break;
                        }
                        logger.debug("sending data to " + targetSocket.getRemoteSocketAddress());
                        try {
                            if (!targetSocket.isClosed()) {
                                targetSocket.getOutputStream().write(output.toByteArray());
                                targetSocket.getOutputStream().flush();
                                logger.debug("sent data to " + targetSocket.getRemoteSocketAddress() + " completed");
                                if (toClose) {
                                    // 从目标接受到结束，发送完毕，需要关闭socket
                                    closeSocket();
                                }
                            }
                        } catch (IOException e) {
                            logger.error("sending data to " + targetSocket.getRemoteSocketAddress() + " failed", e);
                            break;
                        }
                    }
                    if (srcTotalTimeout != -1 && System.currentTimeMillis() - start > srcTotalTimeout) {
                        logger.warn("waiting timeout " + (System.currentTimeMillis() - start));
                        break;
                    }
                }
            } catch (IOException e) {
                logger.error("receive data from " + accepted + " failed", e);
                return;
            } finally {
                closeSocket();
            }
        }

        private void closeSocket() {
            toClose = true;
            related.toClose = true;
            if (sourceSocket != null) {
                closeQuietly(sourceSocket);
            }
            if (targetSocket != null) {
                closeQuietly(targetSocket);
            }
        }

    }

    private void closeQuietly(Socket socket) {
        try {
            if (socket != null && !socket.isClosed()) {
                logger.debug("close socket " + socket.getRemoteSocketAddress());
                socket.close();
            }
        } catch (IOException e1) {
            logger.error(e1.getMessage(), e1);
        }
    }
}
