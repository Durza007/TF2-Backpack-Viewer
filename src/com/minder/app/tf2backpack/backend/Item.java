package com.minder.app.tf2backpack.backend;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.minder.app.tf2backpack.Attribute.ItemAttribute;

public class Item implements Parcelable {
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
	private boolean notCraftable;
	private ArrayList<ItemAttribute> attributeList;
	
	public void setDefIndex(int defIndex) {
		this.defIndex = defIndex;
	}
	
	public int getDefIndex(){
		return defIndex;
	}

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
	
	public boolean isNotCraftable() {
		return notCraftable;
	}
	
	public void setNotCraftable(boolean notCraftable) {
		this.notCraftable = notCraftable;
	}

	public String getCustomDesc() {
		return customDesc;
	}

	public void setCustomDesc(String customDesc) {
		this.customDesc = customDesc;
	}

	public String getCustomName() {
		return customName;
	}
	
	public void setCustomName(String customName) {
		this.customName = customName;
	}

	public Item() {
		this.itemColor = 0;
		this.notTradable = false;
		this.level = -1;
		attributeList = new ArrayList<ItemAttribute>();
	}
	
	public Item(int defIndex, int level, int quality, int quantity, long inventoryToken){
		this.setDefIndex(defIndex);
		this.setLevel(level);
		this.setQuality(quality);
		this.setQuantity(quantity);
		this.inventoryToken = inventoryToken;
		this.setBackpackPosition(ExtractBackpackPosition(inventoryToken));
		this.itemColor = 0;
		this.notTradable = false;
		this.attributeList = new ArrayList<ItemAttribute>();
	}
	
	public Item(Parcel in){		
		this.setDefIndex(in.readInt());
		this.setCustomDesc(in.readString());
		this.setCustomName(in.readString());
		this.level = in.readInt();
		this.quality = in.readInt();
		this.itemColor = in.readInt();
		this.particleEffect = in.readInt();
		boolean[] bools = new boolean[3];
		in.readBooleanArray(bools);
		this.equipped = bools[0];
		this.notTradable = bools[1];
		this.setNotCraftable(bools[2]);
		this.attributeList = new ArrayList<ItemAttribute>();
		in.readTypedList(this.attributeList, ItemAttribute.CREATOR);
	}
	
	// find out if the item is equipped at the same time
	public int ExtractBackpackPosition(long inventoryToken){
		// awarded but not yet given
		if (inventoryToken == 0 || (inventoryToken & 0x40000000) != 0) {
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
		dest.writeInt(getDefIndex());
		dest.writeString(getCustomDesc());
		dest.writeString(getCustomName());
		dest.writeInt(level);
		dest.writeInt(quality);
		dest.writeInt(itemColor);
		dest.writeInt(particleEffect);
		dest.writeBooleanArray(new boolean[] {equipped, notTradable, isNotCraftable()});
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