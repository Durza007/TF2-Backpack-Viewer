package com.minder.app.tf2backpack.frontend;

import java.lang.ref.WeakReference;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.google.ads.AdView;
import com.minder.app.tf2backpack.App;
import com.minder.app.tf2backpack.HttpConnection;
import com.minder.app.tf2backpack.Internet;
import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.SteamUser;
import com.minder.app.tf2backpack.backend.DataBaseHelper;

public class SelectPlayerFragment extends Fragment {
	private class ItemAutoTextAdapter extends CursorAdapter implements
			AdapterView.OnItemClickListener {

		/**
		 * Constructor. Note that no cursor is needed when we create the
		 * adapter. Instead, cursors are created on demand when completions are
		 * needed for the field. (see
		 * {@link ItemAutoTextAdapter#runQueryOnBackgroundThread(CharSequence)}
		 * .)
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

			Cursor cursor = App
					.getDataManager()
					.getDatabaseHandler()
					.querySql("SELECT * FROM name_history WHERE name LIKE ?",
							params);
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
			final View view = inflater.inflate(
					android.R.layout.simple_dropdown_item_1line, parent, false);

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
		public void onItemClick(AdapterView<?> listView, View view,
				int position, long id) {
			// Get the cursor, positioned to the corresponding row in the result
			// set
			Cursor cursor = (Cursor) listView.getItemAtPosition(position);

			// Get the state's capital from this row in the database.
			String name = cursor
					.getString(cursor.getColumnIndexOrThrow("name"));

			// Update the parent class's TextView
			editTextPlayer.setText(name);
			editTextPlayer.setSelection(name.length());
		}
	}

	public static interface OnSearchClickedListener {
		public void onSearchClicked(String searchQuery);
	}

	private final static int COMMUNITY_ID_TUTORIAL = 1;

	private WeakReference<OnPlayerSelectedListener> playerSelectedListener;
	private WeakReference<OnSearchClickedListener> searchClickedListener;
	private Typeface tf2Secondary;
	private Button buttonOk;
	private AutoCompleteTextView editTextPlayer;
	private ProgressDialog myProgressDialog;
	private AdView adView;

	public void setPlayerSelectedListener(OnPlayerSelectedListener listener) {
		this.playerSelectedListener = new WeakReference<OnPlayerSelectedListener>(
				listener);
	}

	private void notifyPlayerSelectedListener(SteamUser user) {
		OnPlayerSelectedListener l = playerSelectedListener.get();
		if (l != null)
			l.onPlayerSelected(user, 0);
	}

	public void setSearchClickedListener(OnSearchClickedListener listener) {
		this.searchClickedListener = new WeakReference<OnSearchClickedListener>(
				listener);
	}

	private void notifySearchClickedListener(String searchQuery) {
		OnSearchClickedListener l = searchClickedListener.get();
		if (l != null) {
			l.onSearchClicked(searchQuery);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setRetainInstance(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		final View view = inflater.inflate(R.layout.enter_user_id, container);

		/*
		 * Bundle extras = getIntent().getExtras(); if(extras != null){ String
		 * title = extras.getString("title"); if (title != null){
		 * this.setTitle(title);
		 * 
		 * } }
		 */

		/*
		 * if (getIntent().getAction() != null){ if
		 * (getIntent().getAction().equals
		 * ("com.minder.app.tf2backpack.SET_STEAMID")){ setSteamId = true; }
		 * else { setSteamId = false; } }
		 */

		tf2Secondary = Typeface.createFromAsset(getActivity().getAssets(),
				"fonts/TF2secondary.ttf");

		TextView textView = (TextView) view.findViewById(R.id.user_id_textView);
		textView.setTypeface(tf2Secondary, 0);

		editTextPlayer = (AutoCompleteTextView) view
				.findViewById(R.id.EditTextSteamId);

		ItemAutoTextAdapter adapter = new ItemAutoTextAdapter(getActivity());
		editTextPlayer.setAdapter(adapter);
		editTextPlayer.setOnItemClickListener(adapter);
		editTextPlayer.setThreshold(1);

		editTextPlayer.setOnEditorActionListener(new OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				boolean handled = false;
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					textInputDone();

					handled = true;
				}
				return handled;
			}
		});

		buttonOk = (Button) view.findViewById(R.id.ButtonOk);
		buttonOk.setOnClickListener(onButtonOkClick);

		Button buttonSearch = (Button) view.findViewById(R.id.buttonSearch);
		buttonSearch.setOnClickListener(onButtonSearchClick);

		// Look up the AdView as a resource and load a request.
		adView = (AdView) view.findViewById(R.id.ad);
		/*
		 * if (adView != null) { adView.loadAd(new AdRequest()); }
		 */

		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		boolean dontShowInfo = sp.getBoolean("community_info_dont_show", false);
		if (!dontShowInfo) {
			// TODO deprecated stuff
			getActivity().showDialog(COMMUNITY_ID_TUTORIAL);
		}

		return null;
	}

	@Override
	public void onPause() {
		super.onPause();
		if (myProgressDialog != null) {
			if (myProgressDialog.isShowing()) {
				try {
					myProgressDialog.dismiss();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (adView != null) {
			adView.destroy();
		}
	}

	OnClickListener onButtonOkClick = new OnClickListener() {
		public void onClick(View v) {
			textInputDone();
		}
	};

	OnClickListener onButtonSearchClick = new OnClickListener() {
		public void onClick(View v) {
			final String searchQuery = editTextPlayer.getText().toString();
			if (!searchQuery.equals("")) {
				notifySearchClickedListener(searchQuery);

				InputMethodManager imm = (InputMethodManager) getActivity()
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(editTextPlayer.getWindowToken(), 0);
			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			// TODO handle this!
			notifyPlayerSelectedListener(null);

			/*
			 * if(setSteamId == false){ setResult(Activity.RESULT_OK , data);
			 * 
			 * Bundle bundle = data.getExtras();
			 * storeName(bundle.getString("name")); } else { SharedPreferences
			 * playerPrefs = getActivity().getSharedPreferences("player",
			 * Activity.MODE_PRIVATE); SharedPreferences.Editor editor =
			 * playerPrefs.edit(); Bundle bundle = data.getExtras();
			 * editor.putString("name", bundle.getString("name"));
			 * editor.putString("id", bundle.getString("id")); editor.commit();
			 * 
			 * storeName(bundle.getString("name")); }
			 */
		}
	}

	private void textInputDone() {
		if (!editTextPlayer.getText().toString().equals("")) {
			myProgressDialog = ProgressDialog.show(getActivity(),
					"Please Wait...", "Verifying player info...");
			fetchPlayerId();
		}
	}

	private void fetchPlayerId() {
		Handler handler = new Handler() {
			public void handleMessage(Message message) {
				switch (message.what) {
				case HttpConnection.DID_START: {
					break;
				}
				case HttpConnection.DID_SUCCEED: {
					String textId = (String) message.obj;
					Log.d("GetPlayer", textId);
					int idStartIndex = textId.indexOf("<steamID64>");
					int idEndIndex = textId.indexOf("</steamID64>");

					// check if player id was present
					if (idStartIndex == -1) {
						idStartIndex = textId.indexOf("![CDATA[");
						idEndIndex = textId.indexOf("]]");
						if (myProgressDialog != null) {
							if (myProgressDialog.isShowing()) {
								try {
									myProgressDialog.dismiss();
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
						if (idStartIndex == -1) {
							Toast.makeText(getActivity(),
									"Failed to verify player",
									Toast.LENGTH_LONG).show();
						} else {
							Toast.makeText(
									getActivity(),
									textId.substring(idStartIndex + 8,
											idEndIndex), Toast.LENGTH_LONG)
									.show();
						}

					} else {
						// TODO check this!
						SteamUser user = new SteamUser();
						user.steamName = editTextPlayer.getText().toString();
						user.steamdId64 = Long.parseLong(textId.substring(
								idStartIndex + 11, idEndIndex));
						notifyPlayerSelectedListener(user);

						/*
						 * if(setSteamId == false){ Intent result = new
						 * Intent(); result.putExtra("name",
						 * editTextPlayer.getText()); result.putExtra("id",
						 * textId.substring(idStartIndex + 11, idEndIndex));
						 * setResult(RESULT_OK , result); } else {
						 * SharedPreferences playerPrefs =
						 * getActivity().getSharedPreferences("player",
						 * MODE_PRIVATE); SharedPreferences.Editor editor =
						 * playerPrefs.edit(); editor.putString("name",
						 * editTextPlayer.getText().toString());
						 * editor.putString("id", textId.substring(idStartIndex
						 * + 11, idEndIndex)); editor.commit(); }
						 */
						if (myProgressDialog != null) {
							if (myProgressDialog.isShowing()) {
								try {
									myProgressDialog.dismiss();
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}

						storeName(editTextPlayer.getText().toString());
					}
					break;
				}
				case HttpConnection.DID_ERROR: {
					Exception e = (Exception) message.obj;
					e.printStackTrace();
					if (Internet.isOnline(getActivity())) {
						if (e instanceof UnknownHostException) {
							Toast.makeText(getActivity(),
									R.string.no_steam_api, Toast.LENGTH_LONG)
									.show();
						} else if (e instanceof SocketTimeoutException) {
							Toast.makeText(getActivity(), R.string.connection_timed_out, Toast.LENGTH_LONG).show();
						} else {
							Toast.makeText(getActivity(),
									e.getLocalizedMessage(), Toast.LENGTH_LONG)
									.show();
						}
					} else {
						Toast.makeText(getActivity(), R.string.no_internet,
								Toast.LENGTH_LONG).show();
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
		if (newId.equals(id)) {
			id = java.net.URLEncoder.encode(id);
			new HttpConnection(handler).getSpecificLines(
					"http://steamcommunity.com/id/" + id + "/?xml=1", 4);
		} else {
			new HttpConnection(handler).getSpecificLines(
					"http://steamcommunity.com/profiles/" + newId + "/?xml=1",
					4);
		}
	}

	private String CheckId(String id) {
		boolean steamId = id.matches("(?i)STEAM_\\d:\\d:\\d+");
		if (!steamId) {
			steamId = id.matches("\\d:\\d:\\d+");
		}
		if (steamId) {
			final int yIndex = id.indexOf(":");
			final long y = Long.parseLong(id.substring(yIndex + 1, yIndex + 2));

			final int zIndex = id.indexOf(':', yIndex + 1);
			final long z = Long.parseLong(id.substring(zIndex + 1));
			;

			final long communityId = (z * 2) + 76561197960265728l + y;

			id = String.valueOf(communityId);
		} else if (id.matches("\\d:\\d+")) {
			// X:XXXXXX format
			final long y = Long.parseLong(id.substring(0, 1));

			final int zIndex = id.indexOf(':', 1);
			final long z = Long.parseLong(id.substring(zIndex + 1));
			;

			final long communityId = (z * 2) + 76561197960265728l + y;

			id = String.valueOf(communityId);
		}

		return id;
	}

	/*
	 * @Override public Dialog onCreateDialog(int id) { switch (id) { case
	 * COMMUNITY_ID_TUTORIAL: return new AlertDialog.Builder(getActivity())
	 * .setIcon(R.drawable.ic_dialog_info) .setTitle(R.string.community_id)
	 * .setMessage(R.string.tutorial_how_to_set_community_id)
	 * .setPositiveButton(android.R.string.ok, new
	 * DialogInterface.OnClickListener() { public void onClick(DialogInterface
	 * dialog, int whichButton) { } })
	 * .setNeutralButton(R.string.alert_dialog_dont_show_again, new
	 * DialogInterface.OnClickListener() { public void onClick(DialogInterface
	 * dialog, int whichButton) { SharedPreferences sp =
	 * PreferenceManager.getDefaultSharedPreferences(getActivity()); Editor
	 * editor = sp.edit(); editor.putBoolean("community_info_dont_show", true);
	 * editor.commit(); } }).create(); } return null; }
	 */

	private void storeName(String name) {
		Thread thread = new Thread(new SaveNameToDb(App.getAppContext(), name));
		thread.setDaemon(true);
		thread.start();
	}

	private static class SaveNameToDb implements Runnable {
		private DataBaseHelper db;
		private SQLiteDatabase sqlDb;
		private String name;

		public SaveNameToDb(Context context, String name) {
			this.db = new DataBaseHelper(context);
			this.sqlDb = db.getReadableDatabase();
			this.name = name;
		}

		public void run() {
			Cursor c = sqlDb.rawQuery("SELECT * FROM name_history WHERE name='"
					+ name + "'", null);
			if (c == null || c.getCount() < 1) {
				sqlDb.execSQL("INSERT INTO name_history (name) VALUES ('"
						+ name + "')");
			}
			sqlDb.close();
			db.close();
		}
	}
}