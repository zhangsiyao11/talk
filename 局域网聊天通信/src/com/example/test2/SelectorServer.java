package com.example.test2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.example.test1.Server;
import com.example.test3.Mysql;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class FileInfo{
	private String fileName;
	private long fileSize;
	private long finishSize;
	
	public FileInfo(String fileName, long fileSize) {
		this.fileName = fileName;
		this.fileSize = fileSize;
	}

	public String getFileName() {
		return fileName;
	}
	public void setFileName(String path) {
		this.fileName = path;
	}
	public long getFileSize() {
		return fileSize;
	}
	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}
	public long getFinishSize() {
		return finishSize;
	}
	public void setFinishSize(long finishSize) {
		this.finishSize += finishSize;
	}
}

class ClientInfo{
	private String name;
	private LinkedList<FileInfo> fileList ; 
	
	public ClientInfo() {
		this.name = null;
		fileList = new LinkedList<FileInfo>();
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public LinkedList<FileInfo> getFileList() {
		return fileList;
	}

	public void insertFileList(FileInfo finfo) {
		fileList.add(finfo);
	}
	
	
}



public class SelectorServer {
	public static String PATH;
	public static ConcurrentHashMap<String,SocketChannel> usermap ;
	public static ConcurrentHashMap<SocketChannel,ClientInfo> userinfo ;
	public static List<SocketChannel> channelList ;
	private Selector selector;
	private ServerSocketChannel serverChannel;
	private ExecutorService threadPool ;
	private WorkTask task;
	
	static{
		usermap = new ConcurrentHashMap<String, SocketChannel>();
		channelList = Collections.synchronizedList(new ArrayList<SocketChannel>());
		userinfo = new  ConcurrentHashMap<SocketChannel,ClientInfo>();
		PATH = "g:/data";
	}
	
	public SelectorServer(String ip,int port) throws IOException{
	
		selector = Selector.open();
		serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);
		serverChannel.bind(new InetSocketAddress(ip,port));
		serverChannel.register(selector, SelectionKey.OP_ACCEPT);
	
		threadPool = Executors.newSingleThreadExecutor();
		task = new WorkTask();
		
		threadPool.submit(task);
	}
	
	public void start() throws IOException{
		System.out.println("server supply service on 6000 port...");
		int num = 0;
		while(selector.isOpen()){
			
			num = selector.select();
			
			if( num <= 0){
				continue ;
			}
			
			Set<SelectionKey> channelSet = selector.selectedKeys();
			Iterator<SelectionKey> it = channelSet.iterator();
			while(it.hasNext()){
				SelectionKey key = it.next();
				it.remove();
	
				if(key.isValid() && key.isAcceptable()){
					
					SocketChannel clientChannel = serverChannel.accept();
					clientChannel.configureBlocking(false);
					channelList.add(clientChannel);
					task.getSelector().wakeup();
					System.out.println("new client connection:"
							+clientChannel.getRemoteAddress());
				}	
			}
		}
		
		selector.close();
	}


	public static void main(String[] args)  {
		// TODO Auto-generated method stub
		
		try {
			new SelectorServer("127.0.0.1",6000).start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
