package com.minder.app.tf2backpack.backend;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class DataServerConnection {
	//private final static String host = "tf2.dyndns-server.com";
	private final static String host = "192.168.0.137";
	private final static int port = 9999;
	
	private Socket socket;
	
	private InputStream input;
	private OutputStream output;
	
	public InputStream getInputStream() {
		return this.input;
	}
	
	public OutputStream getOutputStream() {
		return this.output;
	}
	
	public DataServerConnection() throws IOException {
		socket = new Socket(host, port);
		
		input = socket.getInputStream();
		output = socket.getOutputStream();
	}
	
	public long readLong() throws IOException {
		DataInputStream i = new DataInputStream(input);
		
		return i.readLong();
	}
}
