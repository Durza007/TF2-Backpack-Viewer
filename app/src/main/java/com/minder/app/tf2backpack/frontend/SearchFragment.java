package com.minder.app.tf2backpack.frontend;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.minder.app.tf2backpack.App;
import com.minder.app.tf2backpack.PlayerAdapter;
import com.minder.app.tf2backpack.R;
import com.minder.app.tf2backpack.SteamUser;
import com.minder.app.tf2backpack.backend.AsyncTaskListener;
import com.minder.app.tf2backpack.backend.DataManager;
import com.minder.app.tf2backpack.backend.ProgressUpdate;
import com.minder.app.tf2backpack.backend.DataManager.Request;
import com.minder.app.tf2backpack.frontend.PlayerListFragment.byPersonaState;
import com.minder.app.tf2backpack.frontend.PlayerListFragment.byPlayerName;
import com.minder.app.tf2backpack.frontend.PlayerListFragment.byWrenchNumber;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class SearchFragment extends Fragment implements OnScrollListener {
	private WeakReference<OnPlayerSelectedListener> listener;
	private final boolean isAboveGingerBread;

	private boolean ready = false;
	private boolean loadingMore = false;
	private boolean nothingMoreToLoad = false;
	private boolean loadAvatars = false;

	private final int CONTEXTMENU_VIEW_BACKPACK = 0;
	private final int CONTEXTMENU_VIEW_STEAMPAGE = 1;

	public int searchPage = 1;
	public String searchQuery;

	private View progressContainer;
	private ListView playerList;
	private PlayerAdapter adapter;
	private View footerView;
	private View noResultFooterView;

	private List<SteamUser> steamUserList;
	private Request currentRequest;

	private OnItemClickListener clickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			if (steamUserList.size() <= position)
				return;
			final SteamUser user = (SteamUser) adapter.getItem(position);

			view.setPressed(true);
			notifyListener(user, position);
		}
	};

	public static SearchFragment createInstance(String searchTerm) {
		final SearchFragment fragment = new SearchFragment();
		fragment.searchQuery = searchTerm;

		return fragment;
	}

	public void newSearchQuery(String searchQuery) {
		this.searchQuery = searchQuery;
		this.searchPage = 1;
		steamUserList.clear();
		adapter.clearPlayers();
		adapter.notifyDataSetChanged();
		
		setLoadingFooterVisible(false);
		setNoResultFooterVisible(false);

		progressContainer.setVisibility(View.VISIBLE);
		playerList.setVisibility(View.GONE);

		currentRequest = App.getDataManager().requestSteamUserSearch(
				getUserSearchListener, searchQuery, searchPage);
	}

	public SearchFragment() {
		isAboveGingerBread = android.os.Build.VERSION.SDK_INT > 10;
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

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.list_content, container, false);

		progressContainer = view.findViewById(R.id.progressContainer);

		playerList = view.findViewById(android.R.id.list);
		final View root = view.findViewById(R.id.frameLayoutRoot);
		root.setBackgroundResource(R.color.bg_color);

		// Set up our adapter
		// TODO maybe re-think if this needs an activity?
		adapter = new PlayerAdapter(getActivity());
		adapter.setShowAvatars(loadAvatars);
		footerView = inflater.inflate(R.layout.loading_footer, null);
		noResultFooterView = inflater.inflate(R.layout.noresult_footer, null);
		playerList.addFooterView(footerView, null, false);
		playerList.addFooterView(noResultFooterView, null, false);
		playerList.setAdapter(adapter);
		// playerList.removeFooterView(footerView);
		setLoadingFooterVisible(false);
		setNoResultFooterVisible(false);

		playerList.setBackgroundResource(R.color.bg_color);
		playerList.setCacheColorHint(this.getResources().getColor(
				R.color.bg_color));
		// adapter.setComparator(new byPersonaState());

		playerList.setOnItemClickListener(clickListener);
		playerList.setOnScrollListener(this);

		if (steamUserList != null) {
			adapter.setPlayers(steamUserList);
		} else {
			progressContainer.setVisibility(View.VISIBLE);
			playerList.setVisibility(View.GONE);
			steamUserList = new LinkedList<SteamUser>();
			currentRequest = App.getDataManager().requestSteamUserSearch(
					getUserSearchListener, searchQuery, searchPage);
		}

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

	private void setLoadingFooterVisible(boolean show) {
		if (show)
			footerView.setVisibility(View.VISIBLE);
		else
			footerView.setVisibility(View.GONE);
	}

	private void setNoResultFooterVisible(boolean show) {
		if (show)
			noResultFooterView.setVisibility(View.VISIBLE);
		else
			noResultFooterView.setVisibility(View.GONE);
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
		getActivity().getActionBar().setTitle(R.string.app_name);
		Log.d("PlayerList", "stop");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// not interested in this event
	}

	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		boolean loadMore = firstVisibleItem + visibleItemCount >= totalItemCount;

		if (loadMore && ready && !loadingMore && !nothingMoreToLoad) {
			loadingMore = true;
			setLoadingFooterVisible(true);

			currentRequest = App.getDataManager().requestSteamUserSearch(
					getUserSearchListener, searchQuery, ++searchPage);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.friend_menu, menu);
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
			startActivity(new Intent(SearchFragment.this.getActivity(),
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

	public void setPlayerSelectedListener(OnPlayerSelectedListener listener) {
		this.listener = new WeakReference<OnPlayerSelectedListener>(listener);
	}

	private void notifyListener(SteamUser user, int index) {
		OnPlayerSelectedListener l = listener.get();
		if (l != null)
			l.onPlayerSelected(user, index);
	}

	private AsyncTaskListener getUserSearchListener = new AsyncTaskListener() {
		public void onPreExecute() {
		}

		public void onProgressUpdate(ProgressUpdate object) {
		}

		@SuppressWarnings("unchecked")
		public void onPostExecute(Request request) {
			final Object data = request.getData();
			if (data != null) {
				final DataManager.SearchUserResult result = (DataManager.SearchUserResult) data;

				getActivity().getActionBar().setTitle(getResources().getQuantityString(R.plurals.found_users, result.totalResults, result.totalResults));
				ArrayList<SteamUser> list = result.users;

				progressContainer.setVisibility(View.GONE);
				playerList.setVisibility(View.VISIBLE);

				nothingMoreToLoad = true;
				if (list.size() > 0) {
					for (SteamUser p : list) {
						if (adapter.addPlayerInfo(p)) {
							steamUserList.add(p);
							nothingMoreToLoad = false;
						}
					}
				} else {
					setLoadingFooterVisible(false);
					setNoResultFooterVisible(true);
				}

			}
			setLoadingFooterVisible(false);
			loadingMore = false;
			ready = true;
		}
	};
}
