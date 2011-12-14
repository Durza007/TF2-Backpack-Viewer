package com.minder.app.tf2backpack;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Parcel;
import android.os.Parcelable;

import com.minder.app.tf2backpack.Attribute.ItemAttribute;

public class PlayerItemListParser {
	public static class Item implements Parcelable {
		private int defIndex;
		private String customName;
		private String customDesc; 
		private int level;
		private int quality;
		private int quantity;
		private long inventoryToken;
		private int backpackPosition;
		private int itemColor;
		private int particleEffect;
		private boolean equipped;
		private boolean notTradable;
		private ArrayList<ItemAttribute> attributeList;
		
		public ArrayList<ItemAttribute> getAttributeList() {
			return attributeList;
		}
		
		public void addAttribute(ItemAttribute item){
			this.attributeList.add(item);
		}

		public void setLevel(int level) {
			this.level = level;
		}

		public int getLevel() {
			return level;
		}

		public int getDefIndex(){
			return defIndex;
		}
		
		public void setQuality(int quality) {
			this.quality = quality;
		}

		public int getQuality() {
			return quality;
		}

		public int getQuantity() {
			return quantity;
		}

		public int getBackPackPosition(){
			return backpackPosition;
		}
		
		public int getColor(){
			return itemColor;
		}
		
		public void setColor(int color){
			itemColor = color;
		}
		
		public boolean isEquipped(){
			return equipped;
		}
		
		public void setNotTradable(boolean notTradable) {
			this.notTradable = notTradable;
		}

		public boolean isNotTradable() {
			return notTradable;
		}
		
		public String getCustomDesc() {
			return customDesc;
		}

		public String getCustomName() {
			return customName;
		}
		
		public Item() {
			this.itemColor = 0;
			this.notTradable = false;
			this.level = -1;
			attributeList = new ArrayList<ItemAttribute>();
		}
		
		public Item(int defIndex, int level, int quality, int quantity, long inventoryToken){
			this.defIndex = defIndex;
			this.setLevel(level);
			this.setQuality(quality);
			this.quantity = quantity;
			this.inventoryToken = inventoryToken;
			this.backpackPosition = ExtractBackpackPosition(inventoryToken);
			this.itemColor = 0;
			this.notTradable = false;
			this.attributeList = new ArrayList<ItemAttribute>();
		}
		
		public Item(Parcel in){		
			this.defIndex = in.readInt();
			this.customDesc = in.readString();
			this.customName = in.readString();
			this.level = in.readInt();
			this.quality = in.readInt();
			this.itemColor = in.readInt();
			this.particleEffect = in.readInt();
			boolean[] bools = new boolean[2];
			in.readBooleanArray(bools);
			this.equipped = bools[0];
			this.notTradable = bools[1];
			this.attributeList = new ArrayList<ItemAttribute>();
			in.readTypedList(this.attributeList, ItemAttribute.CREATOR);
		}
		
		// find out if the item is equipped at the same time
		public int ExtractBackpackPosition(long inventoryToken){
			// awarded but not yet given
			if (inventoryToken == 0) {
				return -1;
			}
			// check if it is equipped
			if ((inventoryToken & 0x0FFF0000) != 0){
				equipped = true;
			}
			return (int)(inventoryToken & 0xFFFF) - 1;
		}

		public void setParticleEffect(int particleEffect) {
			this.particleEffect = particleEffect;
		}

		public int getParticleEffect() {
			return particleEffect;
		}

		//Parcelable functions
		public int describeContents() {
			return 0;
		}

		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(defIndex);
			dest.writeString(customDesc);
			dest.writeString(customName);
			dest.writeInt(level);
			dest.writeInt(quality);
			dest.writeInt(itemColor);
			dest.writeInt(particleEffect);
			dest.writeBooleanArray(new boolean[] {equipped, notTradable});
			dest.writeTypedList(attributeList);
		}
		
		public static final Parcelable.Creator<Item> CREATOR = new Parcelable.Creator<Item>() 
		{
			public Item createFromParcel(Parcel source) {
				return new Item(source);
			}

			public Item[] newArray(int size) {
				return new Item[size];
			}
			
		};
	}
	
	private JSONObject jObject;
	private ArrayList<Item> itemList;
	private int statusCode;
	private int numberOfBackpackSlots;
	
	public ArrayList<Item> GetItemList(){
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
			jObject = new JSONObject(data);
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
								item.defIndex = valArray.getInt(arrayIndex);
							} else if (type.equals("level")){
								item.setLevel(valArray.getInt(arrayIndex));
							} else if (type.equals("quality")){
								item.quality = valArray.getInt(arrayIndex);
							} else if (type.equals("quantity")){
								item.quantity = valArray.getInt(arrayIndex);
							} else if (type.equals("inventory")){
								item.backpackPosition = item.ExtractBackpackPosition(valArray.getLong(arrayIndex));
							} else if (type.equals("flag_cannot_trade")){
								item.notTradable = valArray.getBoolean(arrayIndex);
							} else if (type.equals("custom_name")){
								item.customName = valArray.getString(arrayIndex);
							} else if (type.equals("custom_desc")){
								item.customDesc = valArray.getString(arrayIndex);
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
