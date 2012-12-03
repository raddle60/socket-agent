package com.socket.agent;

/**
 * description:
 * @author xurong.raddle
 * time : 2012-12-3 上午10:10:05
 */
public class HeartbeatRecord {
    private long lastHeartbeatTime = -1;

    public long getLastHeartbeatTime() {
        return lastHeartbeatTime;
    }

    public void heartBeat() {
        lastHeartbeatTime = System.currentTimeMillis();
    }
}
