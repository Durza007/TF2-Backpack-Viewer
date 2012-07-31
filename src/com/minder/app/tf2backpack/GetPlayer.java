package com.minder.app.tf2backpack;

import java.net.UnknownHostException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.google.ads.AdView;
import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.R.drawable;
import com.minder.app.tf2backpack.R.id;
import com.minder.app.tf2backpack.R.layout;
import com.minder.app.tf2backpack.R.string;

public class GetPlayer extends Activity {
    private class ItemAutoTextAdapter extends CursorAdapter implements AdapterView.OnItemClickListener {
    	
    	/**
		 * Constructor. Note that no cursor is needed when we create the
		 * adapter. Instead, cursors are created on demand when completions are
		 * needed for the field. (see
		 * {@link ItemAutoTextAdapter#runQueryOnBackgroundThread(CharSequence)}.)
		 *
		 * @param dbHelper
		 *            The AutoCompleteDbAdapter in use by the outer class
		 *            object.
		 */
		public ItemAutoTextAdapter(Context context) {
		    // Call the CursorAdapter constructor with a null Cursor.
		    super(context, null);
		}
		
		/**
		 * Invoked by the AutoCompleteTextView field to get completions for the
		 * current input.
		 *
		 * NOTE: If this method either throws an exception or returns null, the
		 * Filter class that invokes it will log an error with the traceback,
		 * but otherwise ignore the problem. No choice list will be displayed.
		 * Watch those error logs!
		 *
		 * @param constraint
		 *            The input entered thus far. The resulting query will
		 *            search for states whose name begins with this string.
		 * @return A Cursor that is positioned to the first row (if one exists)
		 *         and managed by the activity.
		 */
		@Override
		public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
		    String query = constraint != null ? constraint.toString() : null;
		    if (query != null) {
		    	query = query + "_%";
		    }
		
		    String params[] = { query };
		    if (query == null) {
		    	params = null;
		    }

		    Cursor cursor = Util.getDbHandler().querySql("SELECT * FROM name_history WHERE name LIKE ?", params);	  
		    return cursor;
		}
		
		/**
		 * Called by the AutoCompleteTextView field to get the text that will be
		 * entered in the field after a choice has been made.
		 *
		 * @param Cursor
		 *            The cursor, positioned to a particular row in the list.
		 * @return A String representing the row's text value. (Note that this
		 *         specializes the base class return value for this method,
		 *         which is {@link CharSequence}.)
		 */
		@Override
		public String convertToString(Cursor cursor) {
		    final int columnIndex = cursor.getColumnIndexOrThrow("name");
		    final String str = cursor.getString(columnIndex);
		    return str;
		}
		
		/**
		 * Called by the ListView for the AutoCompleteTextView field to display
		 * the text for a particular choice in the list.
		 *
		 * @param view
		 *            The TextView used by the ListView to display a particular
		 *            choice.
		 * @param context
		 *            The context (Activity) to which this form belongs;
		 *            equivalent to {@code SelectState.this}.
		 * @param cursor
		 *            The cursor for the list of choices, positioned to a
		 *            particular row.
		 */
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
		    final String text = convertToString(cursor);
		    ((TextView) view).setText(text);
		}
		
		/**
		 * Called by the AutoCompleteTextView field to display the text for a
		 * particular choice in the list.
		 *
		 * @param context
		 *            The context (Activity) to which this form belongs;
		 *            equivalent to {@code SelectState.this}.
		 * @param cursor
		 *            The cursor for the list of choices, positioned to a
		 *            particular row.
		 * @param parent
		 *            The ListView that contains the list of choices.
		 *
		 * @return A new View (really, a TextView) to hold a particular choice.
		 */
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			final LayoutInflater inflater = LayoutInflater.from(context);
		    final View view =
		            inflater.inflate(android.R.layout.simple_dropdown_item_1line,
		                    parent, false);
		    
		    return view;
		}
		
		/**
		 * Called by the AutoCompleteTextView field when a choice has been made
		 * by the user.
		 *
		 * @param listView
		 *            The ListView containing the choices that were displayed to
		 *            the user.
		 * @param view
		 *            The field representing the selected choice
		 * @param position
		 *            The position of the choice within the list (0-based)
		 * @param id
		 *            The id of the row that was chosen (as provided by the _id
		 *            column in the cursor.)
		 */
		public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
			// Get the cursor, positioned to the corresponding row in the result set
		    Cursor cursor = (Cursor)listView.getItemAtPosition(position);
		
		    // Get the state's capital from this row in the database.
		    String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
		
		    // Update the parent class's TextView
		    editTextPlayer.setText(name);
		    editTextPlayer.setSelection(name.length());
		}
    }
	
	
	private final static int COMMUNITY_ID_TUTORIAL = 1;
	
	private Typeface tf2Secondary;
	private Button buttonOk;
	private AutoCompleteTextView editTextPlayer;
	private ProgressDialog myProgressDialog;
	private Context mContext;
	private AdView adView;
	
	private boolean setSteamId;
	
    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.enter_user_id);
    
        Bundle extras = getIntent().getExtras();
        if(extras != null){
        	String title = extras.getString("title");
        	if (title != null){
        		this.setTitle(title);
        	}
        }
        
        if (getIntent().getAction() != null){
	        if (getIntent().getAction().equals("com.minder.app.tf2backpack.SET_STEAMID")){
	        	setSteamId = true;
	        } else {
	        	setSteamId = false;
	        }
        }
        
        mContext = this;
        
        tf2Secondary = Typeface.createFromAsset(getAssets(), "fonts/TF2secondary.ttf");
        
        TextView textView = (TextView)findViewById(R.id.user_id_textView);
        textView.setTypeface(tf2Secondary, 0);
        
        editTextPlayer = (AutoCompleteTextView)findViewById(R.id.EditTextSteamId);
        
        ItemAutoTextAdapter adapter = new ItemAutoTextAdapter(this);
        editTextPlayer.setAdapter(adapter);
        editTextPlayer.setOnItemClickListener(adapter);
        editTextPlayer.setThreshold(1);
        
        editTextPlayer.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                	textInputDone();
                	
                    handled = true;
                }
                return handled;
            }
        });
        
        buttonOk = (Button)findViewById(R.id.ButtonOk);
        buttonOk.setOnClickListener(onButtonOkClick);
        
        Button buttonSearch = (Button)findViewById(R.id.buttonSearch);
        buttonSearch.setOnClickListener(onButtonSearchClick);
        
        // Look up the AdView as a resource and load a request.
        adView = AdMobActivity.createAdView(adView, this);
        /*if (adView != null) {
        	adView.loadAd(new AdRequest());
        }*/
        
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        boolean dontShowInfo = sp.getBoolean("community_info_dont_show", false);
        if (!dontShowInfo) {
        	showDialog(COMMUNITY_ID_TUTORIAL);
        }
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	if (myProgressDialog != null)
    	{
			if (myProgressDialog.isShowing()){
				try {
					myProgressDialog.dismiss();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			
    	}
    }
    
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	if (adView != null) {
    		adView.destroy();
    	}
    }
    
    OnClickListener onButtonOkClick = new OnClickListener(){
		public void onClick(View v) {
			textInputDone();
		}
    };
    
    OnClickListener onButtonSearchClick = new OnClickListener(){
		public void onClick(View v) {
			if (!editTextPlayer.getText().toString().equals("")){
				Intent intent = new Intent(GetPlayer.this, PlayerList.class);
				intent.setAction("com.minder.app.tf2backpack.SEARCH");
				intent.putExtra(SearchManager.QUERY, editTextPlayer.getText().toString());

				intent.putExtra("setid", true);		
				startActivityForResult(intent, 0);
			} else {
				onSearchRequested();
			}
		}
    };
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	if (resultCode == RESULT_OK) {
			if(setSteamId == false){
				setResult(RESULT_OK , data);
				
				Bundle bundle = data.getExtras();
				storeName(bundle.getString("name"));
			} else {
				SharedPreferences playerPrefs = GetPlayer.this.getSharedPreferences("player", MODE_PRIVATE);
	    		SharedPreferences.Editor editor = playerPrefs.edit();
	    		Bundle bundle = data.getExtras();
	    		editor.putString("name", bundle.getString("name"));
	    		editor.putString("id", bundle.getString("id"));
	    		editor.commit();
	    		
	    		storeName(bundle.getString("name"));
			}
			
	    	finish();
    	}
    }
    
    private void textInputDone() {
		if (!editTextPlayer.getText().toString().equals("")){
			myProgressDialog = ProgressDialog.show(mContext, "Please Wait...", "Verifying player info...");
			FetchPlayerId();
		}
    }
    
    private void FetchPlayerId(){
		Handler handler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
					case HttpConnection.DID_START: {
						break;
					}
					case HttpConnection.DID_SUCCEED: {
						String textId = (String)message.obj;
						int idStartIndex = textId.indexOf("<steamID64>");
						int idEndIndex = textId.indexOf("</steamID64>");
						
						// check if player id was present
						if (idStartIndex == -1){
							idStartIndex = textId.indexOf("![CDATA[");
							idEndIndex = textId.indexOf("]]");
					    	if (myProgressDialog != null)
					    	{
								if (myProgressDialog.isShowing()){
									try {
										myProgressDialog.dismiss();
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
					    	}
							if (idStartIndex == -1){
								Toast.makeText(GetPlayer.this, "Failed to verify player", Toast.LENGTH_LONG).show();
							} else {
								Toast.makeText(GetPlayer.this, textId.substring(idStartIndex + 8, idEndIndex), Toast.LENGTH_LONG).show();
							}
							
						} else {	
							if(setSteamId == false){
								Intent result = new Intent();
								result.putExtra("name", editTextPlayer.getText());
								result.putExtra("id", textId.substring(idStartIndex + 11, idEndIndex));
								setResult(RESULT_OK , result);
							} else {
								SharedPreferences playerPrefs = GetPlayer.this.getSharedPreferences("player", MODE_PRIVATE);
					    		SharedPreferences.Editor editor = playerPrefs.edit();
					    		editor.putString("name", editTextPlayer.getText().toString());
					    		editor.putString("id", textId.substring(idStartIndex + 11, idEndIndex));
					    		editor.commit();
							}
					    	if (myProgressDialog != null)
					    	{
								if (myProgressDialog.isShowing()){
									try {
										myProgressDialog.dismiss();
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
					    	}
							
					    	storeName(editTextPlayer.getText().toString());
							finish();
						}
						break;
					}
					case HttpConnection.DID_ERROR: {
						Exception e = (Exception) message.obj;
						e.printStackTrace();
						if (Internet.isOnline(GetPlayer.this)){
							if (e instanceof UnknownHostException){
								Toast.makeText(GetPlayer.this, R.string.no_steam_api, Toast.LENGTH_LONG).show();
							} else {
								Toast.makeText(GetPlayer.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
							}
						} else {
							Toast.makeText(GetPlayer.this, R.string.no_internet, Toast.LENGTH_LONG).show();
						}
	        			try {
	        				myProgressDialog.dismiss();
	        				myProgressDialog = null;
						} catch (Exception e2) {
							e2.printStackTrace();
						}
						break;
					}
				}
			}
		};
		String id = editTextPlayer.getText().toString();
		// remove spacing at end and beginning
		id = id.trim();
		
		// check what format the string is written in
		String newId = CheckId(id);
		
		// if the string was changed, it was changed to 64 bit steam id
		if (newId.equals(id)){
			id = java.net.URLEncoder.encode(id);
			new HttpConnection(handler)
				.getSpecificLines("http://steamcommunity.com/id/" + id + "/?xml=1", 2);
		} else {
			new HttpConnection(handler)
				.getSpecificLines("http://steamcommunity.com/profiles/" + newId + "/?xml=1", 2);
		}
    }
    
    private String CheckId(String id){
    	boolean steamId = id.matches("(?i)STEAM_\\d:\\d:\\d+");
    	if (!steamId){
    		steamId = id.matches("\\d:\\d:\\d+");
    	}
    	if (steamId){
			final int yIndex = id.indexOf(":");
			final long y = Long.parseLong(id.substring(yIndex + 1, yIndex + 2));
			
			final int zIndex = id.indexOf(':', yIndex + 1);
			final long z = Long.parseLong(id.substring(zIndex + 1));;
			
			final long communityId = (z * 2) + 76561197960265728l + y;
			
			id = String.valueOf(communityId);
    	} else if (id.matches("\\d:\\d+")){
    		// X:XXXXXX format
			final long y = Long.parseLong(id.substring(0, 1));
			
			final int zIndex = id.indexOf(':', 1);
			final long z = Long.parseLong(id.substring(zIndex + 1));;
			
			final long communityId = (z*2) + 76561197960265728l + y;
			
			id = String.valueOf(communityId);	
    	}
    	
    	return id;
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case COMMUNITY_ID_TUTORIAL:
            return new AlertDialog.Builder(this)
                .setIcon(R.drawable.ic_dialog_info)
                .setTitle(R.string.community_id)
                .setMessage(R.string.tutorial_how_to_set_community_id)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	/*gameSchemePref = GameSchemeDownloader.this.getSharedPreferences("gamescheme", MODE_PRIVATE);
                    	GameSchemeDownloader.this.setContentView(R.layout.downloader);
                    	
                    	downloadText = (TextView)findViewById(R.id.TextViewDownloadStatus);
                    	
                    	imageDownloadProgress = (ProgressBar)findViewById(R.id.ProgressBarImageDownload);
                    	imageDownloadProgress.setProgress(0);
                    	Download();*/
                    }
                })
                .setNeutralButton(R.string.alert_dialog_dont_show_again, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
                    	Editor editor = sp.edit();
                    	editor.putBoolean("community_info_dont_show", true);
                    	editor.commit();
                    }
                }).create();
        }
        return null;
    }
    
    private void storeName(String name) {
    	Thread thread = new Thread(new SaveNameToDb(getApplicationContext(), name));
    	thread.setDaemon(true);
    	thread.start();
    }
    
    private static class SaveNameToDb implements Runnable {
    	private DataBaseHelper db;
    	private SQLiteDatabase sqlDb;
    	private String name;

    	public SaveNameToDb(Context context, String name) {
            this.db = new DataBaseHelper(context);
    		this.sqlDb =  db.getReadableDatabase();
    		this.name = name;
    	}
    	
    	
		public void run() {
			Cursor c = sqlDb.rawQuery("SELECT * FROM name_history WHERE name='" + name + "'", null);
			if (c == null || c.getCount() < 1) {
				sqlDb.execSQL("INSERT INTO name_history (name) VALUES ('" + name + "')");
			}
			sqlDb.close();
			db.close();
		}
    	
    }

}
