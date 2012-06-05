package com.socket.agent;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Properties;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(0, 50, 10L, TimeUnit.SECONDS,
            new SynchronousQueue<Runnable>());

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
                    }
                    Socket socket = null;
                    while (true) {
                        try {
                            socket = server.accept();
                            logger.info("accepted socket :" + socket.getRemoteSocketAddress());
                        } catch (IOException e) {
                            logger.error("accept socket failed", e);
                            continue;
                        }
                        try {
                            threadPoolExecutor.execute(new AgentTask(socket));
                        } catch (Exception e) {
                            logger.error(e.getMessage());
                        }
                    }
                }
            });
            thread.start();
        }
    }

    private class AgentTask implements Runnable {

        private Socket socket;

        public AgentTask(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            int destPort = Integer.parseInt(properties.getProperty("dest.port"));
            String destIp = properties.getProperty("dest.ip");
            Socket forwardSocket = null;
            try {
                forwardSocket = new Socket();
                forwardSocket.connect(new InetSocketAddress(destIp, destPort),
                        Integer.parseInt(properties.getProperty("dest.conn.timeout", "5000")));
                logger.info("connected to dest " + destIp + ":" + destPort);
            } catch (IOException e) {
                logger.error(socket.getRemoteSocketAddress() + ", connect to dest " + destIp + ":" + destPort
                        + " failed", e);
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
            logger.debug("transfer data from " + socket.getRemoteSocketAddress() + " to "
                    + forwardSocket.getRemoteSocketAddress() + " complete");
        }

    }

    private class TransferData extends Thread {

        private Socket sourceSocket;
        private Socket targetSocket;
        private boolean toClose = false;
        private TransferData related;
        private boolean srcToDest;

        public TransferData(Socket sourceSocket, Socket targetSocket, boolean srcToDest) {
            this.sourceSocket = sourceSocket;
            this.targetSocket = targetSocket;
            this.srcToDest = srcToDest;
        }

        @Override
        public void run() {
            String accepted = sourceSocket.getRemoteSocketAddress() + "";
            int soTimeout = Integer.parseInt(properties.getProperty("so.timeout", "10000"));
            try {
                InputStream input = sourceSocket.getInputStream();
                logger.debug("wating data from " + accepted);
                while (!toClose) {
                    int n = 0;
                    byte[] buffer = new byte[1024 * 32];
                    try {
                        // 读取源socket
                        if (soTimeout != -1) {
                            sourceSocket.setSoTimeout(soTimeout);
                        }
                        while (!sourceSocket.isClosed() && -1 != (n = input.read(buffer))) {
                            logger.info("received data from " + accepted + " size : " + n);
                            // 发送给目标socket
                            targetSocket.getOutputStream().write(buffer, 0, n);
                            targetSocket.getOutputStream().flush();
                        }
                        if (!srcToDest) {
                            // 从目标接收结束，需要关闭socket
                            toClose = true;
                            closeQuietly(sourceSocket);
                        }
                    } catch (SocketTimeoutException e) {
                        logger.trace(e.getMessage());
                    } catch (SocketException e) {
                        // 并发关闭问题，远程已关闭，这里还阻塞在读取，忽略这个错误
                        logger.trace(e.getMessage());
                        closeQuietly(sourceSocket);
                    }
                }
            } catch (IOException e) {
                logger.error("transfer data from " + sourceSocket.getRemoteSocketAddress() + " to "
                        + targetSocket.getRemoteSocketAddress() + " failed , " + e.getMessage());
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
                socket.close();
                logger.info("closed socket " + socket.getRemoteSocketAddress());
            }
        } catch (IOException e1) {
            logger.error(e1.getMessage(), e1);
        }
    }
}