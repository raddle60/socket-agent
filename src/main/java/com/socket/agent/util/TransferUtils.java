package com.socket.agent.util;

import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.socket.agent.model.SocketCopySocket;
import com.socket.agent.model.ToScoket;

/**
 * description: 
 * @author Administrator
 * time : 2015年11月23日 下午9:18:30
 */
public class TransferUtils {
    private static Map<Socket, Set<ToScoket>> socketMap = new HashMap<Socket, Set<ToScoket>>();
    private static Map<Socket, SocketTranferTask> transferMap = new HashMap<Socket, SocketTranferTask>();

    public static void addSocket(Socket fromSocket, SocketCopySocket toSocket) {
        List<SocketCopySocket> ss = new ArrayList<SocketCopySocket>();
        ss.add(toSocket);
        addSocket(fromSocket, ss);
    }

    public static void addSocket(Socket fromSocket, List<SocketCopySocket> toSockets) {
        Set<ToScoket> srcSet = socketMap.get(fromSocket);
        if (srcSet == null) {
            srcSet = new LinkedHashSet<ToScoket>();
            socketMap.put(fromSocket, srcSet);
        }
        for (SocketCopySocket toScoket : toSockets) {
            srcSet.add(new ToScoket(toScoket.getToSocket(), toScoket.isPrimary()));
        }
        startTask();
    }

    private static void startTask() {
        for (Socket srcSocket : socketMap.keySet()) {
            if (!transferMap.containsKey(srcSocket)) {
                // 源socket任务
                SocketTranferTask srcTask = new SocketTranferTask(srcSocket, socketMap.get(srcSocket));
                new Thread(srcTask).start();
                Set<ToScoket> toSet = socketMap.get(srcSocket);
                for (ToScoket toScoket : toSet) {
                    // 目标socket任务
                    if (!transferMap.containsKey(toScoket.getSocket())) {
                        Set<ToScoket> toSrcSet = socketMap.get(toScoket.getSocket());
                        if (toSrcSet == null) {
                            toSrcSet = new LinkedHashSet<ToScoket>();
                            socketMap.put(toScoket.getSocket(), toSrcSet);
                        }
                        ToScoket o = new ToScoket(srcSocket, false);
                        if (!toSrcSet.contains(o)) {
                            toSrcSet.add(o);
                        }
                        SocketTranferTask task2 = new SocketTranferTask(toScoket.getSocket(), toSrcSet);
                        new Thread(task2).start();
                    }
                }
            }
        }
    }
}
