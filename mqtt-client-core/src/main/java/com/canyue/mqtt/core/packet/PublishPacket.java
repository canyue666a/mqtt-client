package com.canyue.mqtt.core.packet;


import com.canyue.mqtt.core.Message;
import com.canyue.mqtt.core.utils.PacketUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * @author canyue
 *发布消息：
 *      固定报头：
 *             byte1：   0 0 1 1 DUP Qos-H Qos-L RETAIN
 *                    DUP: 重发标志，0表示这是第一次请求发送的，1表示这是早前报文请求的重发
 *                    Qos:不能等于3
 *                          Qos 0 必须 DUP = 0;
 *                          Qos 1
 *                          Qos 2,
 *                    RETAIN:
 *                          若设为1，服务端必须存储这个消息和他的Qos以便可以发给未来主题名匹配的订阅者
 *                          如果保留标志为1且有效载荷为0字节，意思为现存的客户端收到的消息中保留标志未被设置，服务端会删除
 *                      该主题的所有保留消息。
 *             剩余长度;
 *      可变报头：
 *              主题名    mqtt-utf8 不能包含通配符
 *              报文标识符(当Qos>0时)
 *      有效载荷
 *          要发布的消息，可以为0长度，长度=剩余长度-可变报头的长度
 */

public class PublishPacket extends BasePacket{
    public Message getMessage() {
        return message;
    }
    private final  PacketType type = PacketType.PUBLISH_TYPE;
    private Message message;
    public int getMsgId() {
        return msgId;
    }

    public void setMsgId(int msgId) {
        this.msgId = msgId;
    }

    private int msgId =0;
    private static Logger logger = LoggerFactory.getLogger(PublishPacket.class);
    public PublishPacket(byte flag, byte[] data) {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);
        try {
            String topic = PacketUtils.decodeMQTTUTF8(dis);
            this.message=new Message(topic);
            this.message.setQos((flag&0x06)>>1);
            this.message.setRetain((flag&0x01)==1?true:false);
            this.message.setDup((flag&0x08)==1?true:false);

            if(this.message.getQos()>0){
                this.msgId = dis.readUnsignedShort();
                this.message.setMsgId(this.msgId);
            }
            //utf-8字符串长度+2字节长度
            int encodedTopicLength = topic.getBytes("UTF-8").length+2;
            //剩余长度-（主题长度+报文标识符长度）=有效载荷长度
            int payloadLength = data.length-(encodedTopicLength+(this.message.getQos()==0?0:2));
            byte[] payload = new byte[payloadLength];
            dis.readFully(payload,0,payloadLength);
            this.message.setPayload(payload);

        } catch (IOException e) {
           logger.error("生成publish报文出现异常",e);
        }
        logger.debug("publish 报文解析完毕:" +
                "\tmsgId:{}" +
                "\tmessage:{};",msgId,message);
    }
    public PublishPacket(Message message){
        this.message=message;
        if(message.getQos()>0){
            msgId = message.getMsgId();
        }
        logger.debug("publish 报文生成完毕:" +
                "\tmsgId:{}," +
                "\tmessage:{};",msgId,message);
    }

    @Override
    public byte[] getVariableHeader() throws IOException {
        ByteArrayOutputStream baos=new ByteArrayOutputStream();
        DataOutputStream dos=new DataOutputStream(baos);
        PacketUtils.encodeMqttUtf8(dos,message.getTopic());
        if(message.getQos()>0){
            dos.writeShort(msgId);
        }
        return baos.toByteArray();
    }

    @Override
    public byte[] getPayload() throws IOException {
        return message.getPayload();
    }

    @Override
    public byte getFixHeaderFlag() {
        byte flag = 0;
        if(message.isDup()){
            flag|=0x08;
        }
        flag|=message.getQos()<<1;
        if(message.isRetain()){
            flag|=0x01;
        }
        return flag;
    }
    @Override
    public PacketType getType() {
        return type;
    }
}
