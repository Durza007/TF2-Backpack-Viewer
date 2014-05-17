package com.minder.app.tf2backpack.test;

import android.test.ActivityInstrumentationTestCase2;

import com.jayway.android.robotium.solo.Solo;
import com.minder.app.tf2backpack.Main;

public class TestBackpackMemoryLeak extends ActivityInstrumentationTestCase2<Main>   {
	private Solo solo;
	
	public TestBackpackMemoryLeak() {
		super("com.minder.app.tf2backpack", Main.class);
	}
	
	public void setUp() throws Exception {
		solo = new Solo(getInstrumentation(), getActivity());
	}
	
	public void testMemory() {
		solo.clickOnImageButton(1);
		assertTrue(solo.waitForDialogToClose(10000));
		for (int index = 0; index < 20; index++) {
			do {
				solo.clickInList(0);
			} while (!solo.waitForActivity("Backpack", 1000));

			assertTrue(solo.waitForDialogToClose(10000));
			solo.goBack();
		}
	}
	
	
	@Override
	public void tearDown() throws Exception {
		//Robotium will finish all the activities that have been opened
		solo.finishOpenedActivities();
	}
}
