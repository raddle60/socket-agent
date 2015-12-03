package com.socket.agent.util;

import java.net.Socket;

/**
 * description: 
 * @author Administrator
 * time : 2015年12月3日 上午11:29:51
 */
public interface SocketCallback {
    public void dataReceived(Socket socket, byte[] data);
}
