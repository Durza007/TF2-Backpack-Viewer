package com.minder.app.tf2backpack.backend;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

public class PlayerItemListParser {
	private List<Item> itemList;
	private int statusCode;
	private int numberOfBackpackSlots;
	private Exception error;
	
	public List<Item> getItemList(){
		return itemList;
	}	
	
	public int getStatusCode() {
		return statusCode;
	}
	
	public int getNumberOfBackpackSlots() {
		return numberOfBackpackSlots;
	}
	
	public Exception getException() {
		return this.error;
	}
	
	public PlayerItemListParser(InputStream inputStream) throws IOException {
		itemList = new LinkedList<Item>();
        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
        
        reader.beginObject();
        while (reader.hasNext()) {
        	String name = reader.nextName();
            if (name.equals("result")) {
            	parseResult(reader);
            } else {
            	reader.skipValue();
            }
        }

        reader.endObject();
        reader.close();
	}
	
	private void parseResult(JsonReader reader) throws IOException {
		reader.beginObject();
		
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("status")) {
				statusCode = reader.nextInt();
			} else if (name.equals("num_backpack_slots")) {
				numberOfBackpackSlots = reader.nextInt();
			} else if (name.equals("items")) {
				parseItems(reader);
			} else {
				reader.skipValue();
			}
		}
		
		reader.endObject();
	}
	
	private void parseItems(JsonReader reader) throws IOException {
		Gson gson = new Gson();
		reader.beginArray();
		
		while (reader.hasNext()) {	
			itemList.add((Item)gson.fromJson(reader, Item.class));
		}
		
		reader.endArray();
	}
}
