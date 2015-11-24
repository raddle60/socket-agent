package com.socket.agent;

import com.socket.agent.middle.SocketMiddleClient;
import com.socket.agent.model.SocketMiddleFoward;

/**
 * description: 
 * @author xurong
 * time : 2015年11月17日 下午2:59:58
 */
public class SocketMiddleTest {

    /**
     * @param args
     * @throws InterruptedException 
     */
    public static void main(String[] args) throws InterruptedException {
//        SocketMiddleServer middleServer = new SocketMiddleServer();
//        middleServer.getMiddles().add(new SocketMiddle(10004, 10002));
//        middleServer.start();
        SocketMiddleClient client = new SocketMiddleClient();
        client.getMiddles().add(new SocketMiddleFoward("127.0.0.1:8082", "127.0.0.1:8080"));
        client.start();
    }

}
