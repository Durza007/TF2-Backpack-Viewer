package com.minder.app.tf2backpack.frontend;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.minder.app.tf2backpack.Attribute;
import com.minder.app.tf2backpack.Attribute.ItemAttribute;
import com.minder.app.tf2backpack.Attribute.StrangeQuality;
import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.Util;
import com.minder.app.tf2backpack.backend.DataBaseHelper;
import com.minder.app.tf2backpack.backend.Item;

public class WeaponInfo extends Activity {
	private static final int blueColor = Color.rgb(153, 204, 255);
	private static final int redColor = Color.rgb(255, 64, 64);
	private static final int whiteColor = Color.rgb(236, 227, 203);

	private TextView tvAttributes;
	private SpannableStringBuilder attributeText;
	private int startIndex, endIndex;
	private boolean hideLargeCraftOrder;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // no titlebar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.weapon_info);
        
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        hideLargeCraftOrder = sp.getBoolean("hidelargecraftnumber", false);
        
        Item item = getIntent().getParcelableExtra("com.minder.app.tf2backpack.PlayerItemParser.Item");
        
        DataBaseHelper db = new DataBaseHelper(this.getApplicationContext());
        SQLiteDatabase sqlDb = db.getReadableDatabase();
        
        Cursor c = sqlDb.rawQuery("SELECT name, type_name, description FROM items WHERE defindex = "
        		+ item.getDefIndex(), null);
        
        ArrayList<ItemAttribute> itemAttributeList = item.getAttributeList();
        String sqlParam = null;
        for (ItemAttribute ia : itemAttributeList){
        	if (sqlParam == null){
        		sqlParam = "";
        		sqlParam += "WHERE defindex = " + ia.getAttributeDefIndex();
        	} else {
        		sqlParam += " OR defindex = " + ia.getAttributeDefIndex();
        	}
        }
        
        Cursor cAttribute = null;
        if (sqlParam != null){
        	cAttribute = sqlDb.rawQuery("SELECT  description_string, value, description_format, effect_type, hidden, defindex FROM item_attributes, attributes WHERE itemdefindex = " + item.getDefIndex() + " AND defindex = attributedefindex UNION SELECT description_string, defindex, description_format, effect_type, hidden, defindex FROM attributes " + sqlParam, null);
        } else {
        	cAttribute = sqlDb.rawQuery("SELECT  description_string, value, description_format, effect_type, hidden, defindex FROM item_attributes, attributes WHERE itemdefindex = " + item.getDefIndex() + " AND defindex = attributedefindex", null);
        }
        
        TextView tvName = (TextView)findViewById(R.id.TextViewWeaponName);
        TextView tvLevel = (TextView)findViewById(R.id.TextViewWeaponLevel);
        TextView tvDescription = (TextView)findViewById(R.id.TextViewDescription);
        TextView tvTradable = (TextView)findViewById(R.id.TextViewTradable);
        tvAttributes = (TextView)findViewById(R.id.TextViewAttributes);
        
        if (c != null) {
        	if (c.moveToFirst()) {
		        String namePrefix = "";
		        if (item.getQuality() == 1){
		        	namePrefix = "Genuine ";
		        } else if (item.getQuality() == 3){
		        	namePrefix = "Vintage ";
		        } else if (item.getQuality() == 5){
		        	namePrefix = "Unusual ";
		        } else if (item.getQuality() == 7){
		        	namePrefix = "Community ";
		        } else if (item.getQuality() == 8){
		        	namePrefix = "Valve ";
		        } else if (item.getQuality() == 9){
		        	namePrefix = "Self-Made ";
		        } else if (item.getQuality() == 11){
	    			// get kills if the weapon is a strange weapon
		        	int kills = 0;
	    	        for (ItemAttribute ia : itemAttributeList){
	    	        	if (ia.getAttributeDefIndex() == 214){
	    	        		kills = (int) ia.getValue();
	    	        	}
	    	        }
	    	        
		        	namePrefix = getStrangeRank(kills) + " ";
		        } else if (item.getQuality() == 13){
		        	namePrefix = "Haunted ";
		        }
		     
		        if (item.getCustomName() != null){
		        	tvName.setText("\"" + item.getCustomName() + "\"");
		        } else {
		        	tvName.setText(namePrefix + c.getString(c.getColumnIndex("name")));
		        }
		        
		        String weaponClass = c.getString(c.getColumnIndex("type_name"));
		        if (weaponClass.contains("TF_")){
		        	weaponClass = "";
		        }
		        
		        if (item.getLevel() != -1) {
		        	tvLevel.setText("Level " + item.getLevel() + " " + weaponClass);
		        } else {
		        	tvLevel.setVisibility(View.GONE);
		        }
		        
		        if (item.getCustomDesc() != null){
		        	tvDescription.setText("\"" + item.getCustomDesc() + "\"");
		        } else {
			        String descr = c.getString(c.getColumnIndex("description"));
			        
			        if (!descr.equals("null")){
			        	tvDescription.setText(descr);
			        } else {
			        	tvDescription.setVisibility(View.GONE);
			        }
		        }
		        
		        if (item.getParticleEffect() != 0){
		        	tvAttributes.setText("Effect: " + getParticleName(sqlDb, item.getParticleEffect()));
		        	tvAttributes.setVisibility(View.VISIBLE);
		        }
		        
		        if (item.isNotTradable() || item.isNotCraftable()){
		        	tvTradable.setVisibility(View.VISIBLE);
		        	
		        	if (item.isNotTradable() && !item.isNotCraftable()) {
		        		tvTradable.setText(R.string.not_tradable);
		        	} else if (!item.isNotTradable() && item.isNotCraftable()) {
		        		tvTradable.setText(R.string.not_craftable);
		        	}
		        }
		        
		        tvName.setTextColor(Util.getItemColor(item.getQuality()));
		        
		        /**
		         * Attributes
		         */
    			boolean skip = false;
		        boolean crateAttrib = false;
		        boolean hireAttrib = false;
		        
		        StrangeQuality[] strangeQualities = new StrangeQuality[StrangeQuality.MAX_NUM_STRANGE_PARTS];
		        for (int i = 0; i < strangeQualities.length; i++) {
		        	strangeQualities[i] = new StrangeQuality();
				}
		        
		        if (cAttribute != null) {
		        	attributeText = new SpannableStringBuilder();
		        	int textIndex = 0;
		        	while (cAttribute.moveToNext()) {
		        		int attributeDefIndex = cAttribute.getInt(5);
		        		
		        		// check if hidden == false
		        		if (cAttribute.getInt(4) == 0) {
		        			String description = cAttribute.getString(0);
		        			int descriptionFormat = cAttribute.getInt(2);
		        			int effectType = cAttribute.getInt(3);
		        			double value = cAttribute.getDouble(1);
		        			
		        			long steamId;
		        			String personaName = null;
		        			
		        			skip = false;
		        			
		        			// set correct value for unique attributes
		        	        for (ItemAttribute ia : itemAttributeList) {
		        	        	if (ia.getAttributeDefIndex() == cAttribute.getInt(5)) {
		        	        		steamId = ia.getAccountSteamId();
		        	        		personaName = ia.getAccountPersonaName();
		        	        		
		        	        		// hide duplicate attributes
		        	        		if (ia.getAttributeDefIndex() == 187) {
		        	        			if (crateAttrib) {
			        	        			skip = true;
			        	        			continue;
		        	        			} else {
		        	        				crateAttrib = true;
		        	        			}
		        	        		}
		        	        		
		        	        		if (ia.getAttributeDefIndex() == 143) {
		        	        			if (hireAttrib){
			        	        			skip = true;
			        	        			continue;
		        	        			} else {
		        	        				hireAttrib = true;
		        	        			}
		        	        		}      	        		
		        	        		
		        	        		if (descriptionFormat == Attribute.FORMAT_DATE){
		        	        			value = ia.getValue();
		        	        		} else if (ia.getFloatValue() == 0){
		        	        			value = ia.getValue();
		        	        		} else {
		        	        			value = ia.getFloatValue();
		        	        		}
		        	        		
		        	        		break;
		        	        	}
		        	        }
		        	        
		        	        if (skip) continue;
		        			
		        			switch(descriptionFormat) {
		        			case Attribute.FORMAT_PERCENTAGE:
			        			if (value < 1){
			        				value = 1 - value;
			        				description = description.replace("%s1", "-" + String.valueOf((int)Math.round(value * 100))) + "\n";
			        			} else {
			        				value -= 1;
			        				description = description.replace("%s1", String.valueOf((int)Math.round(value * 100))) + "\n";
			        			}
		        				break;
		        				
		        			case Attribute.FORMAT_INVERTED_PERCENTAGE:
			        			value = 1 - value;
			        			description = description.replace("%s1", String.valueOf((int)Math.round(value * 100))) + "\n";
		        				break;
		        				
		        			case Attribute.FORMAT_ADDITIVE:
		        				if (value != (int)value){
		        					description = description.replace("%s1", String.valueOf(value)) + "\n";
		        				} else {
		        					description = description.replace("%s1", String.valueOf((int)value)) + "\n";
		        				}
		        				break;
		        				
		        			case Attribute.FORMAT_ADDITIVE_PERCENTAGE:
		        				description = description.replace("%s1", String.valueOf((int)Math.round(value * 100))) + "\n";
		        				break;
		        				
		        			case Attribute.FORMAT_DATE:
		        				Date date = getDateFromUnix(((long)value) * 1000);
		        				description = description.replace("%s1", date.toGMTString()) + "\n";
		        				break;
		        				
		        			case Attribute.FORMAT_PARTICLE_INDEX:   					
		        				description = description.replace("%s1", getParticleName(sqlDb, (int)value)) + "\n";
		        				break;
		        				
		        			case Attribute.FORMAT_ACCOUNT_ID:
		        				description = description.replace("%s1", personaName) + "\n";
		        				startIndex = description.indexOf(personaName);
		        				endIndex = personaName.length();
		        				startIndex += textIndex;
		        				break;
		        				
		        			default:
		        				description = description.replace("%s1", String.valueOf((int)value)) + "\n";
		        				break;
		        			}
		        			
		        			
		        			attributeText.append(description);
		        			if (effectType == Attribute.EFFECT_POSITIVE){
		        				attributeText.setSpan(new ForegroundColorSpan(blueColor), textIndex, textIndex + description.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        			} else if (effectType == Attribute.EFFECT_NEGATIVE) {
		        				attributeText.setSpan(new ForegroundColorSpan(redColor), textIndex, textIndex + description.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        			} else {
		        				attributeText.setSpan(new ForegroundColorSpan(whiteColor), textIndex, textIndex + description.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		        			}
		        			
		        			textIndex += description.length();
		        		} else if (attributeDefIndex == 214 || attributeDefIndex == 294 || attributeDefIndex == 494) {
		        			/**
		        			 * Strange weapon kills
		        			 */
		        			int index;
		        			for (index = 0; index < StrangeQuality.STRANGE_SCORE_ARRAY.length; index++) {
		        				if (attributeDefIndex == StrangeQuality.STRANGE_SCORE_ARRAY[index])
		        					break;
		        			}
		        			
		        			String description = (String) tvName.getText();
		        			double value = cAttribute.getDouble(1);
		        			
		        			// set correct value for unique attributes
		        	        for (ItemAttribute ia : itemAttributeList){
		        	        	if (ia.getAttributeDefIndex() == attributeDefIndex) {	
		        	        		if (ia.getFloatValue() == 0){
		        	        			value = ia.getValue();
		        	        		} else {
		        	        			value = ia.getFloatValue();
		        	        		}
		        	        		break;
		        	        	}
		        	        }
		        	        
		        	        strangeQualities[index].setValue((int)value);
		        			
		        			description += " - Kills: " + (int)value + "\n";
		        			
		        			tvLevel.setText(description);
		        			tvLevel.setVisibility(View.VISIBLE);
		        		} else if (attributeDefIndex == 292 || attributeDefIndex == 293 || attributeDefIndex == 495 || 
		        				attributeDefIndex == 380 || attributeDefIndex == 382 || attributeDefIndex == 384) {
		        			/*
		        			 * Strange score type
		        			 */
		        			int index;
		        			for (index = 0; index < StrangeQuality.STRANGE_SCORE_TYPES_ARRAY.length; index++) {
		        				if (attributeDefIndex == StrangeQuality.STRANGE_SCORE_TYPES_ARRAY[index])
		        					break;
		        			}
		        			
		        			double value = cAttribute.getDouble(1);
		    	
		        			// set correct value for unique attributes
		        	        for (ItemAttribute ia : itemAttributeList){
		        	        	if (ia.getAttributeDefIndex() == attributeDefIndex) {	
		        	        		if (ia.getFloatValue() == 0){
		        	        			value = ia.getValue();
		        	        		} else {
		        	        			value = ia.getFloatValue();
		        	        		}
		        	        		break;
		        	        	}
		        	        }
		        	        
		        	        strangeQualities[index].setStrangeType((int)value);        
		        		} else if (attributeDefIndex == 229) {
		        			/**
		        			 * Craft order attribute
		        			 */
		        			String name = (String) tvName.getText();
		        			double value = cAttribute.getDouble(1);
		        			
		        			// set correct value for unique attributes
		        	        for (ItemAttribute ia : itemAttributeList){
		        	        	if (ia.getAttributeDefIndex() == cAttribute.getInt(5)){    
		        	        		
		        	        		if (ia.getFloatValue() == 0){
		        	        			value = ia.getValue();
		        	        		} else {
		        	        			value = ia.getFloatValue();
		        	        		}
		        	        		break;
		        	        	}
		        	        }
		        	        
		        	        if (value <= 100 || !hideLargeCraftOrder) {      			
			        			name += " #" + (int)value;
			        			tvName.setText(name);
		        	        }
		        		} else if (attributeDefIndex == 228) {
		        			/**
		        			 * Makers mark id - the attribute that shows who crafted the item
		        			 */
		        			
		        			String personaName = null;
		        			// set correct value for unique attributes
		        	        for (ItemAttribute ia : itemAttributeList){
		        	        	if (ia.getAttributeDefIndex() == cAttribute.getInt(5)){    
		        	        		personaName = ia.getAccountPersonaName();
		        	        		break;
		        	        	}
		        	        }
		        	        
		        	        String description = cAttribute.getString(0);
	        				description = description.replace("%s1", personaName) + "\n";
	        				startIndex = description.indexOf(String.valueOf(personaName));
	        				endIndex = String.valueOf(personaName).length();
	        				startIndex += textIndex;
	        				
		        			attributeText.append(description);
		        			attributeText.setSpan(new ForegroundColorSpan(blueColor), textIndex, textIndex + description.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	        			
		        			textIndex += description.length();
	        				
		        			/*if (name.length() == 0) {
			        			final DownloadFilesTask task = new DownloadFilesTask();
			        			task.setStringToReplace(String.valueOf(value));
		        				task.execute(value);
		        			}*/
		        		}
		        	}
		        	
		        	if (textIndex != 0) { 
			        	tvAttributes.setText(attributeText.subSequence(0, attributeText.length() - 1));
	        			tvAttributes.setVisibility(View.VISIBLE);
		        	}
		        }
		        
		        // handle strange qualities
		        StringBuilder strangeTextBuilder = new StringBuilder();
		        boolean namePrefixSet = false;
		        
		        for (int index = 0; index < strangeQualities.length; index++) {
					if (strangeQualities[index].isChanged()) {
						final StrangeQuality sq = strangeQualities[index];
						Cursor strangeType = 
								sqlDb.rawQuery("SELECT type_name, level_data FROM strange_score_types WHERE type=" + sq.getStrangeType(), null);
						
						if (strangeType.moveToFirst()) {
							Cursor strangeLevel = 
									sqlDb.rawQuery("SELECT name FROM KillEaterRank WHERE required_score>" + sq.getValue() + " LIMIT 1", null);
							
							if (strangeLevel.moveToFirst()) {
								if (namePrefixSet) {
									strangeTextBuilder
									.append('(')
									.append(strangeType.getString(0))
									.append(": ")
									.append(sq.getValue())
									.append(")\n");
								} else {
									strangeTextBuilder
										.append(strangeLevel.getString(0))
										.append(' ')
										.append(weaponClass)
										.append(" - ")
										.append(strangeType.getString(0))
										.append(": ")
										.append(sq.getValue())
										.append('\n');
									
									namePrefixSet = true;
								}
							} else {
								// TODO this should NOT happen!!!
							}
						} else {
							// TODO handle missing strange type
						}
					}
				}
		        
		        // do we have any stuff to add?
		        if (strangeTextBuilder.length() != 0) {
		        	tvLevel.setText(strangeTextBuilder.toString());
		        	tvLevel.setVisibility(View.VISIBLE);
		        }
        	}
        } else {
        	tvName.setText("UNKNOWN ITEM: " + item.getDefIndex());
        }
        
        cAttribute.close();
        c.close();
        sqlDb.close();
    }
    
    private String getParticleName(SQLiteDatabase sqlDb, int index) {
		Cursor particleEffect = sqlDb.rawQuery("SELECT name FROM particles WHERE id=" + index, null);
		
		String name;
		if (particleEffect.moveToFirst()) {	        					
			name =  particleEffect.getString(0);
		} else {
			name =  "UNKOWN, update tf2 files!";
		}
		
		particleEffect.close();
		return name;
    }
    
    private String getStrangeRank(int kills) {
    	String[] strangeRank = new String[] {
    			"Strange",
        		"Unremarkable",
        		"Scarcely Lethal",
        		"Mildly Menacing",
        		"Somewhat Threatening",
        		"Uncharitable",
        		"Notably Dangerous",
        		"Sufficiently Lethal",
        		"Truly Feared",
        		"Spectacularly Lethal",
        		"Gore-Spattered",
        		"Wicked Nasty",
        		"Positively Inhumane",
        		"Totally Ordinary",
        		"Face-Melting",
        		"Rage-Inducing",
        		"Server-Clearing",
        		"Epic",
        		"Legendary",
        		"Australian",
        		"Hale's Own",
        	};
    	
    	final int[] rankKills = new int[] {
    			0, 10, 25, 45, 70, 
				100, 135, 175, 225, 275, 
				350, 500, 750, 999, 1000, 
				1500, 2500, 5000, 7500, 7616, 
				8500
    	};
    	int index = 0;
    	
    	for (int i = 0; i < rankKills.length; i++) {
    		if (kills >= rankKills[i]) {
    			index = i;
    		}
    	}
    	
    	return strangeRank[index];
    }
    
    private Date getDateFromUnix(long timestamp) {
    	final Calendar cal = Calendar.getInstance();
    	cal.setTimeInMillis(timestamp);
    	return cal.getTime();
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event){
    	if (event.getAction() == MotionEvent.ACTION_UP){
    		finish();
    	}
    	
		return true;   	
    }
    
    private class DownloadFilesTask extends AsyncTask<Long, Void, String> {
    	private String stringToReplace;
    	
    	public void setStringToReplace(String string) {
    		this.stringToReplace = string;
    		Log.d("WeaponInfo", string);
    	}
    	
		@Override
		protected String doInBackground(Long... params) {		
			// TODO Auto-generated method stub
	        XmlPullParserFactory pullMaker;
	        try {
	        	URL url = new URL("http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=***REMOVED***&steamids=" + params[0] + "&format=xml");
	        	//String xml = (String) new HttpConnection().getDirect("http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=***REMOVED***&steamids=" + params[0] + "&format=xml", 86400);
	        	
	            pullMaker = XmlPullParserFactory.newInstance();

	            XmlPullParser parser = pullMaker.newPullParser();
	            InputStream fis = url.openStream();
	            //InputStream fis = new ByteArrayInputStream(xml.getBytes("UTF-8"));

	            parser.setInput(fis, null);

	        	boolean personaName = false;
	        	
	            int eventType = parser.getEventType();
	            while (eventType != XmlPullParser.END_DOCUMENT) {
	                switch (eventType) {
	                case XmlPullParser.START_DOCUMENT:
	                    break;
	                case XmlPullParser.START_TAG:
	                    if (parser.getName().equals("personaname")) {
	                    	personaName = true;
	                    }
	                    break;
	                case XmlPullParser.END_TAG:
	                    if (parser.getName().equals("personaname")) {
	                    	personaName = false;
	                    }
	                    break;
	                case XmlPullParser.TEXT:
	                    if (personaName) {
	                    	return parser.getText();
	                    }
	                    break;

	                }
	                eventType = parser.next();
	            }

	        } catch (Exception e) {
	            Log.e("xml_perf", "Pull parser failed", e);
	        }
			return null;
		}
		
        protected void onPostExecute(String result) {
        	if (result != null){
            	DataBaseHelper.cacheSteamUserName(Long.parseLong(stringToReplace), result);
        		
        		int index = attributeText.toString().indexOf(stringToReplace);
        		if (index != -1) {
		        	attributeText.replace(index, index + stringToReplace.length(), result);
		        	tvAttributes.setText(attributeText);
        		}
        	}
        }
    }
}
