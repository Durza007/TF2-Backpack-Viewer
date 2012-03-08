package com.minder.app.tf2backpack;

public class SteamUser {
	public long steamdId64;
	public String steamName;
	public String communityId;
	public String gameId;
	public PersonaState personaState;
	public String avatarUrl;
	// left here in case it would ever be re-implemented
	public int wrenchNumber;
	// TODO shoud be removed as soon as possible
	public boolean fetchingData;
	
	public SteamUser(long id64, String name, PersonaState personaState){
		this.steamdId64 = id64;
		this.steamName = name;
		this.personaState = personaState;
		this.gameId = "";
	}

	public SteamUser() {
		this.personaState = PersonaState.Offline;
		this.gameId = "";
	}
}
