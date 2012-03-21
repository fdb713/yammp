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

import java.io.File;

import org.yammp.Constants;
import org.yammp.R;
import org.yammp.YAMMPApplication;
import org.yammp.dialog.DeleteDialogFragment;
import org.yammp.util.LazyImageLoader;
import org.yammp.util.MediaUtils;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

public class AlbumFragment extends SherlockFragment implements Constants, OnItemClickListener,
		OnItemSelectedListener, OnScrollListener, LoaderCallbacks<Cursor> {

	private AlbumsAdapter mAdapter;

	private GridView mGridView;
	private Cursor mCursor;
	private int mSelectedPosition;
	private long mSelectedId;
	private String mCurrentAlbumName, mCurrentArtistNameForAlbum;
	private int mIdIdx, mAlbumIdx, mArtistIdx, mArtIdx;
	private LazyImageLoader mImageLoader;

	private BroadcastReceiver mMediaStatusReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			mGridView.invalidateViews();
		}

	};

	MediaUtils mUtils;

	private int mScrollState = OnScrollListener.SCROLL_STATE_IDLE;

	private boolean mScrollStopped = true;

	private int mFirstVisible = 0;

	public AlbumFragment() {

	}

	public AlbumFragment(Bundle args) {
		setArguments(args);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mUtils = ((YAMMPApplication) getSherlockActivity().getApplication()).getMediaUtils();
		// We have a menu item to show in action bar.
		setHasOptionsMenu(true);

		mImageLoader = ((YAMMPApplication) getSherlockActivity().getApplication())
				.getLazyImageLoader();

		mAdapter = new AlbumsAdapter(getSherlockActivity(), R.layout.album_grid_item, null,
				new String[] {}, new int[] {}, 0);

		View fragmentView = getView();
		mGridView = (GridView) fragmentView.findViewById(android.R.id.list);
		mGridView.setAdapter(mAdapter);
		mGridView.setOnItemClickListener(this);
		mGridView.setOnItemSelectedListener(this);
		mGridView.setOnCreateContextMenuListener(this);
		mGridView.setOnScrollListener(this);

		registerForContextMenu(mGridView);

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		if (item.getGroupId() == hashCode()) {
			if (mCursor == null) return true;
			switch (item.getItemId()) {
				case PLAY_SELECTION:
					int position = mSelectedPosition;
					long[] list = mUtils.getSongListForAlbum(mSelectedId);
					mUtils.playAll(list, position);
					return true;
				case DELETE_ITEMS:
					DeleteDialogFragment
							.getInstance(false, mSelectedId, DeleteDialogFragment.ALBUM).show(
									getFragmentManager(), "dialog");
					return true;
				case DELETE_LYRICS:
					DeleteDialogFragment
							.getInstance(false, mSelectedId, DeleteDialogFragment.ALBUM).show(
									getFragmentManager(), "dialog");
					return true;
				case SEARCH:
					doSearch();
					return true;
			}
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo info) {

		if (mCursor == null) return;

		menu.add(hashCode(), PLAY_SELECTION, 0, R.string.play_selection);
		menu.add(hashCode(), DELETE_ITEMS, 0, R.string.delete_music);
		menu.add(hashCode(), DELETE_LYRICS, 0, R.string.delete_lyrics);

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
		if (mCurrentAlbumName != null && !MediaStore.UNKNOWN_STRING.equals(mCurrentAlbumName)) {
			menu.add(hashCode(), SEARCH, 0, R.string.play_selection);
			menu.setHeaderTitle(mCurrentAlbumName);
		} else {
			menu.setHeaderTitle(R.string.unknown_album);
		}

	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		String[] cols = new String[] { Audio.Albums._ID, Audio.Albums.ALBUM, Audio.Albums.ARTIST,
				Audio.Albums.ALBUM_ART };
		Uri uri = Audio.Albums.EXTERNAL_CONTENT_URI;
		return new CursorLoader(getSherlockActivity(), uri, cols, null, null,
				Audio.Albums.DEFAULT_SORT_ORDER);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.albums_browser, container, false);
	}

	@Override
	public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
		showDetails(position, id);
	}

	@Override
	public void onItemSelected(AdapterView<?> adapter, View view, int position, long id) {
		((MusicBrowserActivity) getSherlockActivity()).setBackground(0, id);

	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {

		mAdapter.changeCursor(null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

		if (data == null) {
			getSherlockActivity().finish();
			return;
		}

		mCursor = data;

		mIdIdx = data.getColumnIndexOrThrow(Audio.Albums._ID);
		mAlbumIdx = data.getColumnIndexOrThrow(Audio.Albums.ALBUM);
		mArtistIdx = data.getColumnIndexOrThrow(Audio.Albums.ARTIST);
		mArtIdx = data.getColumnIndexOrThrow(Audio.Albums.ALBUM_ART);

		mAdapter.changeCursor(data);

	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putAll(getArguments() != null ? getArguments() : new Bundle());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
			int totalItemCount) {
		if (mScrollState == OnScrollListener.SCROLL_STATE_FLING) {
			long item_id = view.getItemIdAtPosition(firstVisibleItem);
			((YAMMPActivity) getSherlockActivity()).setBackground(0, item_id);
		}

	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		mScrollState = scrollState;
	}

	@Override
	public void onStart() {
		super.onStart();

		IntentFilter filter = new IntentFilter();
		filter.addAction(BROADCAST_META_CHANGED);
		filter.addAction(BROADCAST_QUEUE_CHANGED);
		getSherlockActivity().registerReceiver(mMediaStatusReceiver, filter);
	}

	@Override
	public void onStop() {
		getSherlockActivity().unregisterReceiver(mMediaStatusReceiver);
		super.onStop();
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

		View detailsFrame = getSherlockActivity().findViewById(R.id.frame_details);
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
			intent.setClass(getSherlockActivity(), TrackBrowserActivity.class);
			intent.putExtras(bundle);
			startActivity(intent);
		}
	}

	private class AlbumsAdapter extends SimpleCursorAdapter {

		private AlbumsAdapter(Context context, int layout, Cursor cursor, String[] from, int[] to,
				int flags) {
			super(context, layout, cursor, from, to, flags);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {

			ViewHolder viewholder = (ViewHolder) view.getTag();

			if (viewholder == null) return;

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

			long currentalbumid = mUtils.getCurrentAlbumId();
			if (currentalbumid == aid) {
				viewholder.album_name.setCompoundDrawablesWithIntrinsicBounds(0, 0,
						R.drawable.ic_indicator_nowplaying_small, 0);
			} else {
				viewholder.album_name.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}

			String art = cursor.getString(mArtIdx);

			if (art != null && art.toString().length() > 0) {
				mImageLoader.displayImage(new File(art), viewholder.album_art);
			} else {
				viewholder.album_art.setImageResource(R.drawable.ic_mp_albumart_unknown);
			}

		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {

			View view = super.newView(context, cursor, parent);
			ViewHolder viewholder = new ViewHolder(view);
			view.setTag(viewholder);
			return view;
		}

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

	}

}
