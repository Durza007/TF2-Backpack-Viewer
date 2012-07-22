package com.minder.app.tf2backpack;

public enum PersonaState { // 1 - Online, 2 - Busy, 3 - Away, 4 - Snooze
	Offline(0),
	Online(1),
	Busy(2),
	Away(3),
	Snooze(4),
	LookingForTrade(5),
	LookingForPlay(6);

    public final int value;
    
    PersonaState(int value){
    	this.value = value;
    }
}
