package com.minder.app.tf2backpack;

public class Item {
	private int defindex;
	private String item_type_name;
	private String item_name;
	private String image_url;
	private String item_description;
	
	public void setDefindex(int defindex) {
		this.defindex = defindex;
	}
	public int getDefindex() {
		return defindex;
	}
	public void setItem_type_name(String item_type_name) {
		this.item_type_name = item_type_name;
	}
	public String getItem_type_name() {
		return item_type_name;
	}
	public void setItem_name(String item_name) {
		this.item_name = item_name;
	}
	public String getItem_name() {
		return item_name;
	}
	public void setImage_url(String image_url) {
		this.image_url = image_url;
	}
	public String getImage_url() {
		return image_url;
	}
	public void setItem_description(String item_description) {
		this.item_description = item_description;
	}
	public String getItem_description() {
		return item_description;
	}
	
	public String getSqlInsert(){	
		return "(name, defindex, type_name, description, proper_name) VALUES " + 
			"(\"" + this.item_name + "\",\"" + this.defindex + "\",\"" + this.item_type_name + "\",\"" + this.item_description + "\", \"0\")";
	}
}
