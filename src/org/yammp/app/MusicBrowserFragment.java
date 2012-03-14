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

import java.util.ArrayList;

import org.yammp.Constants;
import org.yammp.R;
import org.yammp.util.PreferencesEditor;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;
import com.viewpagerindicator.TabPageIndicator;
import com.viewpagerindicator.TitleProvider;
import com.viewpagerindicator.VerticalTabPageIndicator;

public class MusicBrowserFragment extends SherlockFragment implements Constants,
		OnPageChangeListener {

	private ViewPager mViewPager;
	private TabPageIndicator mIndicator;
	private VerticalTabPageIndicator mVerticalIndicator;

	private TabsAdapter mAdapter;

	private PreferencesEditor mPrefs;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		setHasOptionsMenu(true);

		mPrefs = new PreferencesEditor(getActivity());
		mAdapter = new TabsAdapter(getFragmentManager());
		mViewPager = (ViewPager) getView().findViewById(R.id.pager);
		mIndicator = (TabPageIndicator) getView().findViewById(R.id.indicator);
		mVerticalIndicator = (VerticalTabPageIndicator) getView().findViewById(
				R.id.vertical_indicator);
		configureTabs();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.music_browser, container, false);
		return view;
	}

	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);

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

	private void configureTabs() {

		mAdapter.addFragment(new ArtistFragment(), getString(R.string.artists).toUpperCase());
		mAdapter.addFragment(new AlbumFragment(), getString(R.string.albums).toUpperCase());
		mAdapter.addFragment(new TrackFragment(), getString(R.string.tracks).toUpperCase());
		mAdapter.addFragment(new PlaylistFragment(), getString(R.string.playlists)
				.toUpperCase());
		mAdapter.addFragment(new GenreFragment(), getString(R.string.genres).toUpperCase());

		mViewPager.setAdapter(mAdapter);
		int currenttab = mPrefs.getIntState(STATE_KEY_CURRENTTAB, 0);
		if (mIndicator != null) {
			mIndicator.setViewPager(mViewPager, currenttab);
			mIndicator.setOnPageChangeListener(this);
		}
		if (mVerticalIndicator != null) {
			mVerticalIndicator.setViewPager(mViewPager, currenttab);
			mVerticalIndicator.setOnPageChangeListener(this);
		}
	}

	private class TabsAdapter extends FragmentStatePagerAdapter implements TitleProvider {

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

		@Override
		public String getTitle(int position) {
			return mTitles.get(position);
		}

	}
}
