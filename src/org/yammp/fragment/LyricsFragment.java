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
import org.yammp.util.ServiceInterface;
import org.yammp.util.ServiceInterface.LyricsStateListener;
import org.yammp.widget.TextScrollView;
import org.yammp.widget.TextScrollView.OnLineSelectedListener;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.actionbarsherlock.app.SherlockFragment;

public class LyricsFragment extends SherlockFragment implements Constants, OnLineSelectedListener,
		OnClickListener, OnBackStackChangedListener, LyricsStateListener {

	private ServiceInterface mInterface = null;
	private final static String SEARCH_LYRICS = "search_lyrics";
	// for lyrics displaying
	private TextScrollView mLyricsScrollView;
	private Button mLyricsEmptyView;
	private LinearLayout mLyricsSearchLayout;
	private boolean mIntentDeRegistered = false;
	private LyricsSearchFragment mSearchFragment;
	private boolean mSearchShowed = false;

	private BroadcastReceiver mScreenTimeoutListener = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
				if (mIntentDeRegistered) {
					mIntentDeRegistered = false;
				}
				loadLyricsToView();
				scrollLyrics(true);
			} else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
				if (!mIntentDeRegistered) {
					mIntentDeRegistered = true;
				}
			}
		}
	};

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		mInterface = ((YAMMPApplication) getSherlockActivity().getApplication())
				.getServiceInterface();
		super.onActivityCreated(savedInstanceState);
		mSearchFragment = new LyricsSearchFragment();
		getFragmentManager().addOnBackStackChangedListener(this);
		View view = getView();
		mLyricsScrollView = (TextScrollView) view.findViewById(R.id.lyrics_scroll);
		mLyricsScrollView.setContentGravity(Gravity.CENTER_HORIZONTAL);
		mLyricsScrollView.setLineSelectedListener(this);
		mLyricsEmptyView = (Button) view.findViewById(R.id.lyrics_empty);
		mLyricsEmptyView.setOnClickListener(this);
		mLyricsSearchLayout = (LinearLayout) view.findViewById(R.id.search_lyrics);
		mInterface.addLyricsStateListener(this);
	}

	@Override
	public void onBackStackChanged() {
		if (mSearchFragment != null) {
			boolean search_showed = mSearchFragment.isAdded();
			mSearchShowed = search_showed;
			mLyricsSearchLayout.setVisibility(search_showed ? View.VISIBLE : View.GONE);
			if (mInterface != null) {
				boolean lyrics_status_ok = mInterface.getLyricsStatus() == LYRICS_STATUS_OK;
				mLyricsEmptyView.setVisibility(search_showed || lyrics_status_ok ? View.GONE
						: View.VISIBLE);
				mLyricsScrollView.setVisibility(search_showed || !lyrics_status_ok ? View.GONE
						: View.VISIBLE);
			}

		}
	}

	@Override
	public void onClick(View v) {
		searchLyrics();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.lyrics_view, container, false);
	}

	@Override
	public void onLineSelected(int id) {
		if (mInterface != null) {
			mInterface.seek(mInterface.getPositionByLyricsId(id));
		}

	}

	@Override
	public void onLyricsRefreshed() {
		scrollLyrics(false);

	}

	@Override
	public void onNewLyricsLoaded() {
		loadLyricsToView();

	}

	@Override
	public void onStart() {
		super.onStart();

		try {
			float mWindowAnimation = Settings.System.getFloat(getActivity().getContentResolver(),
					Settings.System.WINDOW_ANIMATION_SCALE);
			mLyricsScrollView.setSmoothScrollingEnabled(mWindowAnimation > 0.0);

		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}

		IntentFilter screenstatusfilter = new IntentFilter();
		screenstatusfilter.addAction(Intent.ACTION_SCREEN_ON);
		screenstatusfilter.addAction(Intent.ACTION_SCREEN_OFF);
		getActivity().registerReceiver(mScreenTimeoutListener, screenstatusfilter);
	}

	@Override
	public void onStop() {

		getActivity().unregisterReceiver(mScreenTimeoutListener);

		super.onStop();
	}

	// TODO lyrics load animation
	private void loadLyricsToView() {

		if (mLyricsScrollView == null || mInterface == null) return;

		mLyricsScrollView.setTextContent(mInterface.getLyrics());
		if (!mSearchShowed) {
			if (mInterface.getLyricsStatus() == LYRICS_STATUS_OK) {
				mLyricsScrollView.setVisibility(View.VISIBLE);
				mLyricsEmptyView.setVisibility(View.GONE);
			} else {
				mLyricsScrollView.setVisibility(View.GONE);
				mLyricsEmptyView.setVisibility(View.VISIBLE);
			}
		}
	}

	private void scrollLyrics(boolean force) {
		if (mInterface == null) return;
		if (mLyricsScrollView == null) return;
		mLyricsScrollView.setCurrentLine(mInterface.getCurrentLyricsId(), force);
	}

	private void searchLyrics() {

		if (mInterface == null) return;
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		ft.setTransitionStyle(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
		Bundle args = new Bundle();
		args.putString(INTENT_KEY_TRACK, mInterface.getTrackName());
		args.putString(INTENT_KEY_ARTIST, mInterface.getArtistName());
		String media_path = mInterface.getMediaPath();
		String lyrics_path = media_path.substring(0, media_path.lastIndexOf(".")) + ".lrc";
		args.putString(INTENT_KEY_PATH, lyrics_path);
		mSearchFragment.setArguments(args);
		ft.replace(R.id.search_lyrics, mSearchFragment);
		ft.addToBackStack(SEARCH_LYRICS);
		ft.commit();
	}

}
