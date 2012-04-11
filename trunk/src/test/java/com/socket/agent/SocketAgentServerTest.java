package com.socket.agent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketAgentServerTest {

    private final static Logger logger = LoggerFactory.getLogger(SocketAgentServerTest.class);

    @Test
    public void testStart() throws InterruptedException, UnknownHostException, IOException {
        Properties properties = new Properties();
        properties.setProperty("local.port", "9000");
        properties.setProperty("remote.ip", "127.0.0.1");
        properties.setProperty("remote.port", "9001");
        SocketAgentServer server = new SocketAgentServer(properties);
        server.start();
        new Thread() {

            @Override
            public void run() {
                try {
                    ServerSocket server = new ServerSocket(9001);
                    while (true) {
                        Socket socket = server.accept();
                        logger.debug("accept " + socket.getRemoteSocketAddress() + "");
                        ByteArrayOutputStream output = new ByteArrayOutputStream();
                        logger.debug("receiving " + socket.getRemoteSocketAddress() + "");
                        socket.setSoTimeout(10);
                        try {
                            IOUtils.copy(socket.getInputStream(), output);
                        } catch (Exception e) {
                        }
                        logger.debug("received " + output.size() + "");
                        socket.getOutputStream().write(DigestUtils.md5(output.toByteArray()));
                        socket.getOutputStream().flush();
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }.start();
        for (int i = 0; i < 100; i++) {
            Socket socket = new Socket("127.0.0.1", 9000);
            byte[] bytes = RandomStringUtils.random(RandomUtils.nextInt(10000)).getBytes();
            socket.getOutputStream().write(bytes);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            IOUtils.copy(socket.getInputStream(), output);
            socket.close();
            boolean ret = Arrays.equals(output.toByteArray(), DigestUtils.md5(bytes));
            Assert.assertTrue(ret);
            logger.info(i + "");
        }
    }
}
