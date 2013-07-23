package com.minder.app.tf2backpack.backend;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.minder.app.tf2backpack.Attribute;
import com.minder.app.tf2backpack.Attribute.ItemAttribute;
import com.minder.app.tf2backpack.Util;

public class GameSchemeParser {
	public static class TF2Weapon {
		private int defindex;
		private String item_slot;
		private int item_quality;
		private String item_type_name;
		private String item_name;
		private String image_url;
		private String image_url_large;
		private String item_description;
		private ItemAttribute[] attributes;
		
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
		
		public String getSqlInsert() {
			if (this.item_description != null)
				return "(name, defindex, item_slot, quality, type_name, description, proper_name) VALUES " + 
					"(\"" + this.item_name + "\",\"" + this.defindex + "\",\"" + this.item_slot + "\",\"" + this.item_quality + "\",\"" + this.item_type_name + "\",\"" + this.item_description.replace("\"", "\"\"") + "\", \"0\")";
			else
				return "(name, defindex, item_slot, quality, type_name, description, proper_name) VALUES " + 
					"(\"" + this.item_name + "\",\"" + this.defindex + "\",\"" + this.item_slot + "\",\"" + this.item_quality + "\",\"" + this.item_type_name + "\",\"" + this.item_description + "\", \"0\")";
		}
	}
	
	public static class StrangeItemLevel {
		public int level;
		public int required_score;
		public String name;
	}
	
	public Exception error;
	
	//private JSONObject jObject;
	private List<TF2Weapon> itemList;
	
	public List<String> sqlExecList;
	private List<Attribute> attributeList;
	private List<ItemAttribute> itemAttributeList;
	
	private Context context;
	
	public List<TF2Weapon> getItemList(){
		return itemList;
	}
	
	public GameSchemeParser(InputStream inputStream, Context context) throws IOException {
		this.context = context;
		
		// init lists
		sqlExecList = new LinkedList<String>();
		itemAttributeList = new LinkedList<Attribute.ItemAttribute>();
		
        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
        
        reader.beginObject();
        if (reader.hasNext()) {
        	String name = reader.nextName();
            if (name.equals("result")) {
            	parseResult(reader);
            } else {
            	// TODO ERROR!
            }
        } else {
        	// TODO ERROR!
        }

        reader.endObject();
        reader.close();
        
        linkItemAttributes();
        
		if (sqlExecList != null){
			saveToDB();
		}
	}
	
	private void parseResult(JsonReader reader) throws IOException {
		reader.beginObject();
		while (reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("status")) {
				if (reader.nextInt() != 1)
					throw new IOException("Status not 1");
			} else if (name.equals("items")) {
				itemList = parseItems(reader);
			} else if (name.equals("attributes")) {
				attributeList = parseAttributes(reader);
			} else if (name.equals("attribute_controlled_attached_particles")) {
				parseParticleEffects(reader);
			} else if (name.equals("item_levels")) {
				parseStrangeItemLevels(reader);
			} else if (name.equals("kill_eater_score_types")) {
				parseStrangeScoreTypes(reader);
			} else {
				reader.skipValue();
			}
		}
		
		reader.endObject();
	}

	private List<TF2Weapon> parseItems(JsonReader reader) throws IOException {
		List<TF2Weapon> itemList = new LinkedList<GameSchemeParser.TF2Weapon>();
		Gson gson = new Gson();
		
		reader.beginArray();
		while (reader.hasNext()) {
			itemList.add((TF2Weapon)gson.fromJson(reader, TF2Weapon.class));
		}
		reader.endArray();
		
		return itemList;
	}
	
	private List<Attribute> parseAttributes(JsonReader reader) throws IOException {
		List<Attribute> attributes = new LinkedList<Attribute>();
		Gson gson = new Gson();
		
		reader.beginArray();
		while (reader.hasNext()) {
			attributes.add((Attribute)gson.fromJson(reader, Attribute.class));
		}
		reader.endArray();
		
		return attributes;
	}
	
	private void parseParticleEffects(JsonReader reader) throws IOException {	
		reader.beginArray();
		while (reader.hasNext()) {
			reader.beginObject();
			
			int id = -1;
			String particleName = null;
			
			while (reader.hasNext()) {
				String name = reader.nextName();
				if (name.equals("id")) {
					id = reader.nextInt();
				} else if (name.equals("name")) {
					particleName = reader.nextString();
				} else {
					reader.skipValue();
				}
			}
			reader.endObject();
			
			if (id != -1)
				sqlExecList.add("INSERT INTO particles (id, name) VALUES (\"" + id + "\", \"" + particleName + "\")"); 
		}	
		reader.endArray();
	}
	
	private void parseStrangeItemLevels(JsonReader reader) throws IOException {
		Gson gson = new Gson();
		reader.beginArray();
		
		while (reader.hasNext()) {
			reader.beginObject();
			String typeName = null;
			List<StrangeItemLevel> strangeItemLevels = new LinkedList<StrangeItemLevel>();
			
			while (reader.hasNext()) {
				String name = reader.nextName();
				if (name.equals("name")) {
					typeName = reader.nextString();
				} else if (name.equals("levels")) {
					reader.beginArray();
					
					while (reader.hasNext()) {
						strangeItemLevels.add(
								(StrangeItemLevel)gson.fromJson(reader, StrangeItemLevel.class));
					}
					
					reader.endArray();
				} else {
					reader.skipValue();
				}
			}
			
			reader.endObject();
			
			if (typeName != null) {
				sqlExecList.add("INSERT INTO strange_item_levels (type_name) VALUES (\"" + typeName + "\")");
				sqlExecList.add("CREATE TABLE " + typeName + " (level INTEGER PRIMARY KEY, required_score INTEGER, name TEXT);");

				for (StrangeItemLevel s : strangeItemLevels) {
					sqlExecList.add("INSERT INTO " + typeName + "(level, required_score, name) VALUES " +
							"(\"" + s.level + "\", \"" + s.required_score + "\", \"" + s.name + "\")");
				}
			}
		}
		
		reader.endArray();
	}
	
	private void parseStrangeScoreTypes(JsonReader reader) throws IOException {
		reader.beginArray();
		
		while (reader.hasNext()) {
			reader.beginObject();
			int type = -1;
			String typeName = null;
			String levelData = null;
			
			while (reader.hasNext()) {
				String name = reader.nextName();
				if (name.equals("type")) {
					type = reader.nextInt();
				} else if (name.equals("type_name")) {
					typeName = reader.nextString();
				} else if (name.equals("level_data")) {
					levelData = reader.nextString();
				} else {
					reader.skipValue();
				}
			}
			
			if (type != -1 && typeName != null && levelData != null)
				sqlExecList.add("INSERT INTO strange_score_types (type, type_name, level_data) VALUES" +
						"(\"" + type + "\", \"" + typeName + "\", \"" + levelData + "\")");
			
			reader.endObject();
		}
		
		reader.endArray();
	}
	
	private void linkItemAttributes() {
		for (TF2Weapon item : itemList) {
			ItemAttribute[] attributes = item.getAttributes();
			
			if (attributes != null) {
				for (ItemAttribute itemAttribute : attributes) {
					itemAttribute.setItemDefIndex(item.getDefIndex());
					
					for (Attribute attribute : attributeList) {
						if (attribute.getName().equals(itemAttribute.getName())) {
							itemAttribute.setAttributeDefIndex(attribute.getDefIndex());
							itemAttributeList.add(itemAttribute);
							break;
						}
					}
				}
			}
		}
	}	
	
	public void saveToDB(){
		new Thread(new Runnable() {
			public void run() {
				Log.i(Util.GetTag(), "Saving to database...");
				long start = System.currentTimeMillis();
				DataBaseHelper db = new DataBaseHelper(context);
				SQLiteDatabase sqlDb = db.getWritableDatabase();
				context = null;		
				
				sqlDb.beginTransaction();
				try {
					// removes everything from database, else we would get duplicates
					sqlDb.execSQL("DELETE FROM items");
					sqlDb.execSQL("DELETE FROM attributes");
					sqlDb.execSQL("DELETE FROM item_attributes");
					sqlDb.execSQL("DELETE FROM particles");
					sqlDb.execSQL("DELETE FROM strange_score_types");
					
					// we need to fetch table names
					final Cursor c = sqlDb.rawQuery("SELECT type_name FROM strange_item_levels", null);
					while (c.moveToNext()) {
						// delete each table for item levels
						sqlDb.execSQL("DROP TABLE " + c.getString(0));
					}
					c.close();
					
					sqlDb.execSQL("DELETE FROM strange_item_levels");
					
					// add all attribute definitions
					for (Attribute attribute : attributeList) {
						sqlDb.execSQL("INSERT INTO attributes " + attribute.getSqlInsert());
					}
					attributeList.clear();
					
					// add all item attributes
					for (ItemAttribute itemAttribute : itemAttributeList) {
						sqlDb.execSQL("INSERT INTO item_attributes " + itemAttribute.getSqlInsert());
					}
					itemAttributeList.clear();
					
					// add all items
					for (TF2Weapon item : itemList) {
						sqlDb.execSQL("INSERT INTO items " + item.getSqlInsert());
					}
					
					// run all the other sql statements
					if (sqlExecList.size() != 0){
						for (String sql : sqlExecList){
							sqlDb.execSQL(sql);
						}
					}
					
					sqlDb.setTransactionSuccessful();
				} finally {
					sqlDb.endTransaction();
				}
				
				sqlDb.close();
				db.close();
				Log.i(Util.GetTag(), "Save to database finished - Time: " + (System.currentTimeMillis() - start) + " ms");
			}
			
		}).start();
	}
}
