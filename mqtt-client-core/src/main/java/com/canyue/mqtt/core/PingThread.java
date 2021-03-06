package com.canyue.mqtt.core;

import com.canyue.mqtt.core.exception.MqttPersistenceException;
import com.canyue.mqtt.core.packet.PingReqPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author canyue
 */
public class PingThread implements Runnable{
    private Object pingLock;
    private MessageQueue messageQueue;
    private final static Logger logger = LoggerFactory.getLogger(PingThread.class);

    public void setMessageQueue(MessageQueue messageQueue) {
        this.messageQueue = messageQueue;
    }

    private int keepAlive;

    public void setKeepAlive(int keepAlive) {
        this.keepAlive = keepAlive;
    }
    public void setPingLock(Object pingLock) {
        this.pingLock = pingLock;
    }

    @Override
    public void run() {
        long start,end;
        if(keepAlive!=0){
            try {
                while (true){
                    synchronized (pingLock){
                        start=System.currentTimeMillis();
                        pingLock.wait(keepAlive*1000);
                        end=System.currentTimeMillis();
                        if(end-start>=keepAlive*1000){
                            messageQueue.handleSendMsg(new PingReqPacket());
                            logger.debug("ping报文已加入队列");
                        }
                    }
                }
            } catch (InterruptedException e) {
                logger.error("PingThread被中断了");
            }  catch (MqttPersistenceException e) {
                logger.error("持久化消息时发生异常",e);
            }finally {
                this.messageQueue.getClientCallback().shutdown();
            }
            logger.debug("PingThread已停止运行！");
        }
    }
}
