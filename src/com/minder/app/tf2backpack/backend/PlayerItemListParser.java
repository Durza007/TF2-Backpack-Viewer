package com.minder.app.tf2backpack.backend;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.minder.app.tf2backpack.Attribute.ItemAttribute;

public class PlayerItemListParser {
	private ArrayList<Item> itemList;
	private int statusCode;
	private int numberOfBackpackSlots;
	
	public ArrayList<Item> getItemList(){
		return itemList;
	}	
	
	public int getStatusCode() {
		return statusCode;
	}
	
	public int getNumberOfBackpackSlots() {
		return numberOfBackpackSlots;
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
								item.setBackpackPosition(item.ExtractBackpackPosition(valArray.getLong(arrayIndex)));
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
							
							//if (attribute.getAttributeDefIndex() == 143){
								//Log.d(Util.GetTag(), "LOL");
							//}
							
							if (!attributeObject.isNull("float_value")){
								attribute.setFloatValue((float)attributeObject.getDouble("float_value"));
							}
							// find colors
							if (attribute.getAttributeDefIndex() == 142){
								item.setColor((int)attribute.getFloatValue());
							}
							
							if (attribute.getAttributeDefIndex() == 134){
								item.setParticleEffect((int)attribute.getFloatValue());
							}
							item.addAttribute(attribute);
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
