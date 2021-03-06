package com.socket.agent.util;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.socket.agent.model.SocketCopySocket;
import com.socket.agent.model.ToScoket;

/**
 * description: 
 * @author Administrator
 * time : 2015年11月23日 下午9:18:30
 */
public class TransferUtils {
    private static final Logger logger = LoggerFactory.getLogger(TransferUtils.class);
    private static Map<Socket, Set<ToScoket>> socketMap = new ConcurrentHashMap<Socket, Set<ToScoket>>();
    private static Map<Socket, SocketTranferTask> transferMap = new ConcurrentHashMap<Socket, SocketTranferTask>();

    static {
        new Thread(new Runnable() {

            public void run() {
                while (true) {
                    try {
                        Thread.sleep(60000);
                        cleanClosedSocket();
                        logger.info("current socket count :" + socketMap.size() + " ,transfer count :" + transferMap.size());
                    } catch (Exception e) {
                        logger.info("remove closed socket exception ," + e.getMessage(), e);
                    }
                }
            }
        }, "TransferUtils-clean").start();
    }

    public static void addSocket(Socket fromSocket, SocketCallback callback, SocketCopySocket toSocket) {
        List<SocketCopySocket> ss = new ArrayList<SocketCopySocket>();
        if (toSocket != null) {
            ss.add(toSocket);
        }
        addSocket(fromSocket, callback, ss);
    }

    public static synchronized void addSocket(Socket fromSocket, SocketCallback callback, List<SocketCopySocket> toSockets) {
        Set<ToScoket> srcSet = socketMap.get(fromSocket);
        if (srcSet == null) {
            srcSet = new LinkedHashSet<ToScoket>();
            socketMap.put(fromSocket, srcSet);
        }
        if (toSockets != null) {
            for (SocketCopySocket toScoket : toSockets) {
                // 将目标加到源中
                srcSet.add(new ToScoket(toScoket.getToSocket(), toScoket.isPrimary()));
                // 将源加到目标中
                Set<ToScoket> toSrcSet = socketMap.get(toScoket.getToSocket());
                if (toSrcSet == null) {
                    toSrcSet = new LinkedHashSet<ToScoket>();
                    socketMap.put(toScoket.getToSocket(), toSrcSet);
                }
                ToScoket o = new ToScoket(fromSocket, false);
                if (!toSrcSet.contains(o)) {
                    toSrcSet.add(o);
                }
            }
        }
        startTask(callback);
    }

    /**
     * 判断到toAddr的连接是否关闭
     * @param fromSocket
     * @param toAddr
     * @return
     */
    public static boolean isToAddrClosed(Socket fromSocket, String toAddr) {
        InetSocketAddress toSocketAddr = new InetSocketAddress(toAddr.split(":")[0], Integer.parseInt(toAddr.split(":")[1]));
        for (ToScoket toScoket : socketMap.get(fromSocket)) {
            if (!toScoket.getSocket().isClosed() && toScoket.getSocket().getRemoteSocketAddress().equals(toSocketAddr)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isAllToSocketClosed(Socket fromSocket) {
        boolean allClosed = true;
        for (ToScoket toScoket : socketMap.get(fromSocket)) {
            if (!toScoket.getSocket().isClosed()) {
                allClosed = false;
                break;
            }
        }
        return allClosed;
    }

    private static synchronized void startTask(SocketCallback callback) {
        for (Socket srcSocket : socketMap.keySet()) {
            if (!transferMap.containsKey(srcSocket) && !srcSocket.isClosed()) {
                // 源socket任务
                SocketTranferTask srcTask = new SocketTranferTask(srcSocket, callback, socketMap.get(srcSocket));
                new Thread(srcTask, "TransferUtils-" + srcSocket.getRemoteSocketAddress() + ">" + srcSocket.getLocalPort()).start();
                transferMap.put(srcSocket, srcTask);
            }
        }
        for (Socket srcSocket : socketMap.keySet()) {
            Set<ToScoket> toSet = socketMap.get(srcSocket);
            for (ToScoket toScoket : toSet) {
                // 目标socket任务
                if (!transferMap.containsKey(toScoket.getSocket()) && !toScoket.getSocket().isClosed()) {
                    SocketTranferTask task2 = new SocketTranferTask(toScoket.getSocket(), callback, socketMap.get(toScoket.getSocket()));
                    new Thread(task2, "TransferUtils-" + toScoket.getSocket().getRemoteSocketAddress() + ">" + toScoket.getSocket().getLocalPort()).start();
                    transferMap.put(toScoket.getSocket(), task2);
                }
            }
        }
    }

    /**
     * 有多个fromSocket发给toSocket，只接收其中一个fromSocket的数据
     * @param fromSocket
     * @param toSocket
     * @return
     */
    public static boolean isDiscardDataMultiFrom(Socket fromSocket, Socket toSocket) {
        Set<ToScoket> srcSet = socketMap.get(toSocket);
        // 只转发到一个socket，不需要丢弃
        if (srcSet.size() == 1) {
            return false;
        }
        ToScoket primarySocket = getPrimarySocket(srcSet);
        if (primarySocket == null) {
            return false;
        }
        return !fromSocket.equals(primarySocket.getSocket());
    }
    
    /**
     * 一个fromSocket发给多个toSocket，只发给其中一个toSocket
     * @param fromSocket
     * @param toSocket
     * @return
     */
    public static boolean isDiscardDataMultiTo(Socket fromSocket, Socket toSocket) {
        Set<ToScoket> srcSet = socketMap.get(fromSocket);
        // 只转发到一个socket，不需要丢弃
        if (srcSet.size() == 1) {
            return false;
        }
        ToScoket primarySocket = getPrimarySocket(srcSet);
        if (primarySocket == null) {
            return false;
        }
        return !toSocket.equals(primarySocket.getSocket());
    }

    private static ToScoket getPrimarySocket(Set<ToScoket> toSockets) {
        // 去除已关闭的
        for (Iterator<ToScoket> iterator = toSockets.iterator(); iterator.hasNext();) {
            ToScoket toScoket = iterator.next();
            if (toScoket.getSocket().isClosed()) {
                logger.info("remove closed socket " + toScoket.getSocket());
                iterator.remove();
            }
        }
        boolean hasPrimary = false;
        for (ToScoket socketCopySocket : toSockets) {
            if (socketCopySocket.isPrimary()) {
                hasPrimary = true;
            }
        }
        for (ToScoket socketCopySocket2 : toSockets) {
            if (hasPrimary) {
                if (socketCopySocket2.isPrimary()) {
                    return socketCopySocket2;
                }
            } else {
                if (!socketCopySocket2.getSocket().isClosed()) {
                    return socketCopySocket2;
                }
            }
        }
        return null;
    }

    private static void cleanClosedSocket() {
        for (Iterator<Socket> iterator = socketMap.keySet().iterator(); iterator.hasNext();) {
            Socket key = iterator.next();
            if (key.isClosed()) {
                logger.info("remove closed socket " + key);
                iterator.remove();
            }
        }
        for (Iterator<Socket> iterator = transferMap.keySet().iterator(); iterator.hasNext();) {
            Socket key = iterator.next();
            if (key.isClosed()) {
                logger.info("remove closed socket " + key);
                iterator.remove();
            }
        }
    }
}
