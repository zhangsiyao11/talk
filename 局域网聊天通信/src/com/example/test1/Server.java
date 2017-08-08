package com.example.test1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/*
 *广播 broadCast:
 * 3 有用户上线
 * 4 有用户下线
 *
 * msgtype
 * 0 登陆
 * 1 聊天
 * 5 查看在线用户
 * 9 退出
 * 10 服务器消息
 * 11 广播消息
 * 12 聊天消息
 * */

class ClientCommunate  implements Runnable { //客户端线程
	private Socket client;
    private String name;
    private Scanner in;
    private PrintWriter out;
	//ClientInfo clientInfo;
	
	public ClientCommunate(Socket client) {
		this.client = client;
		try {
			in = new Scanner(client.getInputStream());
			out = new PrintWriter(client.getOutputStream(), true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void broadCast(int num) {//发送广播消息
		Iterator<PrintWriter> iterator = Server.map.values().iterator();
		while(iterator.hasNext()){
			PrintWriter pout = iterator.next();
			JsonObject sendJson = new JsonObject();
			sendJson.addProperty("msgtype", 11);
				
			switch(num){
				case 3:
					sendJson.addProperty("ack",name + "上线了！！！");
					pout.println(sendJson.toString());
					break;
				case 4:
					sendJson.addProperty("ack",name + "下线了！！！");
					pout.println(sendJson.toString());
			}
		}
	}
	
	public boolean sendMsg(String peername,String msg)  {//发送聊天信息
		
		PrintWriter pout = null;
		if((pout=Server.map.get(peername)) != null){
			JsonObject sendJson = new JsonObject();
			sendJson.addProperty("msgtype", 12);
			sendJson.addProperty("from", name);
			sendJson.addProperty("msg", msg);
			pout.println(sendJson.toString());
			
			return true;
		}
		return false;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		System.out.println("thread "+Thread.currentThread().getId() + " begin supply service for:" + client.getRemoteSocketAddress());

		while(!Thread.currentThread().isInterrupted()){
			String buffer = null;
			try{
				buffer = in.nextLine();
			}catch(Exception e){
				Server.map.remove(name, out);
				broadCast(4);
				System.out.println("thread "+Thread.currentThread().getId() + " end supply service for:" + client.getRemoteSocketAddress());
			}
			
			JsonParser parser = new JsonParser();
			JsonObject recvJson = (JsonObject) parser.parse(buffer);
			JsonObject sendJson = new JsonObject();
			sendJson.addProperty("msgtype", 10);
			switch(recvJson.get("msgtype").getAsInt()){
				case 0://登陆
					name = recvJson.get("name").getAsString();

					if(!Server.map.containsKey(name)){
						//如果用户名已出现在list中，登录失败，否则登陆成功，并将用户信息添加入list
						sendJson.addProperty("ack",name + " login success!!");
						broadCast(3);
						Server.map.put(name, out);
						out.println(sendJson.toString());
					}else{
						sendJson.addProperty("ack",name + " login fail!!");
						out.println(sendJson.toString());
						System.out.println("thread "+Thread.currentThread().getId() + " end supply service for:" + client.getRemoteSocketAddress());
						Thread.currentThread().interrupt();		
					}
					
					break;
				case 1://聊天
					String peername = recvJson.get("to").getAsString();
					String msg = recvJson.get("msg").getAsString();
					sendJson.addProperty("msgtype", 10);
					
					if(sendMsg(peername,msg)){
						sendJson.addProperty("ack","msg send " + peername + " success!!");
					}else{
						sendJson.addProperty("ack","msg send " + peername + " fail!!");
					}
					out.println(sendJson.toString());
					break;
				case 5://查看所有在线用户
					Set<String> set = Server.map.keySet();
					sendJson.addProperty("ack",set.toString());
					out.println(sendJson.toString());
					break;
				case 9://退出
					Server.map.remove(name, out);
					broadCast(4);
					System.out.println("thread "+Thread.currentThread().getId() + " end supply service for:" + client.getRemoteSocketAddress());
					Thread.currentThread().interrupt();		
			}
		}
		in.close();
		out.close();
		try {
			client.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

class ManagerThread implements Runnable{//管理员线程
	
	private String news;

	public void broadCast(){
		JsonObject sendJson = new JsonObject();
		sendJson.addProperty("msgtype", 11);
		sendJson.addProperty("ack", news);
		Iterator<PrintWriter> iterator = Server.map.values().iterator();
		while(iterator.hasNext()){
			PrintWriter pout = iterator.next();
			pout.println(sendJson.toString());
		}
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(!Thread.currentThread().isInterrupted()){
			Scanner in = new Scanner(System.in);
			news = in.nextLine();
			broadCast();
		}	
	}
}

public class Server {
	
	private static ExecutorService threadPool = Executors.newCachedThreadPool();
	//public static ConcurrentLinkedQueue<ClientInfo> list = new ConcurrentLinkedQueue<ClientInfo>();//存放客户端信息（套接字，名字，输出流）
	public static ConcurrentHashMap<String, PrintWriter> map = new ConcurrentHashMap<String, PrintWriter>(); 
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		//先创建ServerSocket
		try {
			System.out.println("server starting listen 6000...");
			ServerSocket server = new ServerSocket(6000);
			threadPool.submit(new ManagerThread());//开启管理员线程
			
			while(!Thread.currentThread().isInterrupted()){
				Socket client = server.accept();   // 服务器线程阻塞getTaskCount()
				//System.out.println(((ThreadPoolExecutor)threadPool).getActiveCount());
				threadPool.submit(new ClientCommunate(client));
			}
			
			server.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			threadPool.shutdown();
		}
	}
}