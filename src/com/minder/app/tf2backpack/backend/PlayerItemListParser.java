package com.minder.app.tf2backpack.backend;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.minder.app.tf2backpack.Attribute.ItemAttribute;

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

	public PlayerItemListParser(String data) {
		try {
			JSONObject jObject = new JSONObject(data);
			
			JSONObject resultObject = jObject.getJSONObject("result");
			//Check if everything is OK
			if (resultObject.getString("status").equals("1")){
				statusCode = 1;
				itemList = new ArrayList<Item>();
				numberOfBackpackSlots = resultObject.getInt("num_backpack_slots");
				
				// New format in IEcon_items
				//JSONObject itemsObject = resultObject.getJSONObject("items");
				JSONArray itemArray = resultObject.getJSONArray("items");
				
				for (int index = 0; index < itemArray.length(); index++){
					JSONObject itemObject = itemArray.getJSONObject(index);
					// Parse the info to a item object
					/*Item item = new Item(itemObject.getInt("defindex"),
							itemObject.getInt("level"),
							itemObject.getInt("quality"),
							itemObject.getInt("quantity"),
							Long.parseLong(itemObject.getString("inventory")));*/
					
					// Magick =)
					JSONArray nameArray = itemObject.names();
					JSONArray valArray = itemObject.toJSONArray(nameArray);
					
					Item item = new Item();
					
					for (int arrayIndex = 0; arrayIndex < nameArray.length(); arrayIndex++){
						String type = nameArray.getString(arrayIndex);
						if (type != null){
							if (type.equals("defindex")){
								item.setDefIndex(valArray.getInt(arrayIndex));
							} else if (type.equals("level")){
								item.setLevel(valArray.getInt(arrayIndex));
							} else if (type.equals("quality")){
								item.setQuality(valArray.getInt(arrayIndex));
							} else if (type.equals("quantity")){
								item.setQuantity(valArray.getInt(arrayIndex));
							} else if (type.equals("inventory")){
								//item.setBackpackPosition(item.extractBackpackPosition(valArray.getLong(arrayIndex)));
							} else if (type.equals("flag_cannot_trade")){
								item.setNotTradable(valArray.getBoolean(arrayIndex));
							} else if (type.equals("flag_cannot_craft")){
								item.setNotCraftable(valArray.getBoolean(arrayIndex));
							} else if (type.equals("custom_name")){
								item.setCustomName(valArray.getString(arrayIndex));
							} else if (type.equals("custom_desc")){
								item.setCustomDesc(valArray.getString(arrayIndex));
							}
						}
					}
					
					
					try {
						JSONArray attributeArray = itemObject.getJSONArray("attributes");
						
						for (int attributeIndex = 0; attributeIndex < attributeArray.length(); attributeIndex++){
							JSONObject attributeObject = attributeArray.getJSONObject(attributeIndex);
							
							ItemAttribute attribute = new ItemAttribute();						
							
							attribute.setAttributeDefIndex(attributeObject.getInt("defindex"));
							attribute.setValue(attributeObject.getLong("value"));
							
							if (!attributeObject.isNull("float_value")){
								attribute.setFloatValue((float)attributeObject.getDouble("float_value"));
							}
							
							// find colors
							if (attribute.getAttributeDefIndex() == 142){
								item.setColor((int)attribute.getFloatValue());
							}
							
							// particle effect
							if (attribute.getAttributeDefIndex() == 134){
								item.setParticleEffect((int)attribute.getFloatValue());
							}
							
							// account info
							if (!attributeObject.isNull("account_info")) {
								JSONObject accountInfo = attributeObject.getJSONObject("account_info");
								attribute.setAccountInfo(accountInfo.getLong("steamid"), accountInfo.getString("personaname"));
							}
							
							//item.addAttribute(attribute);
						}
					} catch (JSONException e) {
						// We didn't find attribute for this item
					}
					
					itemList.add(item);
				}
			} else {
				statusCode = Integer.parseInt(resultObject.getString("status"));
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
			
			if (e instanceof JSONException) {
				int index = data.indexOf("<title>");
				if (index != -1) {
					int endIndex = data.indexOf("</title>");
					if (endIndex != -1) {
						error = new SteamException(data.substring(index + 7, endIndex));
					}
				}
			}
		}
	}

}
