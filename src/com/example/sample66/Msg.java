package com.example.sample66;

public class Msg {
	public static final int SEND_TYPE = 1; 
	public static final int RECEIVED_TYPE = 0;
	private String content ;
	private int type ;
	
	public Msg(String content,int type){
		this.content = content;
		this.type = type;
	}
	
	public String getContent(){
		return this.content; 
	}
	
	public int getType(){
		return this.type;
	}
}
