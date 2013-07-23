package com.minder.app.tf2backpack.backend;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.minder.app.tf2backpack.Attribute;
import com.minder.app.tf2backpack.Attribute.ItemAttribute;
import com.minder.app.tf2backpack.BuildConfig;
import com.minder.app.tf2backpack.Util;

public class GameSchemeParser {
	public static class ImageInfo {
		private String link;
		private int defIndex;
		private int color;
		private int color2;
		
		public void setLink(String link) {
			this.link = link;
		}
		public String getLink() {
			return link;
		}
		public void setDefIndex(int defIndex) {
			this.defIndex = defIndex;
		}
		public int getDefIndex() {
			return defIndex;
		}
		
		public int getFormatedIndexColor() {
			if (color != 0){
				return 1073741824 | defIndex;
			}
			return defIndex;
		}
		
		public void setColor(int color) {
			this.color = color;
		}
		
		public int getColor() {
			return color;
		}
		
		public int getColor2() {
			return color2;
		}
		
		public ImageInfo(int defIndex, int color, int color2, String link){
			this.defIndex = defIndex;
			this.link = link;
			this.color = color;
			this.color2 = color2;
		}
	}
	
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
			this.item_description = itemDescription.replace("\"", "");
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
		
		public String getSqlInsert(){
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
	private ArrayList<ImageInfo> imageURList;
	
	public List<String> sqlExecList;
	private List<Attribute> attributeList;
	private List<ItemAttribute> itemAttributeList;
	
	private Context mContext;
	
	public List<TF2Weapon> getItemList(){
		return itemList;
	}
	
	public ArrayList<ImageInfo> getImageURList(){
		return imageURList;
	}
	
	/*public GameSchemeParser(String data, Context context, boolean test){
	DataBaseHelper db = new DataBaseHelper(context);
		SQLiteDatabase sqlDb = db.getWritableDatabase();
		
		// removes everything from database, else we would get duplicates
		sqlDb.execSQL("DELETE FROM items");
		
		long start = System.currentTimeMillis();
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		
		try {
			Items item = mapper.readValue(context.getAssets().open("items2.txt"), Items.class);
			
			ArrayList<Item> weaponList = item.get("item");
			
			imageURList = new ArrayList<ImageInfo>();
			for (Item i : weaponList){
				//sqlDb.execSQL("INSERT INTO items " + i.getSqlInsert());
				imageURList.add(new ImageInfo(i.getDefindex(), 0, i.getImage_url()));
			}
					
		} catch (JsonParseException e) {
			error = e;
			e.printStackTrace();
		} catch (JsonMappingException e) {
			error = e;
			e.printStackTrace();
		} catch (IOException e) {
			error = e;
			e.printStackTrace();
		}
		
		Log.d("TF2", "JAckson: " + (System.currentTimeMillis() - start) + " ms");
		
		/*try {
			JsonParser jp = jsonFactory.createJsonParser(context.getAssets().open("items2.txt"));
			
			imageURList = new ArrayList<ImageInfo>();
			
			while (jp.nextToken() != JsonToken.NOT_AVAILABLE) {
				if (jp.getCurrentToken() == JsonToken.START_ARRAY) {
					
					jp.nextToken();
					while (jp.getCurrentToken() != JsonToken.END_ARRAY){
						
						TF2Weapon item = new TF2Weapon();
						
						while (jp.nextToken() != JsonToken.END_OBJECT){
							if (jp.getCurrentToken() == JsonToken.FIELD_NAME){
								String type = jp.getCurrentName();
								
								while (jp.nextToken() == JsonToken.START_OBJECT){
									while (jp.nextToken() != JsonToken.END_OBJECT){
									}
								}
								
								String value = jp.getText();
								
								if (type != null){
									if (type.equals("defindex")){
										item.setDefIndex(jp.getIntValue());
									} else if (type.equals("item_type_name")){
										item.setItemTypeName(jp.getText());
									} else if (type.equals("item_name")){
										item.setItemName(jp.getText());
									} else if (type.equals("image_url")){
										item.setImageUrl(jp.getText());
									} else if (type.equals("item_description")){
										item.setItemDescription(jp.getText());
									}
								}
								
								Log.d("TF2", type + value);
							}
						}
						
						// Add item to list
						imageURList.add(new ImageInfo(item.defIndex, 0, item.imageUrl));
						sqlDb.execSQL("INSERT INTO items " + item.getSqlInsert());
						
						if (item.defIndex == 30){
							Log.d("TF2", "OMG");
						}
						
						jp.nextToken();
					}
				}
			}
		} catch (JsonParseException e) {
			// TODO Auto-generated catch block
			error = e;
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			error = e;
			e.printStackTrace();
		}*/
		//sqlDb.close();
	//}
	
	public GameSchemeParser(InputStream inputStream, Context context, boolean highresImages) throws IOException {
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
	
	public GameSchemeParser(String data, Context context, boolean highresImages){		
		long start = System.currentTimeMillis();
		
		mContext = context;
		
		if (BuildConfig.DEBUG) {
			Log.d("GameSchemeParser", "message size: " + data.length());
		}
		
		// init lists
		sqlExecList = new LinkedList<String>();
		itemList = new LinkedList<TF2Weapon>();
		attributeList = new LinkedList<Attribute>();
		itemAttributeList = new LinkedList<ItemAttribute>();
		
		try {
			JSONObject jObject = new JSONObject(data);
			// set to null as soon as possible because it holds >2MB data
			data = null;
			JSONObject resultObject = jObject.getJSONObject("result");
			
			String status = resultObject.getString("status");
			
			{
				/**
				 * Fetch attributes first
				 */
				JSONArray attributeArray = resultObject.getJSONArray("attributes");
				
				for (int index = 0; index < attributeArray.length(); index++) {
					JSONObject attributeObject = attributeArray.getJSONObject(index);
					
					// Magick =)
					JSONArray nameArray = attributeObject.names();
					JSONArray valArray = attributeObject.toJSONArray(nameArray);
					Attribute attribute = new Attribute();
					
					for (int arrayIndex = 0; arrayIndex < nameArray.length(); arrayIndex++){
						String type = nameArray.getString(arrayIndex);
						if (type != null){
							if (type.equals("name")){
								attribute.setName(valArray.getString(arrayIndex));
							} else if (type.equals("defindex")){
								attribute.setDefIndex(valArray.getInt(arrayIndex));
							} else if (type.equals("description_string")){
								attribute.setDescriptionString(valArray.getString(arrayIndex));
							} else if (type.equals("description_format")){
								attribute.setDescriptionFormat(valArray.getString(arrayIndex));
							} else if (type.equals("effect_type")){
								attribute.setEffectType(valArray.getString(arrayIndex));
							} else if (type.equals("hidden")){
								attribute.setHidden(valArray.getBoolean(arrayIndex));
							}
						}
					}
					
					attributeList.add(attribute);
				}
			}
			
			{
				/**
				 * Fetch items
				 */
				//itemList = new ArrayList<TF2Weapon>();
				//JSONObject itemsObject = resultObject.getJSONObject("items");
				JSONArray itemArray = resultObject.getJSONArray("items");			
				imageURList = new ArrayList<ImageInfo>();
				
				for (int index = 0; index < itemArray.length(); index++){
					JSONObject itemObject = itemArray.getJSONObject(index);
					
					// Magic =)
					JSONArray nameArray = itemObject.names();
					JSONArray valArray = itemObject.toJSONArray(nameArray);
					TF2Weapon item = new TF2Weapon();
					
					for (int arrayIndex = 0; arrayIndex < nameArray.length(); arrayIndex++){
						String type = nameArray.getString(arrayIndex);
						if (type != null){
							if (type.equals("defindex")){
								item.setDefIndex(valArray.getInt(arrayIndex));
							} else if (type.equals("item_type_name")){
								item.setItemTypeName(valArray.getString(arrayIndex));
							} else if (type.equals("item_name")){
								item.setItemName(valArray.getString(arrayIndex));
							} else if (!highresImages && (type.equals("image_url")) || (highresImages && type.equals("image_url_large"))){
								item.setImageUrl(valArray.getString(arrayIndex));
							} else if (type.equals("item_description")){
								item.setItemDescription(valArray.getString(arrayIndex));
							} else if (type.equals("item_quality")){
								item.setQuality(valArray.getInt(arrayIndex));
							} else if (type.equals("item_slot")){
								item.setItemSlot(valArray.getString(arrayIndex));
							}
						}
					}
					
					// temporary store paintcan color
					int itemColor = 0;
					// used for team paint
					int itemColor2 = 0;
					try {
						JSONArray itemAttributeArray = itemObject.getJSONArray("attributes");
						
						for (int attribIndex = 0; attribIndex < itemAttributeArray.length(); attribIndex++){
							JSONObject attributeObject = itemAttributeArray.getJSONObject(attribIndex);
							
							JSONArray attributeNameArray = attributeObject.names();
							JSONArray attributeValArray = attributeObject.toJSONArray(attributeNameArray);
							ItemAttribute itemAttribute = new ItemAttribute();
							itemAttribute.setItemDefIndex(item.getDefIndex());
							
							for (int arrayIndex = 0; arrayIndex < attributeNameArray.length(); arrayIndex++){
								String type = attributeNameArray.getString(arrayIndex);
								if (type != null){
									if (type.equals("name")){
										itemAttribute.setName(attributeValArray.getString(arrayIndex));
									} else if (type.equals("value")){
										itemAttribute.setFloatValue((float) attributeValArray.getDouble(arrayIndex));
									}
								}
							}
							
							if (itemAttribute.getName().equals("set item tint RGB")){
								itemColor = (int) itemAttribute.getFloatValue();
								if (itemColor == 1) itemColor = 0;
							}
							
							// Temporary fix for team spirit cans
							if (itemAttribute.getName().equals("set item tint RGB 2")) {
								itemColor2 = (int) itemAttribute.getFloatValue();
							}
							
							for (Attribute a : attributeList) {
								if (a.getName().equals(itemAttribute.getName())) {
									itemAttribute.setAttributeDefIndex(a.getDefIndex());
									itemAttributeList.add(itemAttribute);
									break;
								}
							}
						}
					} catch (Exception e) {
					}
					
					itemList.add(item);						
					imageURList.add(new ImageInfo(item.getDefIndex(), itemColor, itemColor2, item.image_url));
				} // end fetch items
				
				{
					/*
					 * Fetch particles
					 */
					JSONArray particleArray = resultObject.getJSONArray("attribute_controlled_attached_particles");			
					
					for (int index = 0; index < particleArray.length(); index++){
						JSONObject particleObject = particleArray.getJSONObject(index);
						int id = particleObject.getInt("id");
						String  name = particleObject.getString("name");
						
						sqlExecList.add("INSERT INTO particles (id, name) VALUES (\"" + id + "\", \"" + name + "\")"); 
					}
				}
				
				{ // Strange quality ranks
					{
						/*
						 * Strange item ranks
						 */
						JSONArray strangeTypeLevels = resultObject.getJSONArray("item_levels");	
						List<String> strangeTypes = new LinkedList<String>();
						
						// Strange level lists
						for (int index = 0; index < strangeTypeLevels.length(); index++) {
							JSONObject strangeTypeLevel = strangeTypeLevels.getJSONObject(index);
							
							String typeName = strangeTypeLevel.getString("name");
							strangeTypes.add(typeName);
							
							sqlExecList.add("INSERT INTO strange_item_levels (type_name) VALUES (\"" + typeName + "\")");
							sqlExecList.add("CREATE TABLE " + typeName + " (level INTEGER PRIMARY KEY, required_score INTEGER, name TEXT);");
							
							JSONArray levels = strangeTypeLevel.getJSONArray("levels");
							for (int levelIndex = 0; levelIndex < levels.length(); levelIndex++) {
								JSONObject levelObject = levels.getJSONObject(levelIndex);
								
								int level = levelObject.getInt("level");
								int requiredScore = levelObject.getInt("required_score");
								String name = levelObject.getString("name");
								
								sqlExecList.add("INSERT INTO " + typeName + "(level, required_score, name) VALUES " +
										"(\"" + level + "\", \"" + requiredScore + "\", \"" + name + "\")");
							}
						}
					}
					
					{
						/*
						 * Strange types
						 */
						JSONArray strangeScoreTypes = resultObject.getJSONArray("kill_eater_score_types");
						
						// build a list of the different table names
						for (int index = 0; index < strangeScoreTypes.length(); index++) {
							JSONObject scoreType = strangeScoreTypes.getJSONObject(index);
							
							int type = scoreType.getInt("type");
							String typeName = scoreType.getString("type_name");
							String levelData = scoreType.getString("level_data");
							
							sqlExecList.add("INSERT INTO strange_score_types (type, type_name, level_data) VALUES" +
									"(\"" + type + "\", \"" + typeName + "\", \"" + levelData + "\")");
						}
					}
				}
			}
		} catch (Exception e) {
			error = e;
			e.printStackTrace();
		}
		
		Log.i(Util.GetTag(), "Json: " + (System.currentTimeMillis() - start) + " ms");
		
		if (sqlExecList != null){
			saveToDB();
		}
	}	
	
	public void saveToDB(){
		new Thread(new Runnable() {
			public void run() {
				Log.i(Util.GetTag(), "Saving to database...");
				long start = System.currentTimeMillis();
				DataBaseHelper db = new DataBaseHelper(mContext);
				SQLiteDatabase sqlDb = db.getWritableDatabase();
				mContext = null;		
				
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
