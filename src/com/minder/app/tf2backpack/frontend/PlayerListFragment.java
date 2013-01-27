package com.minder.app.tf2backpack.frontend;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.google.ads.AdView;
import com.minder.app.tf2backpack.App;
import com.minder.app.tf2backpack.PlayerAdapter;
import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.SteamUser;
import com.minder.app.tf2backpack.backend.DataManager.Request;

public class PlayerListFragment extends Fragment {
	/**
	 * Interface used by other classes to register notifications when a Steam
	 * user has been selected
	 */
	public static interface OnPlayerSelectedListener {
		public void onPlayerSelected(SteamUser user, int index);
	}

	private List<OnPlayerSelectedListener> listeners;
	private final boolean isAboveGingerBread;

	private boolean ready = false;
	private boolean loadingMore = false;
	private boolean nothingMoreToLoad = false;
	private boolean loadAvatars = false;

	private final int CONTEXTMENU_VIEW_BACKPACK = 0;
	private final int CONTEXTMENU_VIEW_STEAMPAGE = 1;

	//private ProgressDialog mProgress;
	private AdView adView;

	private final boolean friendList = true;
	private boolean searchList;
	public int searchPage = 1;
	public String searchQuery;

	private View progressContainer;
	private View listContainer;
	private boolean choiceModeEnabled = false;
	private ListView playerList;
	private PlayerAdapter adapter;
	private View footerView;
	private View noResultFooterView;
	private int totalInfoDownloads;

	private List<SteamUser> steamUserList;
	private Request currentRequest;

	private OnItemClickListener clickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			final SteamUser user = (SteamUser) adapter.getItem(position);

			view.setPressed(true);
			notifyListeners(user, position);
		}
	};

	public PlayerListFragment() {
		isAboveGingerBread = android.os.Build.VERSION.SDK_INT > 10;
		listeners = new LinkedList<OnPlayerSelectedListener>();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setRetainInstance(true);

		if (!isAboveGingerBread) {
			this.setHasOptionsMenu(true);
		}

		getSettings();
	}

	/**
	 * Sets the player-list that will be displayed by this fragment
	 * 
	 * @param players The list of players
	 */
	public void setPlayerList(List<SteamUser> players) {
		this.steamUserList = players;

		if (adapter != null) {
			adapter.setPlayers(players);
			progressContainer.setVisibility(View.GONE);
			listContainer.setVisibility(View.VISIBLE);
		}
	}

	public void setListItemSelectable(boolean select) {
		choiceModeEnabled = select;

		if (playerList != null) {
			if (choiceModeEnabled) {
				playerList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			} else {
				playerList.setChoiceMode(ListView.CHOICE_MODE_NONE);
			}
		}
	}
	
	public void setSelectedItem(int index) {
		if (playerList != null) {
			if (choiceModeEnabled) {
				playerList.setItemChecked(index, true);
			}
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.list_content, container, false);

		// Look up the AdView as a resource and load a request.
		adView = (AdView) view.findViewById(R.id.ad);
		
		listContainer = view.findViewById(R.id.listContainer);
		progressContainer = view.findViewById(R.id.progressContainer);

		playerList = (ListView) view.findViewById(android.R.id.list);
		setListItemSelectable(choiceModeEnabled);

		// Set up our adapter
		// TODO maybe re-think if this needs an activity?
		adapter = new PlayerAdapter(getActivity());
		adapter.setShowAvatars(loadAvatars);
		footerView = inflater.inflate(R.layout.loading_footer, null);
		noResultFooterView = inflater.inflate(R.layout.noresult_footer, null);
		playerList.addFooterView(footerView, null, false);
		playerList.setAdapter(adapter);
		playerList.removeFooterView(footerView);

		playerList.setBackgroundResource(R.color.bg_color);
		playerList.setCacheColorHint(this.getResources().getColor(
				R.color.bg_color));
		adapter.setComparator(new byPersonaState());

		playerList.setOnItemClickListener(clickListener);

		if (steamUserList != null) {
			adapter.setPlayers(steamUserList);
		} else {
			progressContainer.setVisibility(View.VISIBLE);
			listContainer.setVisibility(View.GONE);
		}

		/*
		 * playerList.setOnItemClickListener(new OnItemClickListener() { public
		 * void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3)
		 * { // TODO This is NOT a good solution final SteamUser player =
		 * (SteamUser)adapter.getItem(arg2); if (player.steamdId64 == 0){ //
		 * this only happens when we search Log.v("PlayerList",
		 * "64-bit id missing - fetching"); Handler handler = new Handler() {
		 * public void handleMessage(Message message) { switch (message.what) {
		 * case HttpConnection.DID_START: { break; } case
		 * HttpConnection.DID_SUCCEED: { String textId = (String)message.obj;
		 * int idStartIndex = textId.indexOf("<steamID64>"); int idEndIndex =
		 * textId.indexOf("</steamID64>");
		 * 
		 * // check if player id was present if (idStartIndex == -1){
		 * idStartIndex = textId.indexOf("![CDATA["); idEndIndex =
		 * textId.indexOf("]]"); if (idStartIndex == -1){ //TODO
		 * Toast.makeText(PlayerList.this, "Failed to verify player",
		 * Toast.LENGTH_LONG).show(); } else { //TODO
		 * Toast.makeText(PlayerList.this, textId.substring(idStartIndex + 8,
		 * idEndIndex), Toast.LENGTH_LONG).show(); }
		 * 
		 * } else { if (setPlayerId) { Intent result = new Intent();
		 * result.putExtra("name", ""); result.putExtra("id",
		 * textId.substring(idStartIndex + 11, idEndIndex)); setResult(RESULT_OK
		 * , result);
		 * 
		 * /*SharedPreferences playerPrefs =
		 * PlayerList.this.getSharedPreferences("player", MODE_PRIVATE); Editor
		 * editor = playerPrefs.edit(); editor.putString("id",
		 * textId.substring(idStartIndex + 11, idEndIndex)); editor.commit();
		 * startActivity(new Intent(PlayerList.this, Main.class));
		 */
		/*
		 * finish(); } else { startActivity(new Intent(PlayerList.this,
		 * Backpack.class).putExtra("id", textId.substring(idStartIndex + 11,
		 * idEndIndex))); } } break; } case HttpConnection.DID_ERROR: { break; }
		 * } } };
		 * 
		 * new HttpConnection(handler)
		 * .getSpecificLines("http://steamcommunity.com/id/" +
		 * player.communityId + "/?xml=1", 2); } else { if (setPlayerId) {
		 * Intent result = new Intent(); result.putExtra("name",
		 * player.steamName); result.putExtra("id",
		 * String.valueOf(player.steamdId64)); setResult(RESULT_OK , result);
		 * 
		 * finish(); } else { startActivity(new Intent(PlayerList.this,
		 * Backpack.class).putExtra("id", String.valueOf(player.steamdId64))); }
		 * } } });
		 */

		playerList
				.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {
					public void onCreateContextMenu(ContextMenu menu, View v,
							ContextMenuInfo menuInfo) {
						menu.setHeaderTitle(R.string.player_options);
						menu.add(0, CONTEXTMENU_VIEW_BACKPACK, 0,
								R.string.view_backpack);
						menu.add(0, CONTEXTMENU_VIEW_STEAMPAGE, 0,
								R.string.view_steampage);
					}

				});

		return view;
	}

	public void refreshList() {
		if (adapter != null)
			adapter.notifyDataSetChanged();
	}

	private void getSettings() {
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		loadAvatars = sp.getBoolean("showavatars", true);
	}

	@Override
	public void onPause() {
		super.onPause();

		// cancel any ongoing work
		if (currentRequest != null) {
			App.getDataManager().cancelRequest(currentRequest);
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		Log.d("PlayerList", "start");
		adapter.startBackgroundLoading();
	}

	@Override
	public void onStop() {
		super.onStop();
		adapter.stopBackgroundLoading();
		Log.d("PlayerList", "stop");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (adView != null) {
			adView.destroy();
		}
	}

	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		if (searchList) {
			boolean loadMore = firstVisibleItem + visibleItemCount >= totalItemCount;

			if (loadMore && ready && !loadingMore && !nothingMoreToLoad) {
				loadingMore = true;
				playerList.addFooterView(footerView, null, false);
				DownloadSearchListTask searchTask = new DownloadSearchListTask();
				searchTask.pageNumber = ++searchPage;
				searchTask.execute(searchQuery);
			}
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (friendList) {
			inflater.inflate(R.menu.friend_menu, menu);
		} else {
			inflater.inflate(R.menu.wrench_menu, menu);
		}
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_sort_number:
			adapter.setComparator(new byWrenchNumber());
			return true;
		case R.id.menu_sort_name:
			adapter.setComparator(new byPlayerName());
			return true;
		case R.id.menu_sort_persona_state:
			adapter.setComparator(new byPersonaState());
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		/* Switch on the ID of the item, to get what the user selected. */
		switch (item.getItemId()) {
		case CONTEXTMENU_VIEW_BACKPACK: {
			/* Get the selected item out of the Adapter by its position. */
			// Favorite favContexted = (Favorite)
			// mFavList.getAdapter().getItem(menuInfo.position);
			SteamUser player = (SteamUser) adapter.getItem(info.position);
			startActivity(new Intent(PlayerListFragment.this.getActivity(),
					BackpackActivity.class).putExtra("id",
					String.valueOf(player.steamdId64)));
			return true; /* true means: "we handled the event". */
		}
		case CONTEXTMENU_VIEW_STEAMPAGE: {
			SteamUser player = (SteamUser) adapter.getItem(info.position);
			Intent browser = new Intent();
			browser.setAction("android.intent.action.VIEW");
			browser.setData(Uri.parse("http://steamcommunity.com/profiles/"
					+ player.steamdId64));
			startActivity(browser);
			return true; /* true means: "we handled the event". */
		}
		}
		return false;
	}

	public void addPlayerSelectedListener(OnPlayerSelectedListener listener) {
		listeners.add(listener);
	}

	public void removePlayerSelectedListener(OnPlayerSelectedListener listener) {
		listeners.remove(listener);
	}

	private void notifyListeners(SteamUser user, int index) {
		for (OnPlayerSelectedListener listener : listeners) {
			listener.onPlayerSelected(user, index);
		}
	}

	private class DownloadSearchListTask extends
			AsyncTask<String, Void, ArrayList<SteamUser>> {
		public int pageNumber = 1;

		@Override
		protected ArrayList<SteamUser> doInBackground(String... params) {
			totalInfoDownloads = 0;
			ArrayList<SteamUser> players = new ArrayList<SteamUser>();

			String data = DownloadText("http://steamcommunity.com/actions/Search?p="
					+ pageNumber + "&T=Account&K=\"" + params[0] + "\"");

			int curIndex = 0;
			SteamUser newPlayer;
			while (curIndex < data.length()) {
				int index = data
						.indexOf(
								"<a class=\"linkTitle\" href=\"http://steamcommunity.com/profiles/",
								curIndex);
				if (index != -1) {
					int endIndex = data.indexOf("\">", index);
					newPlayer = new SteamUser();
					newPlayer.steamdId64 = Long.parseLong(data.substring(
							index + 62, endIndex));

					index = data.indexOf("</a>", endIndex);
					newPlayer.steamName = data.substring(endIndex + 2, index);

					players.add(newPlayer);
					curIndex = index;
				} else {
					index = data
							.indexOf(
									"<a class=\"linkTitle\" href=\"http://steamcommunity.com/id/",
									curIndex);
					if (index != -1) {
						int endIndex = data.indexOf("\">", index);
						newPlayer = new SteamUser();
						newPlayer.communityId = data.substring(index + 56,
								endIndex);

						index = data.indexOf("</a>", endIndex);
						newPlayer.steamName = data.substring(endIndex + 2,
								index);

						players.add(newPlayer);
						curIndex = index;
					}
					break;
				}
			}

			return players;
		}

		protected void onPostExecute(ArrayList<SteamUser> result) {
			if (result != null) {
				/*
				 * currentRequest = App.getDataManager().requestSteamUserInfo(
				 * getSteamUserInfoListener, ((ArrayList<SteamUser>) result));
				 */

				if (totalInfoDownloads == 0) {
					// set progressbar visible false
					setAdVisibility(View.VISIBLE);
					System.gc();
				}

				nothingMoreToLoad = true;
				if (result.size() > 0) {
					for (SteamUser p : result) {
						if (adapter.addPlayerInfo(p)) {
							nothingMoreToLoad = false;
						}
					}
				} else {
					playerList.removeFooterView(footerView);
					playerList.addFooterView(noResultFooterView, null, false);
				}
			}
			// GetPlayerInfoRange(0, 10);
			loadingMore = false;
			playerList.removeFooterView(footerView);
			ready = true;
		}

		private String DownloadText(String URL) {
			int BUFFER_SIZE = 2000;
			InputStream in = null;
			try {
				in = OpenHttpConnection(URL);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				return "";
			}

			if (in != null) {
				InputStreamReader isr = new InputStreamReader(in);
				int charRead;
				String str = "";
				char[] inputBuffer = new char[BUFFER_SIZE];
				try {
					while ((charRead = isr.read(inputBuffer)) > 0) {
						// ---convert the chars to a String---
						String readString = String.copyValueOf(inputBuffer, 0,
								charRead);
						str += readString;
						inputBuffer = new char[BUFFER_SIZE];
					}
					in.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return "";
				}
				return str;
			}
			return "";
		}

		private InputStream OpenHttpConnection(String urlString)
				throws IOException {
			InputStream in = null;
			int response = -1;

			URL url = new URL(urlString);
			URLConnection conn = url.openConnection();

			if (!(conn instanceof HttpURLConnection))
				throw new IOException("Not an HTTP connection");

			try {
				HttpURLConnection httpConn = (HttpURLConnection) conn;
				httpConn.setAllowUserInteraction(false);
				httpConn.setInstanceFollowRedirects(true);
				httpConn.setRequestMethod("GET");
				httpConn.connect();

				response = httpConn.getResponseCode();
				if (response == HttpURLConnection.HTTP_OK) {
					in = httpConn.getInputStream();
				}
			} catch (Exception ex) {
				throw new IOException("Error connecting");
			}
			return in;
		}
	}

	// obsolete
	public static class byWrenchNumber implements
			java.util.Comparator<SteamUser> {
		public int compare(SteamUser boy, SteamUser girl) {
			return boy.wrenchNumber - girl.wrenchNumber;
		}
	}

	public static class byPlayerName implements java.util.Comparator<SteamUser> {
		public int compare(SteamUser boy, SteamUser girl) {
			if (boy.steamName != null && girl.steamName != null) {
				return boy.steamName.compareToIgnoreCase(girl.steamName);
			} else {
				return 0;
			}
		}
	}

	public static class byPersonaState implements
			java.util.Comparator<SteamUser> {
		public int compare(SteamUser boy, SteamUser girl) {
			int boyState = boy.personaState.value;
			int girlState = girl.personaState.value;

			if (boyState > 1)
				boyState = 1;
			if (girlState > 1)
				girlState = 1;

			if (boy.gameId.length() > 0)
				boyState = 2;
			if (girl.gameId.length() > 0)
				girlState = 2;

			// sort by name secondly
			if (boyState == girlState) {
				if (boy.steamName != null && girl.steamName != null) {
					return boy.steamName.compareToIgnoreCase(girl.steamName);
				} else {
					return 0;
				}
			}

			return girlState - boyState;
		}
	}

	private void setAdVisibility(int visibility) {
		if (adView != null) {
			adView.setVisibility(visibility);
		}
	}
}
