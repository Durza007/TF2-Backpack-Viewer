package com.minder.app.tf2backpack;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;

public class Attribute {
	public final static byte EFFECT_POSITIVE = 1;
	public final static byte EFFECT_NEGATIVE = 2;
	public final static byte EFFECT_NEUTRAL = 3;
	
	public final static byte FORMAT_PERCENTAGE = 1;
	public final static byte FORMAT_INVERTED_PERCENTAGE = 2;
	public final static byte FORMAT_ADDITIVE = 3;
	public final static byte FORMAT_ADDITIVE_PERCENTAGE = 4;
	public final static byte FORMAT_DATE = 5;
	public final static byte FORMAT_PARTICLE_INDEX = 6;
	public final static byte FORMAT_ACCOUNT_ID = 7;
	public final static byte FORMAT_OR = 8;
	public final static byte FORMAT_ITEM_DEFINDEX = 9;
	
	private String name;
	private int defindex;
	private String description_string;
	private String description_format;
	private byte descriptionFormat = 0;
	private String effect_type;
	private byte effectType = 0;
	private boolean hidden;
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setDefIndex(int defIndex) {
		this.defindex = defIndex;
	}
	
	public int getDefIndex() {
		return defindex;
	}
	
	public void setDescriptionString(String descriptionString) {
		this.description_string = descriptionString;
	}
	
	public String getDescriptionString() {
		return description_string;
	}
	
	public void setDescriptionFormat(byte descriptionFormat) {
		this.descriptionFormat = descriptionFormat;
	}
	
	public void setDescriptionFormat(String descriptionFormat) {
		this.description_format = descriptionFormat;
		this.descriptionFormat = getDescriptionFormat(descriptionFormat);
	}
	
	public byte getDescriptionFormat() {
		return descriptionFormat;
	}
	
	public void setEffectType(byte effectType) {
		this.effectType = effectType;
	}
	
	public void setEffectType(String effectType) {
		this.effect_type = effectType;
		this.effectType = getEffectType(effectType);
	}
	
	public byte getEffectType() {
		return effectType;
	}
	
	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}
	
	public boolean isHidden() {
		return hidden;
	}
	
	public int isHiddenInt() {
		if (hidden) 
			return 1;
		else 
			return 0;
	}
	
	public ContentValues getSqlValues() {
		if (descriptionFormat == 0)
			descriptionFormat = getDescriptionFormat(description_format);
		
		if (effectType == 0)
			effectType = getEffectType(effect_type);
		
		final ContentValues values = new ContentValues(6);
		values.put("name", name);
		values.put("defindex", defindex);
		values.put("description_string", description_string);
		values.put("description_format", descriptionFormat);
		values.put("effect_type", effectType);
		values.put("hidden", isHiddenInt());
		
		return values;
	}
	
    public static byte getDescriptionFormat(String format){
    	if (format != null){
    		if (format.equals("value_is_percentage")) {
    			return FORMAT_PERCENTAGE;
    		} else if (format.equals("value_is_inverted_percentage")) {
    			return FORMAT_INVERTED_PERCENTAGE;
    		} else if (format.equals("value_is_additive")) {
    			return FORMAT_ADDITIVE;
    		} else if (format.equals("value_is_additive_percentage")) {
    			return FORMAT_ADDITIVE_PERCENTAGE;
    		} else if (format.equals("value_is_date")) {
    			return FORMAT_DATE;
    		} else if (format.equals("value_is_particle_index")) {
    			return FORMAT_PARTICLE_INDEX;
    		} else if (format.equals("value_is_account_id")) {
    			return FORMAT_ACCOUNT_ID;
    		} else if (format.equals("value_is_or")) {
    			return FORMAT_OR;
    		} else if (format.equals("value_is_item_def")) {
    			return FORMAT_ITEM_DEFINDEX;
    		}
    	}
    	return 0;
    }
	
    public static byte getEffectType(String effect){
    	if (effect != null){
    		if (effect.equals("positive")) {
    			return EFFECT_POSITIVE;
    		} else if (effect.equals("negative")){
    			return EFFECT_NEGATIVE;
    		} else if (effect.equals("neutral")){
    			return EFFECT_NEUTRAL;
    		}
    	}
    	
    	return 0;
    }
    
    /**
     * Attributes that are attached to an item
     */
	public static class ItemAttribute implements Parcelable {
		private int itemDefIndex;
		private int attributeDefIndex;
		private String name;
		private long longValue;
		private float value;
		private long accountSteamId;
		private String accountPersonaName;

		public void setItemDefIndex(int itemDefIndex) {
			this.itemDefIndex = itemDefIndex;
		}

		public int getItemDefIndex() {
			return itemDefIndex;
		}

		public void setAttributeDefIndex(int attributeDefIndex) {
			this.attributeDefIndex = attributeDefIndex;
		}

		public int getAttributeDefIndex() {
			return attributeDefIndex;
		} 
		
		public void setName(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		public void setValue(long value) {
			this.longValue = value;
		}
		
		public long getValue() {
			return longValue;
		}
		
		public void setAccountInfo(long steamId, String personaName) {
			this.accountSteamId = steamId;
			this.accountPersonaName = personaName;
		}
		
		public long getAccountSteamId() {
			return this.accountSteamId;
		}
		
		public String getAccountPersonaName() {
			return this.accountPersonaName;
		}
		
		public ContentValues getSqlValues() {
			final ContentValues values = new ContentValues(3);
			
			values.put("itemdefindex", itemDefIndex);
			values.put("attributedefindex", attributeDefIndex);
			values.put("value", value);
			
			return values;
		}
		
		public ItemAttribute(Parcel source) {
			this.attributeDefIndex = source.readInt();
			this.longValue = source.readLong();
			this.value = source.readFloat();
			this.accountSteamId = source.readLong();
			this.accountPersonaName = source.readString();
		}

		public ItemAttribute() {
		}

		//Parcelable functions
		public int describeContents() {
			return 0;
		}

		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(attributeDefIndex);
			dest.writeLong(longValue);
			dest.writeFloat(value);
			dest.writeLong(accountSteamId);
			dest.writeString(accountPersonaName);
		}
		
		public void setFloatValue(float floatValue) {
			this.value = floatValue;
		}

		public float getFloatValue() {
			return value;
		}

		public static final Parcelable.Creator<ItemAttribute> CREATOR = new Parcelable.Creator<ItemAttribute>() 
		{
			public ItemAttribute createFromParcel(Parcel source) {
				return new ItemAttribute(source);
			}

			public ItemAttribute[] newArray(int size) {
				return new ItemAttribute[size];
			}
			
		};
	}
	
	public static class StrangeQuality {
		public final static int MAX_NUM_STRANGE_PARTS = 6;
		public final static int[] STRANGE_SCORE_ARRAY = { 214, 294, 494, 379, 381, 383 };
		public final static int[] STRANGE_SCORE_TYPES_ARRAY = { 292, 293, 495, 380, 382, 384 };
		private boolean changed;
		private int value;
		private int strangeType;
		
		public int getValue() {
			return value;
		}
		
		public void setValue(int value) {
			changed = true;
			this.value = value;
		}
		
		public int getStrangeType() {
			return strangeType;
		}
		
		public void setStrangeType(int strangeType) {
			changed = true;
			this.strangeType = strangeType;
		}
		
		public boolean isChanged() {
			return this.changed;
		}
		
		public StrangeQuality() {
			changed = false;
			value = 0;
			strangeType = 0;
		}
	}
}
