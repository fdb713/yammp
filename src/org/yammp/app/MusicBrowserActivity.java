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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.yammp.Constants;
import org.yammp.IMusicPlaybackService;
import org.yammp.R;
import org.yammp.YAMMPApplication;
import org.yammp.dialog.ScanningProgress;
import org.yammp.util.MediaUtils;
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
import android.widget.ArrayAdapter;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

public class MusicBrowserActivity extends YAMMPActivity implements Constants,
		ServiceConnection {

	private ActionBar mActionBar;

	private ServiceToken mToken;
	private IMusicPlaybackService mService;
	private AsyncAlbumArtLoader mAlbumArtLoader;
	private PagesAdapter mAdapter;
	private MediaUtils mUtils;

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

		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		super.onCreate(icicle);
		mUtils = ((YAMMPApplication)getApplication()).getMediaUtils();
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		setContentView(R.layout.main);

		mActionBar = getSupportActionBar();
		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		String mount_state = Environment.getExternalStorageState();

		if (!Environment.MEDIA_MOUNTED.equals(mount_state)
				&& !Environment.MEDIA_MOUNTED_READ_ONLY.equals(mount_state)) {
			startActivity(new Intent(this, ScanningProgress.class));
			finish();
		}

		mAdapter = new PagesAdapter(mActionBar);
		mAdapter.addPage(MusicBrowserFragment.class, "Music Library");
		mAdapter.addPage(MusicPlaybackFragment.class, "Now Playing");

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
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem item = menu.findItem(PLAY_PAUSE);
		try {
			if (item != null && mService != null) {
				item.setIcon(mService.isPlaying() ? R.drawable.ic_action_media_pause
						: R.drawable.ic_action_media_play);
			}
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
		mToken = mUtils.bindToService(this);
		IntentFilter filter = new IntentFilter();
		filter.addAction(BROADCAST_META_CHANGED);
		filter.addAction(BROADCAST_QUEUE_CHANGED);
		filter.addAction(BROADCAST_PLAYSTATE_CHANGED);
		registerReceiver(mMediaStatusReceiver, filter);

	}

	@Override
	public void onStop() {

		unregisterReceiver(mMediaStatusReceiver);
		mUtils.unbindFromService(mToken);
		mService = null;
		super.onStop();
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
					Bitmap bitmap = mUtils.getArtwork(mService.getAudioId(), mService.getAlbumId());
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

	private class PagesAdapter extends ArrayAdapter<String> implements OnNavigationListener {

		private ArrayList<Class<? extends Fragment>> mFragments = new ArrayList<Class<? extends Fragment>>();

		public PagesAdapter(ActionBar actionbar) {
			super(actionbar.getThemedContext(), R.layout.sherlock_spinner_item,
					new ArrayList<String>());
			setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);
			actionbar.setListNavigationCallbacks(this, this);

		}

		public void addPage(Class<? extends Fragment> fragment, String name) {
			add(name);
			mFragments.add(fragment);
		}

		@Override
		public boolean onNavigationItemSelected(int position, long id) {
			Fragment instance;
			try {
				instance = mFragments.get(position).getConstructor(new Class[] {}).newInstance();
				getSupportFragmentManager().beginTransaction().replace(R.id.content, instance)
						.commit();
				return true;
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}
			return false;
		}

	}

}
