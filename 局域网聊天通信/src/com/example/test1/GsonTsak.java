package com.example.test1;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GsonTsak {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		JsonObject json = new JsonObject();
		json.addProperty("msgtype", 0);
		json.addProperty("name", "your name");
		json.addProperty("pwd", "3333333");
		
		String buffer = json.toString();
		System.out.println(buffer);
		
		JsonParser parser = new JsonParser();
		JsonObject recvJson = (JsonObject) parser.parse(buffer);
		
		int msgType = recvJson.get("msgtype").getAsInt();
		switch(msgType){
			case 0:
				System.out.println(recvJson.get("name").getAsString());
				System.out.println(recvJson.get("pwd").getAsString());
				break;
			case 1:
				break;
			case 2:
			
		}

	}

}
