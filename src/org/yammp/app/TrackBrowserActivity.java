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

import org.yammp.Constants;
import org.yammp.R;
import org.yammp.YAMMPApplication;
import org.yammp.util.MediaUtils;
import org.yammp.util.ServiceToken;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore.Audio;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

public class TrackBrowserActivity extends SherlockFragmentActivity implements Constants,
		ServiceConnection {

	private ServiceToken mToken;
	private Intent intent;
	private Bundle bundle;
	private MediaUtils mUtils;

	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);
		mUtils = ((YAMMPApplication)getApplication()).getMediaUtils();
		setVolumeControlStream(AudioManager.STREAM_MUSIC);

		intent = getIntent();
		bundle = icicle != null ? icicle : intent.getExtras();

		if (bundle == null) {
			bundle = new Bundle();
		}

		if (bundle.getString(INTENT_KEY_ACTION) == null) {
			bundle.putString(INTENT_KEY_ACTION, intent.getAction());
		}
		if (bundle.getString(INTENT_KEY_TYPE) == null) {
			bundle.putString(INTENT_KEY_TYPE, intent.getType());
		}

		TrackFragment fragment = new TrackFragment(bundle);

		getSupportFragmentManager().beginTransaction().replace(android.R.id.content, fragment)
				.commit();

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				finish();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onSaveInstanceState(Bundle outcicle) {
		outcicle.putAll(bundle);
		super.onSaveInstanceState(outcicle);
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {

	}

	@Override
	public void onServiceDisconnected(ComponentName name) {

		finish();
	}

	@Override
	public void onStart() {
		super.onStart();
		mToken = mUtils.bindToService(this);
		setTitle();
	}

	@Override
	public void onStop() {

		mUtils.unbindFromService(mToken);
		super.onStop();
	}

	private void setTitle() {
		String mimeType = bundle.getString(INTENT_KEY_TYPE);
		String name;
		long id;
		if (Audio.Playlists.CONTENT_TYPE.equals(mimeType)) {
			id = bundle.getLong(Audio.Playlists._ID);
			switch ((int) id) {
				case (int) PLAYLIST_QUEUE:
					setTitle(R.string.now_playing);
					return;
				case (int) PLAYLIST_FAVORITES:
					setTitle(R.string.favorites);
					return;
				case (int) PLAYLIST_RECENTLY_ADDED:
					setTitle(R.string.recently_added);
					return;
				case (int) PLAYLIST_PODCASTS:
					setTitle(R.string.podcasts);
					return;
				default:
					if (id < 0) {
						setTitle(R.string.music_library);
						return;
					}
			}

			name = mUtils.getPlaylistName(id);
		} else if (Audio.Artists.CONTENT_TYPE.equals(mimeType)) {
			id = bundle.getLong(Audio.Artists._ID);
			name = mUtils.getArtistName(id, true);
		} else if (Audio.Albums.CONTENT_TYPE.equals(mimeType)) {
			id = bundle.getLong(Audio.Albums._ID);
			name = mUtils.getAlbumName(id, true);
		} else if (Audio.Genres.CONTENT_TYPE.equals(mimeType)) {
			id = bundle.getLong(Audio.Genres._ID);
			name = mUtils.parseGenreName(mUtils.getGenreName(id,
					true));
		} else {
			setTitle(R.string.music_library);
			return;
		}

		setTitle(name);

	}

}
