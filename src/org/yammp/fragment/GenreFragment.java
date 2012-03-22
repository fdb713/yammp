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

package org.yammp.fragment;

import org.yammp.Constants;
import org.yammp.R;
import org.yammp.YAMMPApplication;
import org.yammp.app.TrackBrowserActivity;
import org.yammp.util.MediaUtils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Audio;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;

public class GenreFragment extends SherlockListFragment implements LoaderCallbacks<Cursor>,
		Constants {

	private GenresAdapter mAdapter;

	private int mNameIdx;

	private MediaUtils mUtils;

	public GenreFragment() {

	}

	public GenreFragment(Bundle args) {
		setArguments(args);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mUtils = ((YAMMPApplication) getSherlockActivity().getApplication()).getMediaUtils();
		setHasOptionsMenu(true);

		mAdapter = new GenresAdapter(getActivity(), null, false);

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		String[] cols = new String[] { Audio.Genres._ID, Audio.Genres.NAME };

		String where = mUtils.getBetterGenresWhereClause();

		Uri uri = Audio.Genres.EXTERNAL_CONTENT_URI;

		return new CursorLoader(getActivity(), uri, cols, where, null,
				Audio.Genres.DEFAULT_SORT_ORDER);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.playlists_browser, container, false);
		return view;
	}

	@Override
	public void onListItemClick(ListView listview, View view, int position, long id) {

		showDetails(position, id);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

		if (data == null) {
			getActivity().finish();
			return;
		}

		mNameIdx = data.getColumnIndexOrThrow(Audio.Genres.NAME);

		mAdapter.changeCursor(data);

		setListAdapter(mAdapter);

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putAll(getArguments() != null ? getArguments() : new Bundle());
		super.onSaveInstanceState(outState);
	}

	private void showDetails(int index, long id) {

		View detailsFrame = getActivity().findViewById(R.id.frame_details);
		boolean mDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;

		Bundle bundle = new Bundle();
		bundle.putString(INTENT_KEY_TYPE, Audio.Genres.CONTENT_TYPE);
		bundle.putLong(Audio.Genres._ID, id);

		if (mDualPane) {

			TrackFragment fragment = new TrackFragment();
			fragment.setArguments(bundle);

			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.replace(R.id.frame_details, fragment);
			ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
			ft.commit();

		} else {

			Intent intent = new Intent(getActivity(), TrackBrowserActivity.class);
			intent.putExtras(bundle);
			startActivity(intent);
		}
	}

	private class GenresAdapter extends CursorAdapter {

		private GenresAdapter(Context context, Cursor cursor, boolean autoRequery) {
			super(context, cursor, autoRequery);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {

			ViewHolder viewholder = (ViewHolder) view.getTag();

			String genre_name = cursor.getString(mNameIdx);
			viewholder.genre_name.setText(mUtils.parseGenreName(genre_name));

		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {

			View view = LayoutInflater.from(context).inflate(R.layout.playlist_list_item, null);
			ViewHolder viewholder = new ViewHolder(view);
			view.setTag(viewholder);
			return view;
		}

		private class ViewHolder {

			TextView genre_name;

			public ViewHolder(View view) {
				genre_name = (TextView) view.findViewById(R.id.playlist_name);
			}
		}

	}

}
