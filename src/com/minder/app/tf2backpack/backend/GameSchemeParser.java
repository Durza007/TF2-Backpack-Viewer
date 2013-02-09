package com.minder.app.tf2backpack.backend;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.minder.app.tf2backpack.Attribute;
import com.minder.app.tf2backpack.BuildConfig;
import com.minder.app.tf2backpack.Util;
import com.minder.app.tf2backpack.Attribute.ItemAttribute;

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
	
	public class TF2Weapon {
		private int defIndex;
		private String itemSlot;
		private int quality;
		private String itemTypeName;
		private String itemName;
		private String imageUrl;
		private String itemDescription;
		
		public void setQuality(int quality){
			this.quality = quality;
		}
		
		public int getQuality(){
			return this.quality;
		}
		
		public void setItemSlot(String itemSlot) {
			this.itemSlot = itemSlot;
		}

		public String getItemSlot() {
			return itemSlot;
		}

		public void setDefIndex(int defIndex){
			this.defIndex = defIndex;
		}
		
		public int getDefIndex(){
			return this.defIndex;
		}
		
		public void setItemTypeName(String itemTypeName) {
			this.itemTypeName = itemTypeName;
		}
		
		public String getItemTypeName() {
			return this.itemTypeName;
		}
		
		public void setItemName(String itemName){
			this.itemName = itemName;
		}
		
		public String getItemName(){
			return this.itemName;
		}
		
		public void setImageUrl(String imageUrl) {
			this.imageUrl = imageUrl;
		}

		public String getImageUrl() {
			return imageUrl;
		}
		
		public void setItemDescription(String itemDescription) {
			this.itemDescription = itemDescription.replace("\"", "");
		}

		public String getItemDescription() {
			return itemDescription;
		}
		
		public TF2Weapon(){
			defIndex = 0;
			itemName = "";
			setImageUrl("");
		}
		
		public TF2Weapon(int defIndex, String itemTypeName, String itemName, String imageName){
			this.defIndex = defIndex;
			this.itemTypeName = itemTypeName;
			this.itemName = itemName;
			this.setImageUrl(imageName);
		}
		
		public String getSqlInsert(){
			return "(name, defindex, item_slot, quality, type_name, description, proper_name) VALUES " + 
				"(\"" + this.itemName + "\",\"" + this.defIndex + "\",\"" + this.itemSlot + "\",\"" + this.quality + "\",\"" + this.itemTypeName + "\",\"" + this.itemDescription + "\", \"0\")";
		}
	}
	
	public Exception error;
	
	//private JSONObject jObject;
	private ArrayList<TF2Weapon> itemList;
	private ArrayList<ImageInfo> imageURList;
	
	public ArrayList<String> sqlExecList;
	
	private Context mContext;
	
	public ArrayList<TF2Weapon> getItemList(){
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
	
	
	public GameSchemeParser(String data, Context context, boolean highresImages){		
		long start = System.currentTimeMillis();
		
		mContext = context;
		
		if (BuildConfig.DEBUG) {
			Log.d("GameSchemeParser", "message size: " + data.length());
		}
		
		try {
			JSONObject jObject = new JSONObject(data);
			data = null;
			JSONObject resultObject = jObject.getJSONObject("result");
			
			String status = resultObject.getString("status");
			
			sqlExecList = new ArrayList<String>();
			
			ArrayList<Attribute> attributeList = new ArrayList<Attribute>();
			{
				/**
				 * Fetch attributes first
				 */
				JSONArray attributeArray = resultObject.getJSONArray("attributes");
				
				for (int index = 0; index < attributeArray.length(); index++){
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
					//dbHandler.ExecSql("INSERT INTO attributes " + attribute.getSqlInsert());
					sqlExecList.add("INSERT INTO attributes " + attribute.getSqlInsert());
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
						//JSONObject attributesObject = itemObject.getJSONObject("attributes");
						JSONArray itemAttributeArray = itemObject.getJSONArray("attributes");
						
						for (int attribIndex = 0; attribIndex < itemAttributeArray.length(); attribIndex++){
							JSONObject attributeObject = itemAttributeArray.getJSONObject(attribIndex);
							
							JSONArray attributeNameArray = attributeObject.names();
							JSONArray attributeValArray = attributeObject.toJSONArray(attributeNameArray);
							ItemAttribute itemAttribute = new ItemAttribute();
							itemAttribute.setItemDefIndex(item.defIndex);
							
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
									//dbHandler.ExecSql("INSERT INTO item_attributes " + itemAttribute.getSqlInsert());
									sqlExecList.add("INSERT INTO item_attributes " + itemAttribute.getSqlInsert());
									break;
								}
							}
						}
					} catch (Exception e) {
					}
					
					//dbHandler.ExecSql("INSERT INTO items " + item.getSqlInsert());
					sqlExecList.add("INSERT INTO items " + item.getSqlInsert());
							
					imageURList.add(new ImageInfo(item.defIndex, itemColor, itemColor2, item.imageUrl));
				}
				// end fetch items
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
				
				
				sqlDb.beginTransaction();
				try {
					// removes everything from database, else we would get duplicates
					sqlDb.execSQL("DELETE FROM items");
					sqlDb.execSQL("DELETE FROM attributes");
					sqlDb.execSQL("DELETE FROM item_attributes");
					
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
				mContext = null;
				Log.i(Util.GetTag(), "Save to database finished - Time: " + (System.currentTimeMillis() - start) + " ms");
			}
			
		}).start();
	}
}
