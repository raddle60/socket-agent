package com.socket.agent.model;

import java.net.Socket;

public class ToScoket {
    private Socket socket;
    /**
     * 非主要socket的返回数据将丢弃
     */
    private boolean isPrimary;

    public ToScoket(Socket socket, boolean isPrimary) {
        this.socket = socket;
        this.isPrimary = isPrimary;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + socket.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ToScoket other = (ToScoket) obj;
        return socket.equals(other.socket);
    }

    @Override
    public String toString() {
        return "ToScoket [socket=" + socket + ", isPrimary=" + isPrimary + "]";
    }

}
