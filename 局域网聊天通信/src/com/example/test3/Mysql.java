 package com.example.test3;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.google.gson.JsonObject;

public class Mysql {
	private static Connection conn;
	private static Statement state;
	private static ResultSet result;
	private static PreparedStatement prestatement;
	
	public Mysql(){
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		String url = "jdbc:mysql://localhost:3306/talk";
        try {
			conn = DriverManager.getConnection(url,"root","123456");
			state = conn.createStatement();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
         System.out.println("connect mysql server success!");
	}
	
	public void insert(String fromName,String toName,String msg){
		
		String sql = "insert into msgborad values(?, ?, ?);";
 		try {
 			prestatement = conn.prepareStatement(sql);
 			
 			prestatement.setString(1, fromName);
 			prestatement.setString(2, toName);
 			prestatement.setString(3, msg);
 			
 			prestatement.executeUpdate();
 		} catch (Exception e2) {
 			// TODO Auto-generated catch block
 		}

	}
	
	public void insertFile(String name,String path,long fileSize,long finishSize){
		String sql = "insert into file values(?,?,?,?);";
 		try {
 			prestatement = conn.prepareStatement(sql);
 			
 			prestatement.setString(1, name);
 			prestatement.setString(2, path);
 			prestatement.setLong(3, fileSize);
 			prestatement.setLong(4, finishSize);
 			
 			prestatement.executeUpdate();	
 		} catch (SQLException e2) {
 			// TODO Auto-generated catch block
 		}
	}
	
	public void deleteFile(String name){
		String sql = "delete  from file where toName = '"+ name +"'";
		try {
			state.executeUpdate(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	public ResultSet getMsg(String name) throws SQLException  {
		String sql = "select * from msgBorad where toName = '"+name+"'";
		try {
			result = state.executeQuery(sql);

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
	public ResultSet getFile(String name) throws SQLException  {
		String sql = "select * from file where toName = '"+name+"'";
		try {
			result = state.executeQuery(sql);

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

	public void close(){
		try {
			conn.close();
			state.close();
			result.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	public void deleteMsg(String name){
		String sql = "delete  from msgborad where toName = '"+ name +"'";
		try {
			state.executeUpdate(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	public boolean insert(String name,String pwd){
		String sql = "insert into user values(?, ?);";
 		try {
 			prestatement = conn.prepareStatement(sql);
 			
 			prestatement.setString(1, name);
 			prestatement.setString(2, pwd);
 			
 			if(prestatement.executeUpdate() == 1){
 				return true;
 			}
 		} catch (Exception e2) {
 			// TODO Auto-generated catch block
 			return false;
 		}
		return false;
	}
	
	public void deleteUser(String name){
		String sql = "delete  from user where name = '"+ name +"'";
		try {
			state.executeUpdate(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	public ResultSet Search(String name)   {
		String sql = "select * from user where name = '"+name+"'";
		try {
			result = state.executeQuery(sql);

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		return result;
		
	}
	
	
}
