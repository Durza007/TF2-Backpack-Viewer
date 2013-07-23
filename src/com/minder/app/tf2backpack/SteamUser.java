package com.minder.app.tf2backpack;

import com.google.gson.annotations.SerializedName;

import android.os.Parcel;
import android.os.Parcelable;

public class SteamUser implements Parcelable {
	@SerializedName("steamid") 
	public long steamdId64;
	@SerializedName("personaname") 
	public String steamName;
	public String communityId;
	public String gameId;
	public PersonaState personaState;
	public String avatarUrl;
	// left here in case it would ever be re-implemented
	public int wrenchNumber;
	
	public SteamUser(long id64, String name, PersonaState personaState){
		this();
		this.steamdId64 = id64;
		this.steamName = name;
		this.personaState = personaState;
		this.gameId = "";
	}

	public SteamUser() {
		this.personaState = PersonaState.Offline;
		this.gameId = "";
		this.communityId = "";
		this.avatarUrl = "";
	}

	public SteamUser(Parcel source) {
		this.steamdId64 = source.readLong();
		this.steamName = source.readString();
		this.communityId = source.readString();
		this.gameId = source.readString();
		this.personaState = PersonaState.values()[source.readInt()];
		this.avatarUrl = source.readString();
		this.wrenchNumber = source.readInt();
	}

	// Parcelable functions
	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(this.steamdId64);
		dest.writeString(this.steamName);
		dest.writeString(this.communityId);
		dest.writeString(this.gameId);
		dest.writeInt(this.personaState.ordinal());
		dest.writeString(this.avatarUrl);
		dest.writeInt(this.wrenchNumber);
	}
	
	public static final Parcelable.Creator<SteamUser> CREATOR = new Parcelable.Creator<SteamUser>() 
	{
		public SteamUser createFromParcel(Parcel source) {
			return new SteamUser(source);
		}

		public SteamUser[] newArray(int size) {
			return new SteamUser[size];
		}
		
	};
}
