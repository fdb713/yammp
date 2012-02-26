/*
 *  YAMMP - Yet Another Multi Media Player for android
 *  Copyright (C) 2011-2012  Mariotaku Lee <mariotaku.lee@gmail.com>
 *
 *  This file is part of YAMMP.
 *
 *  YAMMP is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  YAMMP is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with YAMMP.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.yammp.app;

import org.yammp.Constants;
import org.yammp.R;
import org.yammp.util.MusicUtils;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class AlbumFragment extends Fragment implements Constants, ListView.OnScrollListener,
		OnItemClickListener, LoaderCallbacks<Cursor> {

	private AlbumsAdapter mAdapter;
	private GridView mGridView;
	private Cursor mCursor;
	private int mSelectedPosition;
	private long mSelectedId;
	private String mCurrentAlbumName, mCurrentArtistNameForAlbum;
	private int mIdIdx, mAlbumIdx, mArtistIdx, mArtIdx;
	private boolean mBusy = false;
	private int mFirstVisible, mLastVisible;

	public AlbumFragment() {

	}

	public AlbumFragment(Bundle args) {
		setArguments(args);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// We have a menu item to show in action bar.
		setHasOptionsMenu(true);

		mAdapter = new AlbumsAdapter(getActivity(), R.layout.album_grid_item, null,
				new String[] {}, new int[] {}, 0);

		View fragmentView = getView();
		mGridView = (GridView) fragmentView.findViewById(R.id.album_gridview);
		mGridView.setAdapter(mAdapter);
		mGridView.setOnItemClickListener(this);
		mGridView.setOnScrollListener(this);
		mGridView.setOnCreateContextMenuListener(this);
		mGridView.setDrawingCacheEnabled(true);
		mGridView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_AUTO);

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.albums_browser, container, false);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putAll(getArguments() != null ? getArguments() : new Bundle());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onStart() {
		super.onStart();

		IntentFilter filter = new IntentFilter();
		filter.addAction(BROADCAST_META_CHANGED);
		filter.addAction(BROADCAST_QUEUE_CHANGED);
		getActivity().registerReceiver(mMediaStatusReceiver, filter);
	}

	@Override
	public void onStop() {
		getActivity().unregisterReceiver(mMediaStatusReceiver);
		super.onStop();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		String[] cols = new String[] { Audio.Albums._ID, Audio.Albums.ALBUM, Audio.Albums.ARTIST,
				Audio.Albums.ALBUM_ART };
		Uri uri = Audio.Albums.EXTERNAL_CONTENT_URI;
		return new CursorLoader(getActivity(), uri, cols, null, null,
				Audio.Albums.DEFAULT_SORT_ORDER);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

		if (data == null) {
			getActivity().finish();
			return;
		}

		mCursor = data;

		mIdIdx = data.getColumnIndexOrThrow(Audio.Albums._ID);
		mAlbumIdx = data.getColumnIndexOrThrow(Audio.Albums.ALBUM);
		mArtistIdx = data.getColumnIndexOrThrow(Audio.Albums.ARTIST);
		mArtIdx = data.getColumnIndexOrThrow(Audio.Albums.ALBUM_ART);

		mAdapter.changeCursor(data);
		mFirstVisible = mGridView.getFirstVisiblePosition();
		mLastVisible = mGridView.getLastVisiblePosition();

	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {

		mAdapter.changeCursor(null);
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
		showDetails(position, id);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo info) {

		if (mCursor == null) return;

		getActivity().getMenuInflater().inflate(R.menu.music_browser_item, menu);

		AdapterContextMenuInfo adapterinfo = (AdapterContextMenuInfo) info;
		mSelectedPosition = adapterinfo.position;
		mCursor.moveToPosition(mSelectedPosition);
		try {
			mSelectedId = mCursor.getLong(mIdIdx);
		} catch (IllegalArgumentException ex) {
			mSelectedId = adapterinfo.id;
		}

		mCurrentArtistNameForAlbum = mCursor.getString(mArtistIdx);

		mCurrentAlbumName = mCursor.getString(mAlbumIdx);

		menu.setHeaderTitle(mCurrentAlbumName);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		if (mCursor == null) return false;

		Intent intent;

		switch (item.getItemId()) {
			case PLAY_SELECTION:
				int position = mSelectedPosition;
				long[] list = MusicUtils.getSongListForAlbum(getActivity(), mSelectedId);
				MusicUtils.playAll(getActivity(), list, position);
				return true;
			case DELETE_ITEMS:
				intent = new Intent(INTENT_DELETE_ITEMS);
				Bundle bundle = new Bundle();
				bundle.putString(
						INTENT_KEY_PATH,
						Uri.withAppendedPath(Audio.Albums.EXTERNAL_CONTENT_URI,
								Uri.encode(String.valueOf(mSelectedId))).toString());
				intent.putExtras(bundle);
				startActivity(intent);
				return true;
			case SEARCH:
				doSearch();
				return true;
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int state) {
		switch (state) {
			case SCROLL_STATE_IDLE:
				mBusy = false;
				break;
			case SCROLL_STATE_TOUCH_SCROLL:
				mBusy = true;
				break;
			case SCROLL_STATE_FLING:
				mBusy = true;
				break;
		}

	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
			int totalItemCount) {
		mFirstVisible = firstVisibleItem;
		mLastVisible = firstVisibleItem + visibleItemCount;
	}

	private void doSearch() {

		CharSequence title = null;
		String query = null;

		Intent i = new Intent();
		i.setAction(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		title = mCurrentAlbumName;
		if (MediaStore.UNKNOWN_STRING.equals(mCurrentArtistNameForAlbum)) {
			query = mCurrentAlbumName;
		} else {
			query = mCurrentArtistNameForAlbum + " " + mCurrentAlbumName;
			i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, mCurrentArtistNameForAlbum);
		}
		if (MediaStore.UNKNOWN_STRING.equals(mCurrentAlbumName)) {
			i.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, mCurrentAlbumName);
		}
		i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "audio/*");
		title = getString(R.string.mediasearch, title);
		i.putExtra(SearchManager.QUERY, query);

		startActivity(Intent.createChooser(i, title));
	}

	private void showDetails(int index, long id) {

		Bundle bundle = new Bundle();
		bundle.putString(INTENT_KEY_TYPE, MediaStore.Audio.Albums.CONTENT_TYPE);
		bundle.putLong(Audio.Albums._ID, id);

		View detailsFrame = getActivity().findViewById(R.id.frame_details);
		boolean mDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE
				&& getResources().getBoolean(R.bool.dual_pane);

		if (mDualPane) {
			mGridView.setSelection(index);

			TrackFragment fragment = new TrackFragment();
			fragment.setArguments(bundle);

			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.replace(R.id.frame_details, fragment);
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			ft.commit();

		} else {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setClass(getActivity(), TrackBrowserActivity.class);
			intent.putExtras(bundle);
			startActivity(intent);
		}
	}

	private BroadcastReceiver mMediaStatusReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			mGridView.invalidateViews();
		}

	};

	private class AlbumsAdapter extends SimpleCursorAdapter {

		private class ViewHolder {

			TextView album_name;
			TextView artist_name;
			ImageView album_art;

			public ViewHolder(View view) {
				album_name = (TextView) view.findViewById(R.id.album_name);
				artist_name = (TextView) view.findViewById(R.id.artist_name);
				album_art = (ImageView) view.findViewById(R.id.album_art);
			}

		}

		private AlbumsAdapter(Context context, int layout, Cursor cursor, String[] from, int[] to,
				int flags) {
			super(context, layout, cursor, from, to, flags);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {

			View view = super.newView(context, cursor, parent);
			ViewHolder viewholder = new ViewHolder(view);
			view.setTag(viewholder);
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {

			ViewHolder viewholder = (ViewHolder) view.getTag();

			String album_name = cursor.getString(mAlbumIdx);
			if (album_name == null || MediaStore.UNKNOWN_STRING.equals(album_name)) {
				viewholder.album_name.setText(R.string.unknown_album);
			} else {
				viewholder.album_name.setText(album_name);
			}

			String artist_name = cursor.getString(mArtistIdx);
			if (artist_name == null || MediaStore.UNKNOWN_STRING.equals(artist_name)) {
				viewholder.artist_name.setText(R.string.unknown_artist);
			} else {
				viewholder.artist_name.setText(artist_name);
			}

			// We don't actually need the path to the thumbnail file,
			// we just use it to see if there is album art or not
			long aid = cursor.getLong(mIdIdx);
			int width = view.getWidth();
			int height = view.getHeight();

			Log.i("Debug", "usedMemory: " + Debug.getNativeHeapSize() / 1024);

			// if (viewholder != null && viewholder.album_art != null) {
			//
			// String art = cursor.getString(mArtIdx);
			//
			// if (art == null || art.length() == 0) {
			// viewholder.album_art.setImageResource(R.drawable.ic_mp_albumart_unknown);
			// } else {
			// Drawable d = MusicUtils.getCachedArtwork(context, aid, width,
			// height);
			// viewholder.album_art.setImageDrawable(d);
			// d = null;
			// }

			// if (cursor.getPosition() >= mFirstVisible - 1
			// && cursor.getPosition() <= mLastVisible + 1) {
			// Bitmap result =
			// MusicUtils.getCachedArtworkBitmap(getActivity(), aid,
			// width, height);
			// Bitmap result = MusicUtils.getArtworkQuick(getActivity(),
			// aid, width, height);
			// if (result != null) {
			// viewholder.album_art.setImageBitmap(result);
			// } else {
			// viewholder.album_art.setImageResource(R.drawable.ic_mp_albumart_unknown);
			// }
			// } else {
			// viewholder.album_art.setImageResource(R.drawable.ic_mp_albumart_unknown);
			// }
			// Log.d("debug", "mBusy = " + mBusy + ", position = " +
			// cursor.getPosition());
			// if (mBusy) {
			// viewholder.album_art.setVisibility(View.INVISIBLE);
			// } else {
			// viewholder.album_art.setVisibility(View.VISIBLE);
			//
			// viewholder.album_art.setImageResource(R.drawable.ic_mp_albumart_unknown);
			// }

			// }

			long currentalbumid = MusicUtils.getCurrentAlbumId();
			if (currentalbumid == aid) {
				viewholder.album_name.setCompoundDrawablesWithIntrinsicBounds(0, 0,
						R.drawable.ic_indicator_nowplaying_small, 0);
			} else {
				viewholder.album_name.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}

		}

	}

}
