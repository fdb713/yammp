package org.yammp.dialog;

import org.yammp.Constants;
import org.yammp.R;
import org.yammp.YAMMPApplication;
import org.yammp.util.MediaUtils;
import org.yammp.util.ServiceToken;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.widget.LinearLayout;
import android.widget.Toast;

public class PlayShortcut extends FragmentActivity implements Constants, ServiceConnection {

	private long mPlaylistId;
	private ServiceToken mToken = null;
	private MediaUtils mUtils;

	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);
		mUtils = ((YAMMPApplication) getApplication()).getMediaUtils();
		setContentView(new LinearLayout(this));
		mPlaylistId = getIntent().getLongExtra(MAP_KEY_ID, PLAYLIST_UNKNOWN);
		mToken = mUtils.bindToService(this);
	}

	@Override
	public void onServiceConnected(ComponentName classname, IBinder obj) {

		if (getIntent().getAction() != null && getIntent().getAction().equals(INTENT_PLAY_SHORTCUT)
				&& mPlaylistId != PLAYLIST_UNKNOWN) {
			switch ((int) mPlaylistId) {
				case (int) PLAYLIST_ALL_SONGS:
					mUtils.playAll();
					break;
				case (int) PLAYLIST_RECENTLY_ADDED:
					mUtils.playRecentlyAdded();
					break;
				default:
					if (mPlaylistId >= 0) {
						mUtils.playPlaylist(mPlaylistId);
					}
					break;
			}

		} else {
			Toast.makeText(PlayShortcut.this, R.string.error_bad_parameters, Toast.LENGTH_SHORT)
					.show();
		}
		finish();
	}

	@Override
	public void onServiceDisconnected(ComponentName classname) {

		finish();
	}

	@Override
	public void onStop() {

		if (mToken != null) {
			mUtils.unbindFromService(mToken);
		}
		finish();
		super.onStop();
	}

}