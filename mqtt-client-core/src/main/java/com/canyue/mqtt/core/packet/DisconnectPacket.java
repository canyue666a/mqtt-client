package com.canyue.mqtt.core.packet;

/**
 * 断开连接报文
 *      固定报头：       byte1: 0xe0
 *              剩余长度 byte2  0x00
 */

import java.io.IOException;

public class DisconnectPacket extends BasePacket{
	
	private final static PacketType type = PacketType.DISCONNECT_TYPE;
	
	public DisconnectPacket(byte[] data) {
		super();
	}
	
	public DisconnectPacket() {
	
	}
	
	public byte[] getVariableHeader() throws IOException {
        return new byte[0];
    }

    public byte[] getPayload() throws IOException {
        return new byte[0];
    }

    public byte getFixHeaderFlag() {
        return 0;
    }
	
	public PacketType getType() {
		return type;
	}
    @Override
    public String toString() {
        return "DisconnectPacket{" +
                "type=" + type +
                '}';
    }
}