/*
 *              Copyright (C) 2011 The MusicMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.yammp.dialog;

import java.util.ArrayList;
import java.util.List;

import org.yammp.Constants;
import org.yammp.IMusicPlaybackService;
import org.yammp.R;
import org.yammp.util.EqualizerWrapper;
import org.yammp.util.MusicUtils;
import org.yammp.util.PreferencesEditor;
import org.yammp.util.ServiceToken;
import org.yammp.view.EqualizerView;
import org.yammp.view.EqualizerView.OnBandLevelChangeListener;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class EqualizerDialog extends FragmentActivity implements Constants,
		OnBandLevelChangeListener {

	private IMusicPlaybackService mService = null;
	private ServiceToken mToken;
	private EqualizerWrapper mEqualizer;
	private EqualizerView mEqualizerView;
	private int mAudioSessionId;
	private PreferencesEditor mPrefs;

	private ServiceConnection osc = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName classname, IBinder obj) {

			mService = IMusicPlaybackService.Stub.asInterface(obj);
		}

		@Override
		public void onServiceDisconnected(ComponentName classname) {

			mService = null;
		}
	};

	@Override
	public void onBandLevelChange(short band, short level) {
		if (mEqualizer != null) {
			mEqualizer.setBandLevel(band, level);
			mPrefs.setEqualizerSetting(band, level);
			reloadEqualizer();
		}

	}

	@Override
	public void onCreate(Bundle icicle) {

		if (!EqualizerWrapper.isSupported()) {
			finish();
		}

		super.onCreate(icicle);

		mPrefs = new PreferencesEditor(getApplicationContext());

		mEqualizerView = new EqualizerView(this);
		mEqualizerView.setOnBandLevelChangeListener(this);
		setContentView(mEqualizerView);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater mInflater = getMenuInflater();
		mInflater.inflate(R.menu.equalizer, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case EQUALIZER_PRESETS:
				showPresets();
				break;
			case EQUALIZER_RESET:
				resetEqualizer();
				break;

		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		MenuItem MENU_PRESETS = menu.findItem(EQUALIZER_PRESETS);
		if (mEqualizer != null) {
			if (mEqualizer.getNumberOfPresets() <= 0) {
				MENU_PRESETS.setVisible(false);
			} else {
				MENU_PRESETS.setVisible(true);
			}
		}
		return super.onPrepareOptionsMenu(menu);
	}

	private void reloadEqualizer() {

		try {
			mService.reloadEqualizer();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void resetEqualizer() {

		if (mEqualizer != null) {
			short bands = mEqualizer.getNumberOfBands();

			final short minEQLevel = mEqualizer.getBandLevelRange()[0];
			final short maxEQLevel = mEqualizer.getBandLevelRange()[1];

			for (short i = 0; i < bands; i++) {
				final short band = i;
				mEqualizer.setBandLevel(band, (short) ((maxEQLevel + minEQLevel) / 2));
				mPrefs.setEqualizerSetting(band, (short) ((maxEQLevel + minEQLevel) / 2));
			}
			reloadEqualizer();
			setupEqualizerFxAndUI(mAudioSessionId);
		}
	}

	private void setPreset(short preset_id) {

		if (mEqualizer != null) {
			mEqualizer.usePreset(preset_id);
			for (short i = 0; i < mEqualizer.getNumberOfBands(); i++) {
				short band = i;
				mPrefs.setEqualizerSetting(band, mEqualizer.getBandLevel(band));
			}
		}
	}

	private void setupEqualizerFxAndUI(int audioSessionId) {

		// Create the Equalizer object (an AudioEffect subclass) and attach it
		// to our media player, with a default priority (0).
		mEqualizer = new EqualizerWrapper(0, audioSessionId);
		if (mEqualizer == null) {
			finish();
			return;
		}
		mEqualizer.setEnabled(false);

		short bands = mEqualizer.getNumberOfBands();

		mEqualizerView.setNumberOfBands(bands);
		mEqualizerView.setBandLevelRange(mEqualizer.getBandLevelRange());

		for (short i = 0; i < bands; i++) {

			mEqualizerView.setCenterFreq(i, mEqualizer.getCenterFreq(i));
			mEqualizer.setBandLevel(i, mPrefs.getEqualizerSetting(i, (short) 0));
			mEqualizerView.setBandLevel(i, mPrefs.getEqualizerSetting(i, (short) 0));

		}
	}

	private void showPresets() {

		if (mEqualizer != null) {
			List<String> mPresetsList = new ArrayList<String>();
			String mPresetName;
			for (short preset_id = 0; preset_id < mEqualizer.getNumberOfPresets(); preset_id++) {
				mPresetName = mEqualizer.getPresetName(preset_id);
				mPresetsList.add(mPresetName);
			}

			CharSequence[] mPresetsItems = mPresetsList.toArray(new CharSequence[mPresetsList
					.size()]);

			new AlertDialog.Builder(this).setTitle(R.string.equalizer_presets)
					.setItems(mPresetsItems, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int item) {

							setPreset((short) item);
							setupEqualizerFxAndUI(mAudioSessionId);
							reloadEqualizer();
						}
					}).show();
		}
	}

	@Override
	protected void onStart() {

		super.onStart();

		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		mToken = MusicUtils.bindToService(this, osc);
		if (mToken == null) {
			finish();
		}

		if (mService != null) {
			try {
				mAudioSessionId = mService.getAudioSessionId();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		setupEqualizerFxAndUI(mAudioSessionId);
	}

	@Override
	protected void onStop() {

		reloadEqualizer();
		if (mEqualizer != null) {
			mEqualizer.release();
		}
		MusicUtils.unbindFromService(mToken);
		mService = null;
		super.onStop();
	}
}
