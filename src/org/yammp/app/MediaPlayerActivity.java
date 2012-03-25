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
import org.yammp.R;
import org.yammp.dialog.ScanningProgress;
import org.yammp.fragment.MusicBrowserFragment;
import org.yammp.fragment.MusicPlaybackFragment;
import org.yammp.fragment.VideoFragment;
import org.yammp.util.PreferencesEditor;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.widget.ArrayAdapter;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.view.Window;

public class MediaPlayerActivity extends BaseActivity implements Constants {

	private ActionBar mActionBar;
	private PagesAdapter mAdapter;
	private PreferencesEditor mPrefs;

	@Override
	public void onCreate(Bundle icicle) {

		requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		super.onCreate(icicle);
		mPrefs = new PreferencesEditor(this);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		setContentView(R.layout.main);

		mActionBar = getSupportActionBar();
		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		mActionBar.setDisplayShowTitleEnabled(false);

		String mount_state = Environment.getExternalStorageState();

		if (!Environment.MEDIA_MOUNTED.equals(mount_state)
				&& !Environment.MEDIA_MOUNTED_READ_ONLY.equals(mount_state)) {
			startActivity(new Intent(this, ScanningProgress.class));
			finish();
		}

		mAdapter = new PagesAdapter(mActionBar);
		mAdapter.addPage(MusicBrowserFragment.class, getString(R.string.label_music));
		mAdapter.addPage(VideoFragment.class, getString(R.string.label_video));
		mAdapter.addPage(MusicPlaybackFragment.class, getString(R.string.now_playing));

	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putAll(getIntent().getExtras() != null ? getIntent().getExtras() : new Bundle());
		super.onSaveInstanceState(outState);
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
