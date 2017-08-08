package com.example.test3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import com.google.gson.JsonObject;

public class GetFile implements Runnable {
	private static String PATH = "f:/";
	private File file;
	private String fileName;
	private SocketAddress address;
	private long fileSize;
	private DatagramSocket socket;
	private DatagramPacket packet;
	private PrintWriter severOut;
	private int start;

	public GetFile(String fileName, int port, long fileSize,int start,PrintWriter severOut) {
		System.out.println("fileSize:" + fileSize);
		this.fileName = fileName;
		PATH += fileName;
		this.file = new File(PATH);
		this.address = new InetSocketAddress("127.0.0.1",port);
		this.fileSize = fileSize;
		this.severOut = severOut;
		this.start = start;
		
		try {
			socket = new DatagramSocket();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void get(){
		
		try {
			packet = new DatagramPacket("hello".getBytes(),5,address);
			socket.send(packet);
		} catch (SocketException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		
		try {
			RandomAccessFile out = new RandomAccessFile(file,"rw");
			out.seek(start);
			
			long count = 0;
			int num = 0;
			double sum = 0;
			byte[] buffer  = new byte[500 * 1024];

			while(true){
				packet = new DatagramPacket(buffer, buffer.length,address);
				socket.receive(packet);
				num = packet.getLength();
				
				out.write(buffer,0,num);
				count += num;
				sum += count;
				
				if(count >= 10*1024 || sum >= fileSize){
					
					JsonObject Json = new JsonObject();
					Json.addProperty("msgtype", 3);
					Json.addProperty("fileNmae", fileName);
					Json.addProperty("msg", count);
					severOut.println(Json.toString());
					
					count = 0;
				}
				
				System.out.printf("getfile:%.2f\r",sum/fileSize);
				System.out.flush();
				

				if(sum >= fileSize){
					System.out.println("getfile:100.0%");
					System.out.println("get file finish!!");
					break ;
				}
			}
	
			out.close();
			socket.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		get();
	}
}
