package org.yammp.fragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Video;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;

import com.actionbarsherlock.app.SherlockListFragment;

public class VideoFragment extends SherlockListFragment implements LoaderCallbacks<Cursor> {

	private SimpleCursorAdapter mAdapter;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// We have a menu item to show in action bar.
		setHasOptionsMenu(true);

		mAdapter = new SimpleCursorAdapter(getSherlockActivity(),
				android.R.layout.simple_list_item_1, null, new String[] { Video.Media.TITLE },
				new int[] { android.R.id.text1 }, 0);

		setListAdapter(mAdapter);

		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int loader, Bundle args) {
		String[] cols = new String[] { Video.Media._ID, Video.Media.TITLE, Video.Media.DATA,
				Video.Media.MIME_TYPE, Video.Media.ARTIST };
		Uri uri = Video.Media.EXTERNAL_CONTENT_URI;
		String sort_order = Video.Media.TITLE + " COLLATE UNICODE";
		String where = Video.Media.TITLE + " != ''";
		return new CursorLoader(getSherlockActivity(), uri, cols, where, null, sort_order);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.changeCursor(null);

	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		mAdapter.changeCursor(cursor);

	}

}
