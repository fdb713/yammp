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

import java.util.ArrayList;

import org.yammp.Constants;
import org.yammp.R;
import org.yammp.YAMMPApplication;
import org.yammp.util.MediaUtils;
import org.yammp.util.PreferencesEditor;
import org.yammp.util.ServiceInterface;
import org.yammp.util.ServiceInterface.MediaStateListener;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class MusicBrowserFragment extends SherlockFragment implements Constants,
		OnPageChangeListener, MediaStateListener {

	private ViewPager mViewPager;
	private TabsAdapter mAdapter;
	private PreferencesEditor mPrefs;
	private ProgressBar mProgress;
	private ServiceInterface mInterface;
	private MediaUtils mUtils;

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

		inflater.inflate(R.menu.music_browser, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		mUtils = ((YAMMPApplication) getSherlockActivity().getApplication()).getMediaUtils();
		mInterface = ((YAMMPApplication) getSherlockActivity().getApplication())
				.getServiceInterface();
		setHasOptionsMenu(true);
		View view = inflater.inflate(R.layout.music_browser, container, false);
		mViewPager = (ViewPager) view.findViewById(R.id.pager);
		mViewPager.setOnPageChangeListener(this);
		mAdapter = new TabsAdapter(getFragmentManager());
		mPrefs = new PreferencesEditor(getActivity());
		mProgress = (ProgressBar) view.findViewById(R.id.progress);

		mAdapter.addFragment(new ArtistFragment(), getString(R.string.artists).toUpperCase());
		mAdapter.addFragment(new AlbumFragment(), getString(R.string.albums).toUpperCase());
		mAdapter.addFragment(new TrackFragment(), getString(R.string.tracks).toUpperCase());
		mAdapter.addFragment(new PlaylistFragment(), getString(R.string.playlists).toUpperCase());
		mAdapter.addFragment(new GenreFragment(), getString(R.string.genres).toUpperCase());

		new AdapterTask().execute();

		return view;
	}

	@Override
	public void onFavoriteStateChanged() {

	}

	@Override
	public void onMetaChanged() {

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent intent;
		switch (item.getItemId()) {
			case PLAY_PAUSE:
				mInterface.togglePause();
				break;
			case NEXT:
				mInterface.next();
				break;
			case SHUFFLE_ALL:
				mUtils.shuffleAll();
				break;
			case SETTINGS:
				intent = new Intent(INTENT_MUSIC_SETTINGS);
				startActivity(intent);
				break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {

	}

	@Override
	public void onPageScrollStateChanged(int state) {

	}

	@Override
	public void onPageSelected(int position) {
		mPrefs.setIntState(STATE_KEY_CURRENTTAB, position);

	}

	@Override
	public void onPlayStateChanged() {
		getSherlockActivity().invalidateOptionsMenu();
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		MenuItem item = menu.findItem(PLAY_PAUSE);
		if (item != null) {
			item.setIcon(mInterface.isPlaying() ? R.drawable.ic_action_media_pause
					: R.drawable.ic_action_media_play);
		}

		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onQueueChanged() {

	}

	@Override
	public void onRepeatModeChanged() {

	}

	@Override
	public void onShuffleModeChanged() {

	}

	@Override
	public void onStart() {
		super.onStart();
		mInterface.addMediaStateListener(this);
	}

	@Override
	public void onStop() {
		mInterface.removeMediaStateListener(this);
		super.onStop();
	}

	private class AdapterTask extends AsyncTask<Void, Void, Void> {

		@Override
		public Void doInBackground(Void... params) {
			return null;
		}

		@Override
		public void onPostExecute(Void result) {
			mProgress.findViewById(R.id.progress).setVisibility(View.INVISIBLE);
			mViewPager.findViewById(R.id.pager).setVisibility(View.VISIBLE);
			mViewPager.setAdapter(mAdapter);
			int currenttab = mPrefs.getIntState(STATE_KEY_CURRENTTAB, 0);
			mViewPager.setCurrentItem(currenttab);
		}

		@Override
		public void onPreExecute() {
			mViewPager.setAdapter(null);
			mProgress.setVisibility(View.VISIBLE);
			mViewPager.setVisibility(View.INVISIBLE);

		}
	}

	private class TabsAdapter extends FragmentStatePagerAdapter {

		private ArrayList<Fragment> mFragments = new ArrayList<Fragment>();
		private ArrayList<String> mTitles = new ArrayList<String>();

		public TabsAdapter(FragmentManager manager) {
			super(manager);
		}

		public void addFragment(Fragment fragment, String name) {
			mFragments.add(fragment);
			mTitles.add(name);
			notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			return mFragments.size();
		}

		@Override
		public Fragment getItem(int position) {
			return mFragments.get(position);
		}

	}
}
