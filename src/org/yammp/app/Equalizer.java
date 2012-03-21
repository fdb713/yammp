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

package org.yammp.app;

import java.util.ArrayList;

import org.yammp.Constants;
import org.yammp.IMusicPlaybackService;
import org.yammp.R;
import org.yammp.YAMMPApplication;
import org.yammp.util.MediaUtils;
import org.yammp.util.ServiceToken;
import org.yammp.view.EqualizerView;
import org.yammp.view.EqualizerView.OnBandLevelChangeListener;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class Equalizer extends SherlockFragmentActivity implements Constants, ServiceConnection,
		OnBandLevelChangeListener {

	private IMusicPlaybackService mService = null;
	private ServiceToken mToken;
	private Spinner mSpinner;
	private EqualizerView mEqualizerView;
	private MediaUtils mUtils;

	@Override
	public void onBandLevelChange(short band, short level) {
		if (mService != null) {
			try {
				mService.eqSetBandLevel(band, level);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);
		mUtils = ((YAMMPApplication) getApplication()).getMediaUtils();
		setContentView(R.layout.equalizer);

		mEqualizerView = (EqualizerView) findViewById(R.id.equalizer_view);
		mEqualizerView.setOnBandLevelChangeListener(this);

		mSpinner = (Spinner) findViewById(R.id.presets);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater mInflater = getSupportMenuInflater();
		mInflater.inflate(R.menu.equalizer, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
			case EQUALIZER_PRESETS:
				break;
			case EQUALIZER_RESET:
				resetEqualizer();
				break;

		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onServiceConnected(ComponentName classname, IBinder obj) {

		mService = IMusicPlaybackService.Stub.asInterface(obj);
		setupEqualizerFxAndUI();
	}

	@Override
	public void onServiceDisconnected(ComponentName classname) {

		mService = null;
	}

	@Override
	public void onStart() {

		super.onStart();

		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		mToken = mUtils.bindToService(this);
		if (mToken == null) {
			finish();
		}
	}

	@Override
	public void onStop() {

		mUtils.unbindFromService(mToken);
		mService = null;
		super.onStop();
	}

	private void resetEqualizer() {

		if (mService != null) {
			try {
				mService.eqReset();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			setupEqualizerFxAndUI();
		}
	}

	private void setPreset(short preset_id) {

		if (mService != null) {
			try {
				mService.eqUsePreset(preset_id);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	private void setupEqualizerFxAndUI() {

		if (mService == null) return;

		try {

			short bands = (short) mService.eqGetNumberOfBands();

			mEqualizerView.setNumberOfBands(bands);
			int[] value = mService.eqGetBandLevelRange();
			if (value != null) {
				short[] param = new short[value.length];
				for (short i = 0; i < value.length; i++) {
					param[i] = (short) value[i];
				}
				mEqualizerView.setBandLevelRange(param);
			}

			for (short i = 0; i < bands; i++) {

				mEqualizerView.setCenterFreq(i, mService.eqGetCenterFreq(i));
				mEqualizerView.setBandLevel(i, (short) mService.eqGetBandLevel(i));

			}

			ArrayList<String> list = new ArrayList<String>();
			String mPresetName;
			for (short preset_id = 0; preset_id < mService.eqGetNumberOfPresets(); preset_id++) {
				mPresetName = mService.eqGetPresetName(preset_id);
				list.add(mPresetName);
			}
			CharSequence[] items = list.toArray(new CharSequence[list.size()]);
			ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this,
					android.R.layout.simple_spinner_item, items);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mSpinner.setAdapter(adapter);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

}
