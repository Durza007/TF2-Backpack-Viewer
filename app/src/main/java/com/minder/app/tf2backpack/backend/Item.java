package com.minder.app.tf2backpack.backend;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;
import com.minder.app.tf2backpack.SteamUser;

public class Item implements Parcelable {
	public static class Equipped {
		@SerializedName("class") 
		public int character;
		public int slot;
	}
	
	public static class ItemAttribute implements Parcelable {
		private int defindex;
		private String value;
		private float float_value;
		private SteamUser account_info;
		
		public SteamUser getSteamUser() {
			return account_info;
		}
		
		public int getAttributeDefIndex() {
			return defindex;
		}

		public float getFloatValue() {
			return float_value;
		}

		public long getValue() {
			long value = 0;
			
			try {
				value = Long.parseLong(this.value);
			} catch (NumberFormatException e) {		
			}
			
			return value;
		}
		
		public String getStringValue() {
			return value;
		}

		public int describeContents() {
			return 0;
		}
		
		public ItemAttribute(Parcel source) {
			defindex = source.readInt();
			value = source.readString();
			float_value = source.readFloat();
			account_info = (SteamUser)source.readValue(SteamUser.class.getClassLoader());
		}

		public void writeToParcel(Parcel dest, int flags) {
			dest.writeInt(defindex);
			dest.writeString(value);
			dest.writeFloat(float_value);
			dest.writeValue(account_info);
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
	
	private int defindex;
	private String custom_name;
	private String custom_desc; 
	private int level;
	private int quality;
	private int quantity;
	private long inventory;
	private int backpackPosition;
	private int itemColor;
	private int itemColor2;
	private int particleEffect;
	private boolean flag_cannot_trade;
	private boolean flag_cannot_craft;
	private Equipped[] equipped;
	private ItemAttribute[] attributes;
	
	public void setDefIndex(int defIndex) {
		this.defindex = defIndex;
	}
	
	public int getDefIndex(){
		return defindex;
	}

	public ItemAttribute[] getAttributeList() {
		return attributes;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public int getLevel() {
		return level;
	}
	
	public void setQuality(int quality) {
		this.quality = quality;
	}

	public int getQuality() {
		return quality;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public int getQuantity() {
		return quantity;
	}
	
	public void setBackpackPosition(int backpackPosition) {
		this.backpackPosition = backpackPosition;
	}

	public int getBackpackPosition() {
		if (backpackPosition == -1)
			backpackPosition = extractBackpackPosition(inventory);
		
		return backpackPosition;
	}

	public int getColor() {
		if (itemColor == -1) {
			if (attributes != null) {
				for (ItemAttribute attribute : attributes) {
					if (attribute.defindex == 142) {
						itemColor = (int)attribute.getFloatValue();
						return itemColor;
					}
				}
			}
			itemColor = 0;
		}
		
		return itemColor;
	}
	
	public void setColor(int color){
		itemColor = color;
	}
	
	public int getColor2() {
		if (itemColor2 == -1) {
			if (attributes != null) {
				for (ItemAttribute attribute : attributes) {
					if (attribute.defindex == 261) {
						itemColor2 = (int)attribute.getFloatValue();
						return itemColor2;
					}
				}
			}
			itemColor2 = 0;
		}
		
		return itemColor2;
	}
	
	public boolean isEquipped() {
		return equipped != null;
	}
	
	public void setNotTradable(boolean notTradable) {
		this.flag_cannot_trade = notTradable;
	}

	public boolean isNotTradable() {
		return flag_cannot_trade;
	}
	
	public boolean isNotCraftable() {
		return flag_cannot_craft;
	}
	
	public void setNotCraftable(boolean notCraftable) {
		this.flag_cannot_craft = notCraftable;
	}

	public String getCustomDesc() {
		return custom_desc;
	}

	public void setCustomDesc(String customDesc) {
		this.custom_desc = customDesc;
	}

	public String getCustomName() {
		return custom_name;
	}
	
	public void setCustomName(String customName) {
		this.custom_name = customName;
	}
	
	public void setParticleEffect(int particleEffect) {
		this.particleEffect = particleEffect;
	}

	public int getParticleEffect() {
		if (particleEffect == -1) {
			if (attributes != null) {
				for (ItemAttribute attribute : attributes) {
					if (attribute.defindex == 134) {
						particleEffect = (int)attribute.getFloatValue();
						return particleEffect;
					}
				}
			}
			particleEffect = 0;
		}
		
		return particleEffect;
	}

	public Item() {
		this.itemColor = -1;
		this.itemColor2 = -1;
		this.particleEffect = -1;
		this.flag_cannot_trade = false;
		this.level = -1;
		this.backpackPosition = -1;
	}
	
	public Item(int defIndex, int level, int quality, int quantity, long inventoryToken) {
		this();
		this.setDefIndex(defIndex);
		this.setLevel(level);
		this.setQuality(quality);
		this.setQuantity(quantity);
		this.inventory = inventoryToken;
	}
	
	public Item(Parcel in){		
		this.setDefIndex(in.readInt());
		this.setCustomDesc(in.readString());
		this.setCustomName(in.readString());
		this.level = in.readInt();
		this.quality = in.readInt();
		this.itemColor = in.readInt();
		this.itemColor2 = in.readInt();
		this.particleEffect = in.readInt();
		boolean[] bools = new boolean[2];
		in.readBooleanArray(bools);
		this.flag_cannot_trade = bools[0];
		this.flag_cannot_craft = bools[1];
		
		Object[] array = in.readArray(ItemAttribute.class.getClassLoader());	
		if (array != null) {		
			this.attributes = new ItemAttribute[array.length];
			
			for (int index = 0; index < array.length; ++index)
				this.attributes[index] = (ItemAttribute)array[index];
		}
	}
	
	// find out if the item is equipped at the same time
	private static int extractBackpackPosition(long inventoryToken){
		// awarded but not yet given
		if (inventoryToken == 0 || (inventoryToken & 0x40000000) != 0) {
			return -1;
		}
		
		return (int)(inventoryToken & 0xFFFF) - 1;
	}

	// Parcelable functions
	public int describeContents() {
		return 0; 
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(getDefIndex());
		dest.writeString(getCustomDesc());
		dest.writeString(getCustomName());
		dest.writeInt(level);
		dest.writeInt(quality);
		dest.writeInt(itemColor);
		dest.writeInt(itemColor2);
		dest.writeInt(particleEffect);
		dest.writeBooleanArray(new boolean[] {flag_cannot_trade, flag_cannot_craft});
		dest.writeArray(attributes);
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