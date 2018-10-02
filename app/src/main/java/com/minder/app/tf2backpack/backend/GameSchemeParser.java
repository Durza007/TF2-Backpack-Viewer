package com.minder.app.tf2backpack.backend;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.minder.app.tf2backpack.Attribute.ItemAttribute;
import com.minder.app.tf2backpack.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class GameSchemeParser {
	public static class TF2Weapon {
		public static final int USED_BY_SCOUT = 1 << 0;
		public static final int USED_BY_PYRO = 1 << 1;
		public static final int USED_BY_DEMOMAN = 1 << 2;
		public static final int USED_BY_SOLDIER = 1 << 3;
		public static final int USED_BY_HEAVY = 1 << 4;
		public static final int USED_BY_ENGINEER = 1 << 5;
		public static final int USED_BY_MEDIC = 1 << 6;
		public static final int USED_BY_SNIPER = 1 << 7;
		public static final int USED_BY_SPY = 1 << 8;

		
		public static final int USED_BY_ALL = USED_BY_SCOUT | USED_BY_PYRO | USED_BY_DEMOMAN |
			USED_BY_SOLDIER | USED_BY_HEAVY | USED_BY_ENGINEER | USED_BY_MEDIC | USED_BY_SNIPER | USED_BY_SPY;

		private int defindex;
		private String item_slot;
		private int item_quality;
		private String item_type_name;
		private String item_name;
		private String image_url;
		private String image_url_large;
		private String item_description;
		private ItemAttribute[] attributes;
		private String[] used_by_classes;
		
		public void setQuality(int quality){
			this.item_quality = quality;
		}
		
		public int getQuality(){
			return this.item_quality;
		}
		
		public void setItemSlot(String itemSlot) {
			this.item_slot = itemSlot;
		}

		public String getItemSlot() {
			return item_slot;
		}

		public void setDefIndex(int defIndex){
			this.defindex = defIndex;
		}
		
		public int getDefIndex(){
			return this.defindex;
		}
		
		public void setItemTypeName(String itemTypeName) {
			this.item_type_name = itemTypeName;
		}
		
		public String getItemTypeName() {
			return this.item_type_name;
		}
		
		public void setItemName(String itemName){
			this.item_name = itemName;
		}
		
		public String getItemName(){
			return this.item_name;
		}
		
		public void setImageUrl(String imageUrl) {
			this.image_url = imageUrl;
		}

		public String getImageUrl() {
			return image_url;
		}

		public void setImageUrlLarge(String imageUrlLarge) { this.image_url_large = imageUrlLarge; }
		
		public String getLargeImageUrl() {
			return image_url_large;
		}
		
		public void setItemDescription(String itemDescription) {
			this.item_description = itemDescription;
		}

		public String getItemDescription() {
			return item_description;
		}
		
		public ItemAttribute[] getAttributes() {
			return attributes;
		}
		
		public TF2Weapon(){
			defindex = 0;
			item_name = "";
			setImageUrl("");
		}
		
		public TF2Weapon(int defIndex, String itemTypeName, String itemName, String imageName){
			this.defindex = defIndex;
			this.item_type_name = itemTypeName;
			this.item_name = itemName;
			this.setImageUrl(imageName);
		}
		
		public ContentValues getSqlValues() {
			final ContentValues values = new ContentValues(7);

			int usedByClasses = USED_BY_ALL;
			if (used_by_classes != null && used_by_classes.length != 0) {
				usedByClasses = 0;
				for (String className : used_by_classes) {
					if (className.equals("Scout")) {
						usedByClasses |= USED_BY_SCOUT;
					}
					else if (className.equals("Pyro")) {
						usedByClasses |= USED_BY_PYRO;
					}
					else if (className.equals("Demoman")) {
						usedByClasses |= USED_BY_DEMOMAN;
					}
					else if (className.equals("Soldier")) {
						usedByClasses |= USED_BY_SOLDIER;
					}
					else if (className.equals("Heavy")) {
						usedByClasses |= USED_BY_HEAVY;
					}
					else if (className.equals("Engineer")) {
						usedByClasses |= USED_BY_ENGINEER;
					}
					else if (className.equals("Medic")) {
						usedByClasses |= USED_BY_MEDIC;
					}
					else if (className.equals("Sniper")) {
						usedByClasses |= USED_BY_SNIPER;
					}
					else if (className.equals("Spy")) {
						usedByClasses |= USED_BY_SPY;
					}
					else {
						Log.d(Util.GetTag(), "Item " + defindex + " has unkown used by class: " + className);
					}
				}
			}
			
			values.put("name", item_name);
			values.put("defindex", defindex);
			values.put("item_slot", item_slot);
			values.put("quality", item_quality);
			values.put("type_name", item_type_name);
			values.put("description", item_description);
			values.put("proper_name", 0);
			values.put("used_by_classes", usedByClasses);
			values.put("image_url", image_url);
			values.put("image_url_large", image_url_large);
			
			return values;
		}
	}
	
	public Exception error;
	
	//private JSONObject jObject;
	
	private SQLiteDatabase sqlDb;
	private int nextItemId = -1;
	private int parsedItems = 0;
	
	public int getNextStart() { return nextItemId; }
	public int getParsedItems() { return parsedItems; }
	
	public GameSchemeParser(InputStream inputStream, SQLiteDatabase sqlDb) throws IOException {
		this.sqlDb = sqlDb;
		
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
				if (reader.nextInt() != 1)
					throw new IOException("Status not 1");
			} else if (name.equals("next")) {
				nextItemId = reader.nextInt();
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
			TF2Weapon item = gson.fromJson(reader, TF2Weapon.class);

			int itemColor = 0;
			int itemColor2 = 0;
			if (item.getAttributes() != null) {
				for (ItemAttribute attribute : item.getAttributes()) {
					if (attribute.getName().equals("set item tint RGB")) {
						itemColor = (int) attribute.getFloatValue();
						if (itemColor == 1) itemColor = 0;
					}
					else if (attribute.getName().equals("set item tint RGB 2")) {
						itemColor2 = (int) attribute.getFloatValue();
					}
					sqlDb.execSQL("INSERT INTO item_attributes (itemdefindex, attributedefindex, value)\n" +
									"SELECT ?, defindex, ? FROM attributes WHERE name=?",
							new Object[]{ item.getDefIndex(), attribute.getFloatValue(), attribute.getName() }
					);
				}
			}

			if (itemColor != 0 || itemColor2 != 0) {
				item.setImageUrl("paint:" + itemColor + ":" + itemColor2 + ":" + item.getImageUrl());
				item.setImageUrlLarge("paint:" + itemColor + ":" + itemColor2 + ":" + item.getLargeImageUrl());
			}

			parsedItems++;
			sqlDb.insert("items", null, item.getSqlValues());
		}
		reader.endArray();
	}
}