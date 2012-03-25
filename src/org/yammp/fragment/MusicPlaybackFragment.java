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
import org.yammp.app.BaseActivity;
import org.yammp.util.EqualizerWrapper;
import org.yammp.util.MediaUtils;
import org.yammp.util.PreferencesEditor;
import org.yammp.util.ServiceInterface;
import org.yammp.util.ServiceInterface.MediaStateListener;
import org.yammp.widget.RepeatingImageButton;
import org.yammp.widget.RepeatingImageButton.OnRepeatListener;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ViewSwitcher.ViewFactory;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class MusicPlaybackFragment extends SherlockFragment implements Constants, OnClickListener,
		OnLongClickListener, OnRepeatListener, MediaStateListener, ViewFactory {

	private long mStartSeekPos = 0;

	private long mLastSeekEventTime;

	private RepeatingImageButton mPrevButton;
	private ImageButton mPauseButton;
	private RepeatingImageButton mNextButton;

	private PreferencesEditor mPrefs;

	private boolean mShowFadeAnimation = false;
	private boolean mLyricsWakelock = DEFAULT_LYRICS_WAKELOCK;

	int mInitialX = -1;

	int mLastX = -1;

	int mTextWidth = 0;

	int mViewWidth = 0;
	boolean mDraggingLabel = false;

	private BroadcastReceiver mScreenTimeoutListener = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
				if (mInterface != null) {
					mInterface.addMediaStateListener(MusicPlaybackFragment.this);
				}
				getSherlockActivity().invalidateOptionsMenu();
			} else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
				if (mInterface != null) {
					mInterface.removeMediaStateListener(MusicPlaybackFragment.this);
				}
			}
		}
	};

	private MediaUtils mUtils;

	private ServiceInterface mInterface;

	private ImageSwitcher mAlbum;

	private AsyncAlbumArtLoader mAlbumArtLoader;

	@Override
	public View makeView() {
		ImageView view = new ImageView(getActivity());
		view.setScaleType(ImageView.ScaleType.FIT_CENTER);
		view.setLayoutParams(new ImageSwitcher.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
		return view;
	}

	@Override
	public void onActivityCreated(Bundle icicle) {

		mUtils = ((YAMMPApplication) getSherlockActivity().getApplication()).getMediaUtils();
		mInterface = ((YAMMPApplication) getSherlockActivity().getApplication())
				.getServiceInterface();
		mPrefs = new PreferencesEditor(getSherlockActivity());

		super.onActivityCreated(icicle);
		setHasOptionsMenu(true);
		configureActivity();
		mInterface.addMediaStateListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.pause:
				doPauseResume();
				break;
			case R.id.prev:
				doPrev();
				break;
			case R.id.next:
				doNext();
				break;

		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

		inflater.inflate(R.menu.music_playback, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.music_playback, container, false);
	}

	@Override
	public void onFavoriteStateChanged() {
		getSherlockActivity().invalidateOptionsMenu();
	}

	@Override
	public boolean onLongClick(View v) {

		// TODO search media info

		String track = getSherlockActivity().getTitle().toString();
		String artist = "";// mArtistNameView.getText().toString();
		String album = "";// mAlbumNameView.getText().toString();

		CharSequence title = getString(R.string.mediasearch, track);
		Intent i = new Intent();
		i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		i.setAction(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
		i.putExtra(MediaStore.EXTRA_MEDIA_TITLE, track);

		String query = track;
		if (!getString(R.string.unknown_artist).equals(artist)
				&& !getString(R.string.unknown_album).equals(album)) {
			query = artist + " " + track;
			i.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, album);
			i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, artist);
		} else if (getString(R.string.unknown_artist).equals(artist)
				&& !getString(R.string.unknown_album).equals(album)) {
			query = album + " " + track;
			i.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, album);
		} else if (!getString(R.string.unknown_artist).equals(artist)
				&& getString(R.string.unknown_album).equals(album)) {
			query = artist + " " + track;
			i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, artist);
		}
		i.putExtra(SearchManager.QUERY, query);
		i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "audio/*");
		startActivity(Intent.createChooser(i, title));
		return true;
	}

	@Override
	public void onMetaChanged() {
		getSherlockActivity().invalidateOptionsMenu();
		setPauseButtonImage();
		if (mAlbumArtLoader != null) {
			mAlbumArtLoader.cancel(true);
		}
		mAlbumArtLoader = new AsyncAlbumArtLoader();
		mAlbumArtLoader.execute();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent intent;
		switch (item.getItemId()) {
			case MENU_TOGGLE_SHUFFLE:
				if (mInterface != null) {
					mInterface.toggleShuffle();
				}
				break;
			case MENU_TOGGLE_REPEAT:
				if (mInterface != null) {
					mInterface.toggleRepeat();
				}
				break;
			case MENU_ADD_TO_FAVORITES:
				if (mInterface != null) {
					mInterface.toggleFavorite();
				}
				break;
			case MENU_ADD_TO_PLAYLIST:
				intent = new Intent(INTENT_ADD_TO_PLAYLIST);
				long[] list_to_be_added = new long[1];
				list_to_be_added[0] = mUtils.getCurrentAudioId();
				intent.putExtra(INTENT_KEY_LIST, list_to_be_added);
				startActivity(intent);
				break;
			case EQUALIZER:
				intent = new Intent(INTENT_EQUALIZER);
				startActivity(intent);
				break;
			case MENU_SLEEP_TIMER:
				intent = new Intent(INTENT_SLEEP_TIMER);
				startActivity(intent);
				break;
			case DELETE_ITEMS:
				intent = new Intent(INTENT_DELETE_ITEMS);
				Bundle bundle = new Bundle();
				bundle.putString(
						INTENT_KEY_PATH,
						Uri.withAppendedPath(Audio.Media.EXTERNAL_CONTENT_URI,
								Uri.encode(String.valueOf(mUtils.getCurrentAudioId()))).toString());
				intent.putExtras(bundle);
				startActivity(intent);
				break;
			case SETTINGS:
				intent = new Intent(INTENT_APPEARANCE_SETTINGS);
				startActivity(intent);
				break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPlayStateChanged() {
		setPauseButtonImage();
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		MenuItem item = menu.findItem(EQUALIZER);
		if (item != null) {
			item.setVisible(EqualizerWrapper.isSupported());
		}
		item = menu.findItem(MENU_TOGGLE_SHUFFLE);
		if (item != null && mInterface != null) {
			switch (mInterface.getShuffleMode()) {
				case SHUFFLE_NONE:
					item.setIcon(R.drawable.ic_mp_shuffle_off_btn);
					break;
				default:
					item.setIcon(R.drawable.ic_mp_shuffle_on_btn);
					break;
			}
		}
		item = menu.findItem(MENU_TOGGLE_REPEAT);
		if (item != null && mInterface != null) {
			switch (mInterface.getRepeatMode()) {
				case REPEAT_ALL:
					item.setIcon(R.drawable.ic_mp_repeat_all_btn);
					break;
				case REPEAT_CURRENT:
					item.setIcon(R.drawable.ic_mp_repeat_once_btn);
					break;
				default:
					item.setIcon(R.drawable.ic_mp_repeat_off_btn);
					break;
			}
		}
		item = menu.findItem(MENU_ADD_TO_FAVORITES);
		if (item != null && mInterface != null) {
			item.setIcon(mInterface.isFavorite(mInterface.getAudioId()) ? R.drawable.ic_menu_star
					: R.drawable.ic_menu_star_off);
		}

		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onQueueChanged() {

	}

	@Override
	public void onRepeat(View v, long howlong, int repcnt) {
		switch (v.getId()) {
			case R.id.prev:
				scanBackward(repcnt, howlong);
				break;
			case R.id.next:
				scanForward(repcnt, howlong);
				break;
		}
	}

	@Override
	public void onRepeatModeChanged() {
		getSherlockActivity().invalidateOptionsMenu();
	}

	@Override
	public void onResume() {
		super.onResume();
		setPauseButtonImage();
	}

	@Override
	public void onShuffleModeChanged() {
		getSherlockActivity().invalidateOptionsMenu();
	}

	@Override
	public void onStart() {

		super.onStart();
		loadPreferences();

		if (mLyricsWakelock) {
			getSherlockActivity().getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
		} else {
			getSherlockActivity().getWindow().clearFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
		}

		try {
			float mTransitionAnimation = Settings.System.getFloat(getSherlockActivity()
					.getContentResolver(), Settings.System.TRANSITION_ANIMATION_SCALE);
			if (mTransitionAnimation > 0.0) {
				mShowFadeAnimation = true;
			} else {
				mShowFadeAnimation = false;
			}

		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}

		IntentFilter s = new IntentFilter();
		s.addAction(Intent.ACTION_SCREEN_ON);
		s.addAction(Intent.ACTION_SCREEN_OFF);
		getSherlockActivity().registerReceiver(mScreenTimeoutListener, new IntentFilter(s));

	}

	@Override
	public void onStop() {

		getSherlockActivity().unregisterReceiver(mScreenTimeoutListener);
		getSherlockActivity().getWindow().clearFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

		super.onStop();
	}

	private void configureActivity() {

		View view = getView();

		mPrevButton = (RepeatingImageButton) view.findViewById(R.id.prev);
		mPrevButton.setOnClickListener(this);
		mPrevButton.setRepeatListener(this, 260);

		mPauseButton = (ImageButton) view.findViewById(R.id.pause);
		mPauseButton.requestFocus();
		mPauseButton.setOnClickListener(this);

		mNextButton = (RepeatingImageButton) view.findViewById(R.id.next);
		mNextButton.setOnClickListener(this);
		mNextButton.setRepeatListener(this, 260);

		mAlbum = (ImageSwitcher) view.findViewById(R.id.album_art);
		// mAlbum.setOnLongClickListener(mSearchAlbumArtListener);
		mAlbum.setFactory(this);

		getFragmentManager().beginTransaction()
				.replace(R.id.playback_info_lyrics, new LyricsFragment()).commit();

	}

	private void doNext() {

		if (mInterface == null) return;
		mInterface.next();
	}

	private void doPauseResume() {

		if (mInterface != null) {
			if (mInterface.isPlaying()) {
				mInterface.pause();
			} else {
				mInterface.play();
			}
			setPauseButtonImage();
		}
	}

	private void doPrev() {

		if (mInterface == null) return;
		if (mInterface.position() < 2000) {
			mInterface.prev();
		} else {
			mInterface.seek(0);
			mInterface.play();
		}
	}

	private void loadPreferences() {
		mLyricsWakelock = mPrefs.getBooleanPref(KEY_LYRICS_WAKELOCK, DEFAULT_LYRICS_WAKELOCK);
	}

	private void scanBackward(int repcnt, long delta) {

		if (mInterface == null) return;
		if (repcnt == 0) {
			mStartSeekPos = mInterface.position();
			mLastSeekEventTime = 0;
		} else {
			if (delta < 5000) {
				// seek at 10x speed for the first 5 seconds
				delta = delta * 10;
			} else {
				// seek at 40x after that
				delta = 50000 + (delta - 5000) * 40;
			}
			long newpos = mStartSeekPos - delta;
			if (newpos < 0) {
				// move to previous track
				mInterface.prev();
				long duration = mInterface.duration();
				mStartSeekPos += duration;
				newpos += duration;
			}
			if (delta - mLastSeekEventTime > 250 || repcnt < 0) {
				mInterface.seek(newpos);
				mLastSeekEventTime = delta;
			}
			if (repcnt >= 0) {
			} else {
			}
		}
	}

	private void scanForward(int repcnt, long delta) {

		if (mInterface == null) return;
		if (repcnt == 0) {
			mStartSeekPos = mInterface.position();
			mLastSeekEventTime = 0;
		} else {
			if (delta < 5000) {
				// seek at 10x speed for the first 5 seconds
				delta = delta * 10;
			} else {
				// seek at 40x after that
				delta = 50000 + (delta - 5000) * 40;
			}
			long newpos = mStartSeekPos + delta;
			long duration = mInterface.duration();
			if (newpos >= duration) {
				// move to next track
				mInterface.next();
				mStartSeekPos -= duration; // is OK to go negative
				newpos -= duration;
			}
			if (delta - mLastSeekEventTime > 250 || repcnt < 0) {
				mInterface.seek(newpos);
				mLastSeekEventTime = delta;
			}
			if (repcnt >= 0) {
			} else {
			}
		}
	}

	private void setPauseButtonImage() {

		if (mInterface != null) {
			mPauseButton.setImageResource(mInterface.isPlaying() ? R.drawable.btn_playback_ic_pause
					: R.drawable.btn_playback_ic_play);
		} else {

		}
	}

	private class AsyncAlbumArtLoader extends AsyncTask<Void, Void, Drawable> {

		@Override
		public Drawable doInBackground(Void... params) {

			if (isDetached() || !isAdded()) return null;

			if (mInterface != null) {
				Bitmap bitmap = mUtils.getArtwork(mInterface.getAudioId(), mInterface.getAlbumId());
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
			}
			return null;
		}

		@Override
		public void onPostExecute(Drawable result) {

			if (isDetached() || !isAdded()) return;

			if (mAlbum != null) {
				if (result != null) {
					mAlbum.setImageDrawable(result);
				} else {
					mAlbum.setImageResource(R.drawable.ic_mp_albumart_unknown);
				}
			}
			if (mInterface != null) {
				((BaseActivity) getSherlockActivity()).setBackground(mInterface.getAudioId(),
						mInterface.getAlbumId());
			}
		}
	}

}
