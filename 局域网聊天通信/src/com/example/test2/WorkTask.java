package com.example.test2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import com.example.test3.Mysql;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class WorkTask implements Runnable{
	
	private ByteBuffer Buff;
	private Selector rwSelector;
	private Mysql mysql;
	
	public WorkTask(){
		Buff = ByteBuffer.allocate(1024);
		try {
			rwSelector = Selector.open();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		mysql = new Mysql();
	}
	
	public Selector getSelector(){
		return rwSelector;
	}
	
	public void write(SocketChannel client,JsonObject Json){
		
		try {
			client.write(ByteBuffer.wrap((Json.toString()+"\r\n").getBytes()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void exit(SocketChannel client,String exitName,SelectionKey key){
		SelectorServer.usermap.remove(exitName, client);
		
		Iterator<FileInfo> iterator = SelectorServer.userinfo.get(client).getFileList().iterator();
		while(iterator.hasNext()){
			FileInfo fileinfo = iterator.next();
			if(fileinfo.getFileSize() > fileinfo.getFinishSize()){
				mysql.insertFile(exitName, fileinfo.getFileName(),fileinfo.getFileSize(),fileinfo.getFinishSize());
			}
		}
		
		SelectorServer.userinfo.remove(client);
		broadCast(4,exitName,null);
		try {
			client.close();
			key.cancel();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public void loginMsg(SocketChannel client,String name){//login时发送离线消息
		try {
			ResultSet result = mysql.getMsg(name);
			
				while(result.next()){
					JsonObject Json = new JsonObject();
					Json.addProperty("msgtype", 12);
					Json.addProperty("from", result.getString(1));
					Json.addProperty("msg",result.getString(3));
					write(client,Json);
				}
				mysql.deleteMsg(name);
				
		} catch (SQLException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
	}
	
	public void loginFile(int num,SocketChannel client,String name){
		
		try {
			ResultSet result = mysql.getFile(name);
			switch(num){
			case 0: 
				int count = 0;
				while(result.next()){
					count++;
				}
				if(count != 0){
					JsonObject Json = new JsonObject();
					Json.addProperty("msgtype", 10);
					Json.addProperty("ack", "有"+ count + "个离线文件或上次未接收完成的文件，是否接收??");
					write(client,Json);
				}
				break ;
			case 4:
//				while(result.next()){
//					String pathName = result.getString(2);
//					long fileSize = result.getLong(3);
//					long finishSize = result.getLong(4);
//					
//				}
			}
				
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void broadCast(int num,String name,String news) {//发送广播消息
		Iterator<SocketChannel> iterator = SelectorServer.usermap.values().iterator();
		while(iterator.hasNext()){
			
			String respond = null;
			SocketChannel client = iterator.next();
			JsonObject sendJson = new JsonObject();
			sendJson.addProperty("msgtype", 11);
				
			switch(num){
				case 3:
					respond = name + "上线了！！！";
					break ;
				case 4:
					respond = name + "下线了！！！";
					break ;
				case 5:
					respond = news;
			}
			sendJson.addProperty("ack",respond);
			
			write(client,sendJson);	
		}
	}
	
	public int sendMsg(String fromName,String toName,String type,String msg)  {//发送聊天信息

		String sendMsg = null;
		
		if(type.equals("talk")){
			sendMsg = msg;
		}else if(type.equals("putfile")){
			sendMsg = "请求上传文件：" + msg + "是否接受 ？？";
		}else{
			sendMsg = "请求下载文件：" + msg + "是否同意？？";
		}
		
		SocketChannel client = null;
		if((client=SelectorServer.usermap.get(toName)) != null){
			JsonObject sendJson = new JsonObject();
			sendJson.addProperty("msgtype", 12);
			sendJson.addProperty("from", fromName);
			sendJson.addProperty("msg", sendMsg);
			write(client,sendJson);
		
			return 1;
		}
		
		try {
			if(mysql.Search(toName).next()){
				return 0;
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return -1;
	}
	
	public int sendFileInfo(String toName,String fileName,long filesize,int port){
		
		SocketChannel client = null;
		if((client=SelectorServer.usermap.get(toName)) != null){
			JsonObject sendJson = new JsonObject();
			sendJson.addProperty("fileName", fileName);
			sendJson.addProperty("msgtype", 6);
			sendJson.addProperty("port", port);
			sendJson.addProperty("fileSize",filesize);
			write(client,sendJson);
		
			return 1;
		}
		
		try {
			if(mysql.Search(toName).next()){
				return 0;
			}
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return -1;
	}

	public void readWork(SocketChannel client,SelectionKey key) {
	
		String buffer = new String(Buff.array()); 
		int pos = buffer.indexOf("\r\n");
		buffer = buffer.substring(0, pos);
		String respond = null;
		
		JsonParser parser = new JsonParser();
		JsonObject recvJson = (JsonObject) parser.parse(buffer);
		JsonObject sendJson = new JsonObject();
		sendJson.addProperty("msgtype", 10);
		
		switch(recvJson.get("msgtype").getAsInt()){
		
			case 0://登陆
				String name = recvJson.get("name").getAsString();
				String pwd = recvJson.get("pwd").getAsString();
				
				try {
					if(SelectorServer.usermap.containsKey(name)){
						respond = name + " login fail!! 该用户已登录！！";
						break ;
					}
					
					ResultSet result = mysql.Search(name);
					
					if(result.next()){
						if(pwd.equals(result.getString(2))){
							respond = name + " login success!!";
							broadCast(3,name,null);
							SelectorServer.usermap.put(name, client);
							SelectorServer.userinfo.get(client).setName(name);
							loginMsg(client, name);
							//loginFile(0,client,name);
							break ;
						}
					}
					respond = name + " login fail!!  该用户不存在";
					break ;
					
				} catch (SQLException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
			case 1://注册
				String myname = recvJson.get("name").getAsString();
				String mypwd = recvJson.get("pwd").getAsString();
				if(mysql.insert(myname, mypwd)){
					//System.out.println("true");
					respond = "register success!!";
				}else{
					//System.out.println("false");
					respond = "register fail!!用户名已存在";
				}
				
				break ;
			case 2://聊天
				String fromName = recvJson.get("from").getAsString();
				String toName = recvJson.get("to").getAsString();
				String type = recvJson.get("type").getAsString();
				String msg = recvJson.get("msg").getAsString();
		
				int flg = sendMsg(fromName,toName,type,msg);
				
				if(flg == 1){
					respond = "msg send " + toName + " success!!";

				}else if(flg == 0){
					if(type.equals("talk")){
						respond =  toName + " 已离线!!";
						mysql.insert(fromName, toName, msg);
					}else if(type.equals("putfile")){
						respond =  toName + " 已离线!! 是否发送离线文件??";
					}else{
						respond =  toName + " 已离线!!";
					}
				}else{
					respond =  toName + "用户不存在！！";
				}
				
				break ;
				
			case 3:
				long finishSize = recvJson.get("ack").getAsInt();
				String fileNmae = recvJson.get("fileNmae").getAsString();
				ClientInfo cli = SelectorServer.userinfo.get(client);
				Iterator<FileInfo> it = cli.getFileList().iterator(); 
				while(it.hasNext()){
					FileInfo fileInfo = it.next();
					if(fileInfo.getFileName().equals(fileNmae)){
						fileInfo.setFinishSize(finishSize);
						if(fileInfo.getFileSize() == fileInfo.getFinishSize()){
							it.remove();
						}
						
						break;
					}
				}
				
				return ;
			case 4://回复服务器消息
				String ack = recvJson.get("ack").getAsString();
				if(ack.contains("no")){
					return ;
				}
				
				loginFile(4, client,recvJson.get("name").getAsString());
				return;
			case 5://查看所有在线用户
				Set<String> set = SelectorServer.usermap.keySet();
				respond = set.toString();
				break;
			case 6://上传文件
				long filesize = recvJson.get("fileSize").getAsLong();
				int port = recvJson.get("port").getAsInt();
				String fileName = recvJson.get("fileName").getAsString();
				String peer = recvJson.get("toName").getAsString();
				
				int rval = sendFileInfo(peer, fileName,filesize, port);
				
				if(rval == 0){
					respond =  peer + " 已离线!! 是否发送离线文件??";
				}else if(rval == -1){
					respond =  peer + "用户不存在！！";
				}else{
					ClientInfo c = SelectorServer.userinfo.get(SelectorServer.usermap.get(peer));
					c.setName(peer);
					c.insertFileList(new FileInfo(fileName,filesize));
					return ;
				}
				break;
				
			case 8://回复对方请求
				String rename = recvJson.get("fromName").getAsString();
				String peername = recvJson.get("toName").getAsString();
				String str = recvJson.get("ack").getAsString();
				
				SocketChannel socket = SelectorServer.usermap.get(peername);
				JsonObject Json = new JsonObject();
				Json.addProperty("msgtype", 10);
				
				if(str.contains("no")){
					Json.addProperty("ack", rename + " refuse your ask！！");
				}else{
					Json.addProperty("ack", rename + " agree your ask！！");
				}
				
				write(socket,Json);
				return;
			case 9://退出
				String exitName = recvJson.get("name").getAsString();
				exit(client,exitName,key);
				try {
					System.out.println("one client close:"
							+client.getRemoteAddress());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return ;
		}
		sendJson.addProperty("ack",respond);
		write(client,sendJson);
	}
	
	public void writeWork(String news){
		broadCast(5, null, news);
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		int num = 0;
		while(rwSelector.isOpen()){
			
			try {
				num = rwSelector.select();
			
				if(num <= 0){
					Iterator<SocketChannel> it = SelectorServer.channelList.iterator();
					while(it.hasNext()){
						SocketChannel channel = it.next();
						channel.register(rwSelector, SelectionKey.OP_READ);
						SelectorServer.userinfo.put(channel, new ClientInfo());
						it.remove();
					}
					continue;
				}
				
				Iterator<SelectionKey> it = rwSelector.selectedKeys().iterator();
				while(it.hasNext()){
					
					boolean flg = false;
					
					SelectionKey key = it.next();
					it.remove();
					
					if(key.isValid() && key.isReadable()){
						//System.out.println("enter read");
						SocketChannel client =  (SocketChannel)key.channel();
						int cnt = 0;
						try {
							cnt = client.read(Buff);
							if(cnt <= 0){
								flg = true;
								continue ;
							}
							
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							flg = true;
						}finally{
							
							if(flg){
								System.out.println("one client excepte close!!"+client.getRemoteAddress());
								
								ClientInfo clientinfo = SelectorServer.userinfo.get(client);
								String name = clientinfo.getName();
								exit(client,name,key);
								continue ;
							}	
						}
						readWork(client,key);
						
						Buff.flip();
						Buff.clear();
						
					}else if(key.isValid() && key.isWritable()){
						
						writeWork(null);
					}
					
				}
			}catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
		
		try {
			rwSelector.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mysql.close();
	}	
}