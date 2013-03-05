package com.minder.app.tf2backpack.frontend;

import java.lang.ref.WeakReference;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import com.minder.app.tf2backpack.Internet;
import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.SteamUser;
import com.minder.app.tf2backpack.backend.AsyncTaskListener;
import com.minder.app.tf2backpack.backend.DataBaseHelper;
import com.minder.app.tf2backpack.backend.DataManager.Request;
import com.minder.app.tf2backpack.backend.ProgressUpdate;
import com.minder.app.tf2backpack.backend.SteamException;

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

	private WeakReference<OnPlayerSelectedListener> playerSelectedListener;
	private WeakReference<OnSearchClickedListener> searchClickedListener;
	private Typeface tf2Secondary;
	private Button buttonOk;
	private AutoCompleteTextView editTextPlayer;
	private ProgressDialog myProgressDialog;
	private AdView adView;
	
	private Request request;

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
		
		final Button buttonCommunity = (Button)view.findViewById(R.id.buttonCommunityId);
		buttonCommunity.setOnClickListener(onButtonCommunityClick);

		// Look up the AdView as a resource and load a request.
		adView = (AdView) view.findViewById(R.id.ad);

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

	private OnClickListener onButtonOkClick = new OnClickListener() {
		public void onClick(View v) {
			textInputDone();
		}
	};

	private OnClickListener onButtonSearchClick = new OnClickListener() {
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
	
	private OnClickListener onButtonCommunityClick = new OnClickListener() {		
		public void onClick(View v) {
        	GenericDialog dialog = GenericDialog.newInstance(R.string.community_id, R.string.tutorial_how_to_set_community_id);
        	dialog.setNeutralButtonText(android.R.string.ok);
        	dialog.setIcon(android.R.drawable.ic_dialog_info);
        	dialog.setClickListener(new DialogInterface.OnClickListener() {				
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
        	
        	dialog.show(getFragmentManager(), "communityDialog");
		}
	};

	private void textInputDone() {
		if (!editTextPlayer.getText().toString().equals("")) {
			request = App.getDataManager().requestVerifyPlayer(verifyPlayerListener, editTextPlayer.getText().toString());
			
			InputMethodManager imm = (InputMethodManager) getActivity()
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(editTextPlayer.getWindowToken(), 0);
		}
	}
	
	private DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {	
		public void onClick(DialogInterface dialog, int which) {
			dialog.dismiss();
			App.getDataManager().cancelRequest(request);
			request = null;
		}
	};
	
    private AsyncTaskListener verifyPlayerListener = new AsyncTaskListener () {
    	private GenericDialog dialog;
    	
		public void onPreExecute() {
			dialog = GenericDialog.newInstance(R.string.please_wait_title, R.string.verifying_player_info);
			
			dialog.setNeutralButtonText(android.R.string.cancel);
			dialog.setClickListener(dialogClickListener);
			dialog.setShowProgress(true);
			
			dialog.show(getFragmentManager(), "ProgressDialog");
		}

		public void onProgressUpdate(ProgressUpdate object) {
		}

		public void onPostExecute(Request request) {
			if (SelectPlayerFragment.this.request != request)
				return;
			
			dialog.dismiss();
			Object result = request.getData();
			if (result != null) {
				SteamUser user = (SteamUser)result;
				
				notifyPlayerSelectedListener(user);
				
				storeName(editTextPlayer.getText().toString());
			} else {
				// handle error
				Exception e = request.getException();
				if (Internet.isOnline(getActivity())) {
					if (e instanceof UnknownHostException) {
						Toast.makeText(getActivity(),
								R.string.no_steam_api, Toast.LENGTH_LONG)
								.show();
					} else if (e instanceof SocketTimeoutException) {
						Toast.makeText(getActivity(), R.string.connection_timed_out, Toast.LENGTH_LONG).show();
					} else if (e instanceof SteamException) {
						Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
					} else {
						Toast.makeText(getActivity(),
								e.getLocalizedMessage(), Toast.LENGTH_LONG)
								.show();
					}
				} else {
					Toast.makeText(getActivity(), R.string.no_internet,
							Toast.LENGTH_LONG).show();
				}
			}
		}
	};

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