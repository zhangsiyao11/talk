package com.example.test3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

import com.google.gson.JsonObject;
      
public class PutFile implements Runnable{
	
	private File file;
	private DatagramSocket socket;
	private DatagramPacket packet;
	private int start;
	
	public PutFile(File file,int port,int start){
		this.file = file;
		this.start = start;
		
		try {
			socket = new DatagramSocket(port);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void put(){
		
		try {
			RandomAccessFile in = new RandomAccessFile(file,"r");
			in.seek(start);
			
			byte[] buffer  = new byte[500 * 1024];
			double count = 0;
			int num = 0;
			packet = new DatagramPacket(buffer,num);
			socket.receive(packet);
			SocketAddress inet = new InetSocketAddress(packet.getAddress(),packet.getPort());
			
	
			while((num = in.read(buffer)) >= 0){
				packet = new DatagramPacket(buffer,num,inet);
				socket.send(packet);
				count += num;
				System.out.printf("putfile:%.2f\r",count/file.length());
				System.out.flush();
			}
			
			System.out.println("put file finish!!");
			
			in.close();
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
		put();
		
	}
}
