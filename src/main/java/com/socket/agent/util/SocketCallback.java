package com.socket.agent.util;

import java.net.Socket;

/**
 * description: 
 * @author Administrator
 * time : 2015年12月3日 上午11:29:51
 */
public abstract class SocketCallback {
	public void dataReceived(Socket socket, byte[] data) {
	};

	public boolean isDiscardData(Socket fromSocket, Socket toSocket, byte[] data) {
		return TransferUtils.isDiscardData(fromSocket, toSocket);
	}

	public void dataSent(Socket srcSocket, Socket toSocket, byte[] data) {
	};

	public void socketClosed(Socket socket) {
	};
}
