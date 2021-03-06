/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.yammp.fragment;

import org.yammp.R;

import com.actionbarsherlock.app.SherlockActivity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;

/**
 * This activity plays a video from a specified URI.
 */
public class VideoPlaybackActivity extends SherlockActivity {

	private MovieViewControl mControl;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.movie_view);
		View rootView = findViewById(R.id.root);
		Intent intent = getIntent();
		mControl = new MovieViewControl(rootView, this, intent.getData()) {

			@Override
			public void onCompletion() {
				finish();
			}
		};
		if (intent.hasExtra(MediaStore.EXTRA_SCREEN_ORIENTATION)) {
			int orientation = intent.getIntExtra(MediaStore.EXTRA_SCREEN_ORIENTATION,
					ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
			if (orientation != getRequestedOrientation()) {
				setRequestedOrientation(orientation);
			}
		}
	}

	@Override
	public void onPause() {
		mControl.onPause();
		super.onPause();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		if (hasFocus) mControl.onResume();
	}
}
