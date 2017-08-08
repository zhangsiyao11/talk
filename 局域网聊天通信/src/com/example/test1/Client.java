package com.example.test1;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.example.test3.GetFile;
import com.example.test3.PutFile;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/*
 * 10 服务器消息 
 * 11  广播
 * 12 聊天信息  from :   
 *          msg :
 * */

class RecvThread implements Runnable{
	private Socket socket;
	private Scanner in;
	private PrintWriter out;
	
	public RecvThread(Socket socket,Scanner in,PrintWriter out){
		this.socket = socket;
		this.in = in;
		this.out = out;
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			
			while(!Thread.currentThread().isInterrupted()){
			
				String buffer = in.nextLine();
				
				JsonParser parser = new JsonParser();
				JsonObject recvJson = (JsonObject) parser.parse(buffer);
		
				int msgType = recvJson.get("msgtype").getAsInt();
				switch(msgType){
					case 6:
						long fileSize = recvJson.get("fileSize").getAsLong();
						int port = recvJson.get("port").getAsInt();
						String fileName =  recvJson.get("fileName").getAsString();
						Client.pool.submit(new GetFile(fileName,port,fileSize,0,out));
						break ;
					case 10:
						System.out.println(recvJson.get("ack").getAsString());
						break;
					case 11:
						System.out.println("广播:" + recvJson.get("ack").getAsString());
						break;
					case 12:
						System.out.println("from " + recvJson.get("from").getAsString() + " : " + recvJson.get("msg").getAsString());
				}
			}
			socket.close();
			in.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

public class Client {

	public static ExecutorService pool = Executors.newCachedThreadPool();
	private static Future<?> future;
	
	//public void 
	
	public static void main(String[] args)  {
		// TODO Auto-generated method stub
		
		//包括创建client通信的socket， 并且链接服务器6000端口
		Socket socket = null;
		try {
			socket = new Socket("127.0.0.1", 6000);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
			
		try {
			Scanner in = new Scanner(socket.getInputStream());
			PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
			JsonObject json = new JsonObject();
			
			future = pool.submit(new RecvThread(socket,in,out));
			
			Scanner scn = new Scanner(System.in);
			System.out.println("input 0---->login  input 1----> registe  input 2---->talk input 4---->respondserver input 5---->peer  input 6---->putFile  input 7---->getFile input 8---->respondpeer input 9---->exit ");
			String name = null;
			
			while(!Thread.currentThread().isInterrupted()){
				System.out.println("input msgtype:");
				
				int flg = scn.nextInt();
					
				JsonObject sendJson = new JsonObject();
				sendJson.addProperty("msgtype", flg);

				switch(flg){
					case 0:
						System.out.print("input name:");
						name = scn.next();
						System.out.print("input pwd:");
						sendJson.addProperty("name", name);
						sendJson.addProperty("pwd", scn.next());
						out.println(sendJson.toString());
						break ;
					case 1:
						System.out.print("input name:");
						name = scn.next();
						System.out.print("input pwd(1~6位):");
						sendJson.addProperty("name", name);
						sendJson.addProperty("pwd", scn.next());
						out.println(sendJson.toString());
						break ;
					case 2:
						sendJson.addProperty("from", name);
						System.out.print("input peer name:");
						sendJson.addProperty("to", scn.next());
						System.out.print("input msg type(talk/putfile/getfile) :");
						String type = scn.next();
						sendJson.addProperty("type", type);
						System.out.print("input msg or filepath:");
						String str = scn.next();
						if(type.contains("put")){
							File file = new File(str);
							if(!file.exists() || !file.isFile()){
								System.out.println("文件不存在!!!");
								break;
							}
						}
						sendJson.addProperty("msg",str);
						out.println(sendJson.toString());
						break;
					case 4:
						System.out.print("input yes or no:");
						sendJson.addProperty("ack", scn.next());
						out.println(sendJson.toString());
						break ;
					case 5:
						out.println(sendJson.toString());
						break;
					case 6:
						System.out.print("input peer name:");
						String n = scn.next();
						System.out.print("input filepath:");
						String path = scn.next();
						System.out.print("input port:");
						int port = scn.nextInt();
						String[] strings = path.split("/");
						String fileName = null;
						for(String s : strings){
							fileName = s;
						}
						
						File file = new File(path);
						long fileSize = file.length();
						
						sendJson.addProperty("toName", n);
						sendJson.addProperty("fileName", fileName);
						sendJson.addProperty("port", port);
						sendJson.addProperty("fileSize", fileSize);
						out.println(sendJson.toString());
						pool.submit( new PutFile(file,port,0));
						break;
					case 7:
						break;
					case 8:
						System.out.print("input peer name:");
						sendJson.addProperty("fromName", name);
						sendJson.addProperty("toName", scn.next());
						System.out.print("input yes or no:");
						sendJson.addProperty("ack", scn.next());
						out.println(sendJson.toString());
						break;
					case 9:
						sendJson.addProperty("name", name);
						out.println(sendJson.toString());
						future.cancel(true);
						System.out.println("下线成功！！");
						Thread.currentThread().interrupt();
				}
			}
			
			socket.close();
			in.close();
			out.close();
			
		} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		}finally{
			pool.shutdown();
		}
			
	}
}