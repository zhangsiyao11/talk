package com.example.test1;

import java.io.Serializable;

enum MsgType{
	MSG_LOGIN,
};

class Message implements Serializable{
	private MsgType msgType;
	private String name;
	
	public Message(MsgType msgType, String name) {
		super();
		this.msgType = msgType;
		this.name = name;
	}

	@Override
	public String toString() {
		return "Message [msgType=" + msgType + ", name=" + name + "]";
	}
}
