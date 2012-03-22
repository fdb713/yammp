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
import org.yammp.IMusicPlaybackService;
import org.yammp.MusicPlaybackService;
import org.yammp.R;
import org.yammp.YAMMPApplication;
import org.yammp.util.EqualizerWrapper;
import org.yammp.util.MediaUtils;
import org.yammp.util.PreferencesEditor;
import org.yammp.util.ServiceInterface;
import org.yammp.util.ServiceInterface.MediaStateListener;
import org.yammp.util.ServiceToken;
import org.yammp.util.VisualizerCompat;
import org.yammp.view.SliderView;
import org.yammp.view.SliderView.OnValueChangeListener;
import org.yammp.widget.RepeatingImageButton;
import org.yammp.widget.RepeatingImageButton.OnRepeatListener;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.ViewSwitcher.ViewFactory;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class MusicPlaybackFragment extends SherlockFragment implements Constants, OnClickListener,
		OnLongClickListener, OnRepeatListener, MediaStateListener {

	private long mStartSeekPos = 0;

	private long mLastSeekEventTime;

	private RepeatingImageButton mPrevButton;
	private ImageButton mPauseButton;
	private RepeatingImageButton mNextButton;

	private boolean mIntentDeRegistered = false;

	private PreferencesEditor mPrefs;

	private boolean mDisplayVisualizer = false;
	private FrameLayout mVisualizerView;

	private VisualizerCompat mVisualizer;

	private static final int RESULT_ALBUMART_DOWNLOADED = 1;
	private boolean mShowFadeAnimation = false;
	private boolean mLyricsWakelock = DEFAULT_LYRICS_WAKELOCK;

	int mInitialX = -1;

	int mLastX = -1;

	int mTextWidth = 0;

	int mViewWidth = 0;
	boolean mDraggingLabel = false;

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

	private final static int DISABLE_VISUALIZER = 0;

	private final static int ENABLE_VISUALIZER = 1;

	Handler mVisualizerHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			mVisualizerHandler.removeCallbacksAndMessages(null);
			switch (msg.what) {
				case DISABLE_VISUALIZER:
					mVisualizer.setEnabled(false);
					break;
				case ENABLE_VISUALIZER:
					mVisualizer.setEnabled(true);
					break;
			}
		}
	};

	private BroadcastReceiver mScreenTimeoutListener = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
				if (mIntentDeRegistered) {
					mIntentDeRegistered = false;
				}
				if (mDisplayVisualizer) {
					enableVisualizer();
				}
				getSherlockActivity().invalidateOptionsMenu();
			} else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
				disableVisualizer(true);
				if (!mIntentDeRegistered) {
					mIntentDeRegistered = true;
				}
			}
		}
	};

	private MediaUtils mUtils;

	private ServiceInterface mInterface;

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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

		inflater.inflate(R.menu.music_playback, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.music_playback, container, false);
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
	public boolean onOptionsItemSelected(MenuItem item) {

		Intent intent;
		switch (item.getItemId()) {
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
			case ADD_TO_FAVORITES:
				toggleFavorite();
				break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		MenuItem item = menu.findItem(EQUALIZER);
		if (item != null) {
			item.setVisible(EqualizerWrapper.isSupported());
		}
		item = menu.findItem(ADD_TO_FAVORITES);
		if (item != null && mInterface != null) {
			item.setIcon(mInterface.isFavorite(mInterface.getAudioId()) ? R.drawable.ic_menu_star
					: R.drawable.ic_menu_star_off);
		}
		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onResume() {

		super.onResume();
		if (mIntentDeRegistered) {
		}

		setPauseButtonImage();
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

		mInterface = null;
		super.onStop();
	}

	private void configureActivity() {

		mPrevButton = (RepeatingImageButton) getView().findViewById(R.id.prev);
		mPrevButton.setOnClickListener(this);
		mPrevButton.setRepeatListener(this, 260);

		mPauseButton = (ImageButton) getView().findViewById(R.id.pause);
		mPauseButton.requestFocus();
		mPauseButton.setOnClickListener(this);

		mNextButton = (RepeatingImageButton) getView().findViewById(R.id.next);
		mNextButton.setOnClickListener(this);
		mNextButton.setRepeatListener(this, 260);

		mVisualizerView = (FrameLayout) getView().findViewById(R.id.visualizer_view);

		if (getView().findViewById(R.id.albumart_frame) != null) {
			getFragmentManager().beginTransaction()
					.replace(R.id.albumart_frame, new AlbumArtFragment()).commit();
		}

		getFragmentManager().beginTransaction()
				.replace(R.id.playback_frame, new LyricsAndQueueFragment()).commit();

	}

	private void disableVisualizer(boolean animation) {
		if (mVisualizer != null) {
			if (mVisualizerView.getVisibility() == View.VISIBLE) {
				mVisualizerView.setVisibility(View.INVISIBLE);
				if (mShowFadeAnimation) {
					mVisualizerView.startAnimation(AnimationUtils.loadAnimation(
							getSherlockActivity(), android.R.anim.fade_out));
				}
				if (animation) {
					mVisualizerHandler.sendEmptyMessageDelayed(DISABLE_VISUALIZER, AnimationUtils
							.loadAnimation(getSherlockActivity(), android.R.anim.fade_out)
							.getDuration());
				} else {
					mVisualizerHandler.sendEmptyMessage(DISABLE_VISUALIZER);
				}

			}
		}

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

	private void enableVisualizer() {
		if (mVisualizer != null) {
			if (mVisualizerView.getVisibility() != View.VISIBLE) {
				mVisualizerView.setVisibility(View.VISIBLE);
				mVisualizerHandler.sendEmptyMessage(ENABLE_VISUALIZER);
				if (mShowFadeAnimation) {
					mVisualizerView.startAnimation(AnimationUtils.loadAnimation(
							getSherlockActivity(), android.R.anim.fade_in));
				}
			}
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

	private void toggleFavorite() {

		if (mInterface == null) return;
		mInterface.toggleFavorite();
	}

	public static class AlbumArtFragment extends SherlockFragment implements ViewFactory,
			OnClickListener, ServiceConnection {

		private ImageSwitcher mAlbum;

		private IMusicPlaybackService mService;
		private ServiceToken mToken;
		private AsyncAlbumArtLoader mAlbumArtLoader;
		private ImageButton mRepeatButton, mShuffleButton;
		private boolean mIntentDeRegistered = false;

		private BroadcastReceiver mStatusListener = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {

				String action = intent.getAction();
				if (BROADCAST_META_CHANGED.equals(action)) {
					updateTrackInfo();
				}
			}
		};

		private BroadcastReceiver mScreenTimeoutListener = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {

				if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
					if (mIntentDeRegistered) {
						IntentFilter f = new IntentFilter();
						f.addAction(BROADCAST_META_CHANGED);
						getActivity().registerReceiver(mStatusListener, new IntentFilter(f));
						mIntentDeRegistered = false;
					}
					updateTrackInfo();
				} else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
					if (!mIntentDeRegistered) {
						getActivity().unregisterReceiver(mStatusListener);
						mIntentDeRegistered = true;
					}
				}
			}
		};

		private View.OnLongClickListener mSearchAlbumArtListener = new View.OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {

				searchAlbumArt();
				return true;
			}
		};

		private MediaUtils mUtils;

		@Override
		public View makeView() {
			ImageView view = new ImageView(getActivity());
			view.setScaleType(ImageView.ScaleType.FIT_CENTER);
			view.setLayoutParams(new ImageSwitcher.LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT));
			return view;
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			mUtils = ((YAMMPApplication) getSherlockActivity().getApplication()).getMediaUtils();
			View view = getView();

			mAlbum = (ImageSwitcher) view.findViewById(R.id.album_art);
			mAlbum.setOnLongClickListener(mSearchAlbumArtListener);
			mAlbum.setFactory(this);

			mShuffleButton = (ImageButton) view.findViewById(R.id.toggle_shuffle);
			mShuffleButton.setOnClickListener(this);

			mRepeatButton = (ImageButton) view.findViewById(R.id.toggle_repeat);
			mRepeatButton.setOnClickListener(this);

		}

		@Override
		public void onClick(View view) {
			if (view == mShuffleButton) {
				toggleShuffle();
			} else if (view == mRepeatButton) {
				toggleRepeat();
			}
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.playback_albumart, container, false);
			return view;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder obj) {
			mService = IMusicPlaybackService.Stub.asInterface(obj);
			updateTrackInfo();
			setRepeatButtonImage();
			setShuffleButtonImage();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			getActivity().finish();

		}

		@Override
		public void onStart() {
			super.onStart();
			mToken = mUtils.bindToService(this);
			IntentFilter f = new IntentFilter();
			f.addAction(BROADCAST_META_CHANGED);
			getActivity().registerReceiver(mStatusListener, new IntentFilter(f));

			IntentFilter s = new IntentFilter();
			s.addAction(Intent.ACTION_SCREEN_ON);
			s.addAction(Intent.ACTION_SCREEN_OFF);
			getActivity().registerReceiver(mScreenTimeoutListener, new IntentFilter(s));

		}

		@Override
		public void onStop() {
			if (mAlbumArtLoader != null) {
				mAlbumArtLoader.cancel(true);
			}
			if (!mIntentDeRegistered) {
				getActivity().unregisterReceiver(mStatusListener);
			}
			getActivity().unregisterReceiver(mScreenTimeoutListener);

			mUtils.unbindFromService(mToken);
			super.onStop();
		}

		private void searchAlbumArt() {

			String artistName = "";
			String albumName = "";
			String mediaPath = "";
			String albumArtPath = "";
			try {
				artistName = mService.getArtistName();
				albumName = mService.getAlbumName();
				mediaPath = mService.getMediaPath();
				albumArtPath = mediaPath.substring(0, mediaPath.lastIndexOf("/")) + "/AlbumArt.jpg";
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				Intent intent = new Intent(INTENT_SEARCH_ALBUMART);
				intent.putExtra(INTENT_KEY_ARTIST, artistName);
				intent.putExtra(INTENT_KEY_ALBUM, albumName);
				intent.putExtra(INTENT_KEY_PATH, albumArtPath);
				startActivityForResult(intent, RESULT_ALBUMART_DOWNLOADED);
			} catch (ActivityNotFoundException e) {
				// e.printStackTrace();
			}

		}

		private void setRepeatButtonImage() {

			if (mService == null) return;
			try {
				switch (mService.getRepeatMode()) {
					case REPEAT_ALL:
						mRepeatButton.setImageResource(R.drawable.ic_mp_repeat_all_btn);
						break;
					case REPEAT_CURRENT:
						mRepeatButton.setImageResource(R.drawable.ic_mp_repeat_once_btn);
						break;
					default:
						mRepeatButton.setImageResource(R.drawable.ic_mp_repeat_off_btn);
						break;
				}
			} catch (RemoteException ex) {
				ex.printStackTrace();
			}
		}

		private void setShuffleButtonImage() {

			if (mService == null) return;
			try {
				switch (mService.getShuffleMode()) {
					case SHUFFLE_NONE:
						mShuffleButton.setImageResource(R.drawable.ic_mp_shuffle_off_btn);
						break;
					default:
						mShuffleButton.setImageResource(R.drawable.ic_mp_shuffle_on_btn);
						break;
				}
			} catch (RemoteException ex) {
				ex.printStackTrace();
			}
		}

		private void toggleRepeat() {

			if (mService == null) return;
			try {
				int mode = mService.getRepeatMode();
				if (mode == MusicPlaybackService.REPEAT_NONE) {
					mService.setRepeatMode(MusicPlaybackService.REPEAT_ALL);
					Toast.makeText(getActivity(), R.string.repeat_all_notif, Toast.LENGTH_SHORT);
				} else if (mode == MusicPlaybackService.REPEAT_ALL) {
					mService.setRepeatMode(MusicPlaybackService.REPEAT_CURRENT);
					if (mService.getShuffleMode() != MusicPlaybackService.SHUFFLE_NONE) {
						mService.setShuffleMode(MusicPlaybackService.SHUFFLE_NONE);
						setShuffleButtonImage();
					}
					Toast.makeText(getActivity(), R.string.repeat_current_notif, Toast.LENGTH_SHORT);
				} else {
					mService.setRepeatMode(MusicPlaybackService.REPEAT_NONE);
					Toast.makeText(getActivity(), R.string.repeat_off_notif, Toast.LENGTH_SHORT);
				}
				setRepeatButtonImage();
			} catch (RemoteException ex) {
			}

		}

		private void toggleShuffle() {

			if (mService == null) return;
			try {
				int shuffle = mService.getShuffleMode();
				if (shuffle == SHUFFLE_NONE) {
					mService.setShuffleMode(SHUFFLE_NORMAL);
					if (mService.getRepeatMode() == REPEAT_CURRENT) {
						mService.setRepeatMode(REPEAT_ALL);
						setRepeatButtonImage();
					}
					Toast.makeText(getActivity(), R.string.shuffle_on_notif, Toast.LENGTH_SHORT);
				} else if (shuffle == SHUFFLE_NORMAL) {
					mService.setShuffleMode(SHUFFLE_NONE);
					Toast.makeText(getActivity(), R.string.shuffle_off_notif, Toast.LENGTH_SHORT);
				} else {
					Log.e("MediaPlaybackActivity", "Invalid shuffle mode: " + shuffle);
				}
				setShuffleButtonImage();
			} catch (RemoteException ex) {
			}
		}

		private void updateTrackInfo() {
			if (mAlbumArtLoader != null) {
				mAlbumArtLoader.cancel(true);
			}
			mAlbumArtLoader = new AsyncAlbumArtLoader();
			mAlbumArtLoader.execute();
		}

		private class AsyncAlbumArtLoader extends AsyncTask<Void, Void, Drawable> {

			@Override
			public Drawable doInBackground(Void... params) {

				if (mService != null) {
					try {
						Bitmap bitmap = mUtils.getArtwork(mService.getAudioId(),
								mService.getAlbumId());
						if (bitmap == null) return null;
						int value = 0;
						if (bitmap.getHeight() <= bitmap.getWidth()) {
							value = bitmap.getHeight();
						} else {
							value = bitmap.getWidth();
						}
						Bitmap result = Bitmap.createBitmap(bitmap,
								(bitmap.getWidth() - value) / 2, (bitmap.getHeight() - value) / 2,
								value, value);
						return new BitmapDrawable(getResources(), result);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
				return null;
			}

			@Override
			public void onPostExecute(Drawable result) {

				if (mAlbum != null) {
					if (result != null) {
						mAlbum.setImageDrawable(result);
					} else {
						mAlbum.setImageResource(R.drawable.ic_mp_albumart_unknown);
					}
				}
			}
		}
	}

	public static class LyricsAndQueueFragment extends SherlockFragment implements
			OnValueChangeListener {

		private SliderView mVolumeSliderLeft, mVolumeSliderRight;
		private AudioManager mAudioManager;

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);

			mAudioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);

			View view = getView();

			mVolumeSliderLeft = (SliderView) view.findViewById(R.id.volume_slider_left);
			mVolumeSliderLeft.setOnValueChangeListener(this);
			mVolumeSliderLeft.setMax(mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));

			mVolumeSliderRight = (SliderView) view.findViewById(R.id.volume_slider_right);
			mVolumeSliderRight.setOnValueChangeListener(this);
			mVolumeSliderRight.setMax(mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));

			if (view.findViewById(R.id.albumart_frame) != null) {
				getFragmentManager().beginTransaction()
						.replace(R.id.albumart_frame, new AlbumArtFragment()).commit();
			}

			getFragmentManager().beginTransaction()
					.replace(R.id.lyrics_frame, new LyricsFragment()).commit();

		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View view = inflater.inflate(R.layout.playback_info, container, false);
			return view;
		}

		@Override
		public void onValueChanged(int value) {
			adjustVolume(value);
		}

		private void adjustVolume(int value) {

			int max_volume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
			int current_volume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

			if (value + current_volume <= max_volume && value + current_volume >= 0) {
				mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value + current_volume,
						AudioManager.FLAG_SHOW_UI);
			} else if (value + current_volume > max_volume) {
				mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, max_volume,
						AudioManager.FLAG_SHOW_UI);
			} else if (value + current_volume < 0) {
				mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0,
						AudioManager.FLAG_SHOW_UI);
			}

		}

	}


	@Override
	public void onFavoriteStateChanged() {
		getSherlockActivity().invalidateOptionsMenu();
	}

	@Override
	public void onMetaChanged() {
		getSherlockActivity().invalidateOptionsMenu();
		setPauseButtonImage();

	}

	@Override
	public void onPlayStateChanged() {
		setPauseButtonImage();
	}

	@Override
	public void onQueueChanged() {

	}

	@Override
	public void onRepeatModeChanged() {

	}

	@Override
	public void onShuffleModeChanged() {

	}

}
