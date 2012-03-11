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
import org.yammp.IMusicPlaybackService;
import org.yammp.R;
import org.yammp.dialog.ScanningProgress;
import org.yammp.util.MusicUtils;
import org.yammp.util.PreferencesEditor;
import org.yammp.util.ServiceToken;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class MusicBrowserActivity extends SherlockFragmentActivity implements Constants,
		ServiceConnection, OnPageChangeListener {

	private ActionBar mActionBar;
	private ViewPager mViewPager;

	private TabsAdapter mTabsAdapter;

	private ServiceToken mToken;
	private IMusicPlaybackService mService;
	private PreferencesEditor mPrefs;
	private AsyncAlbumArtLoader mAlbumArtLoader;

	private BroadcastReceiver mMediaStatusReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (BROADCAST_META_CHANGED.equals(intent.getAction())
					|| BROADCAST_META_CHANGED.equals(intent.getAction())) {
				updateNowplaying();
				invalidateOptionsMenu();
			} else if (BROADCAST_PLAYSTATE_CHANGED.equals(intent.getAction())) {
				invalidateOptionsMenu();
			}

		}

	};

	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		mActionBar = getSupportActionBar();

		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		mPrefs = new PreferencesEditor(getApplicationContext());

		String mount_state = Environment.getExternalStorageState();

		if (!Environment.MEDIA_MOUNTED.equals(mount_state)
				&& !Environment.MEDIA_MOUNTED_READ_ONLY.equals(mount_state)) {
			startActivity(new Intent(this, ScanningProgress.class));
			finish();
		}

		configureActivity();
		configureTabs(icicle);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getSupportMenuInflater().inflate(R.menu.music_browser, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent intent;
		switch (item.getItemId()) {
			case PLAY_PAUSE:
				if (mService == null) return false;
				try {
					mService.togglePause();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				break;
			case NEXT:
				if (mService == null) return false;
				try {
					mService.next();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				break;
			case GOTO_PLAYBACK:
				intent = new Intent(INTENT_PLAYBACK_VIEWER);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
				break;
			case SHUFFLE_ALL:
				MusicUtils.shuffleAll(getApplicationContext());
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
	public void onPageScrollStateChanged(int arg0) {

	}

	@Override
	public void onPageSelected(int position) {
		mActionBar.setSelectedNavigationItem(position);
		mPrefs.setIntState(STATE_KEY_CURRENTTAB, position);

	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem item = menu.findItem(PLAY_PAUSE);
		try {
			if (item != null && mService != null)
				item.setIcon(mService.isPlaying() ? R.drawable.ic_action_media_pause
						: R.drawable.ic_action_media_play);
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putAll(getIntent().getExtras() != null ? getIntent().getExtras() : new Bundle());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder obj) {
		mService = IMusicPlaybackService.Stub.asInterface(obj);
		updateNowplaying();
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		mService = null;
		finish();
	}

	@Override
	public void onStart() {
		super.onStart();
		mToken = MusicUtils.bindToService(this, this);
		IntentFilter filter = new IntentFilter();
		filter.addAction(BROADCAST_META_CHANGED);
		filter.addAction(BROADCAST_QUEUE_CHANGED);
		filter.addAction(BROADCAST_PLAYSTATE_CHANGED);
		registerReceiver(mMediaStatusReceiver, filter);

	}

	@Override
	public void onStop() {

		unregisterReceiver(mMediaStatusReceiver);
		MusicUtils.unbindFromService(mToken);
		mService = null;
		super.onStop();
	}

	private void configureActivity() {

		setContentView(R.layout.music_browser);

		mTabsAdapter = new TabsAdapter(getSupportFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.pager);

	}

	private void configureTabs(Bundle args) {

		mTabsAdapter.addFragment(new ArtistFragment(), getString(R.string.artists).toUpperCase());
		mTabsAdapter.addFragment(new AlbumFragment(), getString(R.string.albums).toUpperCase());
		mTabsAdapter.addFragment(new TrackFragment(), getString(R.string.tracks).toUpperCase());
		mTabsAdapter.addFragment(new PlaylistFragment(), getString(R.string.playlists)
				.toUpperCase());
		mTabsAdapter.addFragment(new GenreFragment(), getString(R.string.genres).toUpperCase());

		mViewPager.setAdapter(mTabsAdapter);
		mViewPager.setOnPageChangeListener(this);
		int currenttab = mPrefs.getIntState(STATE_KEY_CURRENTTAB, 0);
		mActionBar.setSelectedNavigationItem(currenttab);
	}

	private void updateNowplaying() {
		if (mService == null) return;
		try {
			if (mService.getAudioId() > -1 || mService.getPath() != null) {
				setTitle(mService.getTrackName());
				if (mService.getArtistName() != null
						&& !MediaStore.UNKNOWN_STRING.equals(mService.getArtistName())) {
					mActionBar.setSubtitle(mService.getArtistName());
				} else if (mService.getAlbumName() != null
						&& !MediaStore.UNKNOWN_STRING.equals(mService.getAlbumName())) {
					mActionBar.setSubtitle(mService.getAlbumName());
				} else {
					mActionBar.setSubtitle(R.string.unknown_artist);
				}
			} else {
				setTitle(R.string.music_library);
				mActionBar.setSubtitle(R.string.touch_to_shuffle_all);
			}
			if (mAlbumArtLoader != null) {
				mAlbumArtLoader.cancel(true);
			}
			mAlbumArtLoader = new AsyncAlbumArtLoader();
			mAlbumArtLoader.execute();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private class AsyncAlbumArtLoader extends AsyncTask<Void, Void, Drawable> {

		@Override
		public Drawable doInBackground(Void... params) {

			if (mService != null) {
				try {
					Bitmap bitmap = MusicUtils.getArtwork(getApplicationContext(),
							mService.getAudioId(), mService.getAlbumId());
					if (bitmap == null) return null;
					int value = 0;
					if (bitmap.getHeight() <= bitmap.getWidth()) {
						value = bitmap.getHeight();
					} else {
						value = bitmap.getWidth();
					}
					Bitmap result = Bitmap.createBitmap(bitmap, (bitmap.getWidth() - value) / 2,
							(bitmap.getHeight() - value) / 2, value, value);
					return new BitmapDrawable(getResources(), result);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			return null;
		}

		@Override
		public void onPostExecute(Drawable result) {
			if (result != null) {
				mActionBar.setIcon(result);
			} else {
				mActionBar.setIcon(R.drawable.ic_launcher_music);
			}
		}
	}

	private class TabsAdapter extends FragmentPagerAdapter implements TabListener {

		private ArrayList<Fragment> mFragments = new ArrayList<Fragment>();

		public TabsAdapter(FragmentManager manager) {
			super(manager);
		}

		public void addFragment(Fragment fragment, String name) {
			mFragments.add(fragment);
			Tab tab = mActionBar.newTab();
			tab.setText(name);
			tab.setTabListener(this);
			mActionBar.addTab(tab);
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
		public void onTabReselected(Tab tab, FragmentTransaction ft) {

		}

		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			mViewPager.setCurrentItem(tab.getPosition());
		}

		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {

		}

	}
}
