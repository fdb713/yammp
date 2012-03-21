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
import org.yammp.IMusicPlaybackService;
import org.yammp.MusicPlaybackService;
import org.yammp.R;
import org.yammp.YAMMPApplication;
import org.yammp.util.ColorAnalyser;
import org.yammp.util.EqualizerWrapper;
import org.yammp.util.MediaUtils;
import org.yammp.util.PreferencesEditor;
import org.yammp.util.ServiceToken;
import org.yammp.util.VisualizerCompat;
import org.yammp.util.VisualizerWrapper;
import org.yammp.util.VisualizerWrapper.OnDataChangedListener;
import org.yammp.view.SliderView;
import org.yammp.view.SliderView.OnValueChangeListener;
import org.yammp.view.TouchPaintView;
import org.yammp.view.TouchPaintView.EventListener;
import org.yammp.view.VisualizerViewFftSpectrum;
import org.yammp.view.VisualizerViewWaveForm;
import org.yammp.widget.RepeatingImageButton;
import org.yammp.widget.RepeatingImageButton.RepeatListener;

import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
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
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

public class MusicPlaybackActivity extends SherlockFragmentActivity implements Constants,
		OnLongClickListener, ServiceConnection {

	private boolean mSeeking = false;

	private boolean mDeviceHasDpad;

	private long mStartSeekPos = 0;

	private long mLastSeekEventTime;

	private IMusicPlaybackService mService = null;

	private RepeatingImageButton mPrevButton;
	private ImageButton mPauseButton;
	private RepeatingImageButton mNextButton;

	private AsyncColorAnalyser mColorAnalyser;
	private ServiceToken mToken;
	private boolean mIntentDeRegistered = false;

	private PreferencesEditor mPrefs;

	private int mUIColor = Color.WHITE;
	private boolean mAutoColor = true;
	private boolean mBlurBackground = false;

	private VisualizerViewFftSpectrum mVisualizerViewFftSpectrum;
	private VisualizerViewWaveForm mVisualizerViewWaveForm;
	private boolean mDisplayVisualizer = false;
	private FrameLayout mVisualizerView;

	private VisualizerCompat mVisualizer;

	private static final int RESULT_ALBUMART_DOWNLOADED = 1;
	private boolean mShowFadeAnimation = false;
	private boolean mLyricsWakelock = DEFAULT_LYRICS_WAKELOCK;

	private TextView mTrackName, mTrackDetail;
	private TouchPaintView mTouchPaintView;
	private long mPosOverride = -1;
	private boolean mFromTouch = false;
	private long mDuration;
	private boolean paused;

	private static final int REFRESH = 1;
	private static final int QUIT = 2;

	private TextView mCurrentTime, mTotalTime;

	private OnDataChangedListener mDataChangedListener = new OnDataChangedListener() {

		@Override
		public void onFftDataChanged(byte[] data, int len) {
			if (mVisualizerViewFftSpectrum != null) {
				mVisualizerViewFftSpectrum.updateVisualizer(data);
			}
		}

		@Override
		public void onWaveDataChanged(byte[] data, int len, boolean scoop) {

			if (mVisualizerViewWaveForm != null) {
				mVisualizerViewWaveForm.updateVisualizer(data, scoop);
			}
		}

	};

	int mInitialX = -1;

	int mLastX = -1;

	int mTextWidth = 0;

	int mViewWidth = 0;
	boolean mDraggingLabel = false;
	private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {

		@Override
		public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {

			if (!fromuser || mService == null) return;
			mPosOverride = mDuration * progress / 1000;
			try {
				mService.seek(mPosOverride);
			} catch (RemoteException ex) {
			}

			refreshNow();
			// trackball event, allow progress updates
			if (!mFromTouch) {
				refreshNow();
				mPosOverride = -1;
			}
		}

		@Override
		public void onStartTrackingTouch(SeekBar bar) {

			mLastSeekEventTime = 0;
			mFromTouch = true;
			mHandler.removeMessages(REFRESH);
		}

		@Override
		public void onStopTrackingTouch(SeekBar bar) {

			mPosOverride = -1;
			mFromTouch = false;
			// Ensure that progress is properly updated in the future,
			mHandler.sendEmptyMessage(REFRESH);
		}
	};
	private EventListener mTouchPaintEventListener = new EventListener() {

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			return true;
		}

		@Override
		public boolean onTrackballEvent(MotionEvent event) {
			return true;
		}
	};
	private View.OnClickListener mPauseListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {

			doPauseResume();
		}
	};

	private View.OnClickListener mPrevListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			doPrev();
		}
	};

	private View.OnClickListener mNextListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {

			doNext();
		}
	};

	private RepeatListener mRewListener = new RepeatListener() {

		@Override
		public void onRepeat(View v, long howlong, int repcnt) {

			scanBackward(repcnt, howlong);
		}
	};

	private RepeatListener mFfwdListener = new RepeatListener() {

		@Override
		public void onRepeat(View v, long howlong, int repcnt) {

			scanForward(repcnt, howlong);
		}
	};

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

	private final Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
				case REFRESH:
					long next = refreshNow();
					queueNextRefresh(next);
					break;

				case QUIT:
					Toast.makeText(getApplicationContext(), R.string.service_start_error_msg,
							Toast.LENGTH_SHORT);
					finish();
					break;

				default:
					break;
			}
		}
	};

	private BroadcastReceiver mStatusListener = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();
			if (BROADCAST_META_CHANGED.equals(action)) {
				// redraw the artist/title info and
				// set new max for progress bar
				updateTrackInfo(mShowFadeAnimation);
				invalidateOptionsMenu();
				setPauseButtonImage();
				queueNextRefresh(1);
			} else if (BROADCAST_PLAYSTATE_CHANGED.equals(action)) {
				setPauseButtonImage();
				setVisualizerView();
			} else if (BROADCAST_FAVORITESTATE_CHANGED.equals(action)) {
				invalidateOptionsMenu();
			}
		}
	};

	private BroadcastReceiver mScreenTimeoutListener = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
				if (mIntentDeRegistered) {
					IntentFilter f = new IntentFilter();
					f.addAction(BROADCAST_PLAYSTATE_CHANGED);
					f.addAction(BROADCAST_META_CHANGED);
					f.addAction(BROADCAST_FAVORITESTATE_CHANGED);
					registerReceiver(mStatusListener, new IntentFilter(f));
					mIntentDeRegistered = false;
				}
				updateTrackInfo(false);
				if (mDisplayVisualizer) {
					enableVisualizer();
				}
				long next = refreshNow();
				queueNextRefresh(next);
				invalidateOptionsMenu();
			} else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
				paused = true;
				disableVisualizer(true);
				if (!mIntentDeRegistered) {
					mHandler.removeMessages(REFRESH);
					unregisterReceiver(mStatusListener);
					mIntentDeRegistered = true;
				}
			}
		}
	};

	private MediaUtils mUtils;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {

		super.onCreate(icicle);
		mUtils = ((YAMMPApplication) getApplication()).getMediaUtils();
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		mPrefs = new PreferencesEditor(this);
		configureActivity();

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getSupportMenuInflater().inflate(R.menu.music_playback, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		int repcnt = event.getRepeatCount();

		switch (keyCode) {

			case KeyEvent.KEYCODE_DPAD_LEFT:
				if (!useDpadMusicControl()) {
					break;
				}
				if (!mPrevButton.hasFocus()) {
					mPrevButton.requestFocus();
				}
				scanBackward(repcnt, event.getEventTime() - event.getDownTime());
				return true;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				if (!useDpadMusicControl()) {
					break;
				}
				if (!mNextButton.hasFocus()) {
					mNextButton.requestFocus();
				}
				scanForward(repcnt, event.getEventTime() - event.getDownTime());
				return true;

				// case KeyEvent.KEYCODE_R:
				// toggleRepeat();
				// return true;
				//
				// case KeyEvent.KEYCODE_S:
				// toggleShuffle();
				// return true;

			case KeyEvent.KEYCODE_N:
				if (mService != null) {
					try {
						mService.next();
						return true;
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				} else
					return false;

			case KeyEvent.KEYCODE_P:
				if (mService != null) {
					try {
						mService.prev();
						return true;
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				} else
					return false;

			case KeyEvent.KEYCODE_DPAD_CENTER:
			case KeyEvent.KEYCODE_SPACE:
				doPauseResume();
				return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {

		try {
			switch (keyCode) {
				case KeyEvent.KEYCODE_DPAD_LEFT:
					if (!useDpadMusicControl()) {
						break;
					}
					if (mService != null) {
						if (!mSeeking && mStartSeekPos >= 0) {
							mPauseButton.requestFocus();
							if (mStartSeekPos < 1000) {
								mService.prev();
							} else {
								mService.seek(0);
							}
						} else {
							scanBackward(-1, event.getEventTime() - event.getDownTime());
							mPauseButton.requestFocus();
							mStartSeekPos = -1;
						}
					}
					mSeeking = false;
					mPosOverride = -1;
					return true;
				case KeyEvent.KEYCODE_DPAD_RIGHT:
					if (!useDpadMusicControl()) {
						break;
					}
					if (mService != null) {
						if (!mSeeking && mStartSeekPos >= 0) {
							mPauseButton.requestFocus();
							mService.next();
						} else {
							scanForward(-1, event.getEventTime() - event.getDownTime());
							mPauseButton.requestFocus();
							mStartSeekPos = -1;
						}
					}
					mSeeking = false;
					mPosOverride = -1;
					return true;
			}
		} catch (RemoteException ex) {
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onLongClick(View v) {

		// TODO search media info

		String track = getTitle().toString();
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
			case GOTO_HOME:
				intent = new Intent(INTENT_MUSIC_BROWSER);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
				finish();
				break;
			case ADD_TO_FAVORITES:
				toggleFavorite();
				break;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MenuItem item = menu.findItem(EQUALIZER);
		if (item != null) {
			item.setVisible(EqualizerWrapper.isSupported());
		}
		item = menu.findItem(ADD_TO_FAVORITES);
		try {
			if (item != null && mService != null) {
				item.setIcon(mService.isFavorite(mService.getAudioId()) ? R.drawable.ic_menu_star
						: R.drawable.ic_menu_star_off);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public void onResume() {

		super.onResume();
		if (mIntentDeRegistered) {
			paused = false;
		}

		setPauseButtonImage();
	}

	@Override
	public void onServiceConnected(ComponentName classname, IBinder obj) {

		mService = IMusicPlaybackService.Stub.asInterface(obj);

		try {
			if (mService.getAudioId() >= 0 || mService.isPlaying() || mService.getPath() != null) {

				updateTrackInfo(false);
				long next = refreshNow();
				queueNextRefresh(next);
				setPauseButtonImage();
				invalidateOptionsMenu();

				mVisualizer = VisualizerWrapper.getInstance(mService.getAudioSessionId(), 50);
				mDisplayVisualizer = mPrefs.getBooleanState(KEY_DISPLAY_VISUALIZER, false);
				boolean mFftEnabled = String.valueOf(VISUALIZER_TYPE_FFT_SPECTRUM).equals(
						mPrefs.getStringPref(KEY_VISUALIZER_TYPE, "1"));
				boolean mWaveEnabled = String.valueOf(VISUALIZER_TYPE_WAVE_FORM).equals(
						mPrefs.getStringPref(KEY_VISUALIZER_TYPE, "1"));

				mVisualizerView.removeAllViews();

				if (mFftEnabled) {
					mVisualizerView.addView(mVisualizerViewFftSpectrum);
				}
				if (mWaveEnabled) {
					mVisualizerView.addView(mVisualizerViewWaveForm);
				}

				mVisualizer.setFftEnabled(mFftEnabled);
				mVisualizer.setWaveFormEnabled(mWaveEnabled);
				mVisualizer.setOnDataChangedListener(mDataChangedListener);

				setVisualizerView();

			} else {
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.setClass(getApplicationContext(), MusicBrowserActivity.class);
				startActivity(intent);
				finish();
			}

		} catch (RemoteException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onServiceDisconnected(ComponentName classname) {
		mVisualizer.release();
		mService = null;
		finish();
	}

	@Override
	public void onStart() {

		super.onStart();
		paused = false;
		mToken = mUtils.bindToService(this);
		if (mToken == null) {
			// something went wrong
			mHandler.sendEmptyMessage(QUIT);
		}
		loadPreferences();

		if (mBlurBackground) {
			getWindow().addFlags(LayoutParams.FLAG_BLUR_BEHIND);
		} else {
			getWindow().clearFlags(LayoutParams.FLAG_BLUR_BEHIND);
		}

		if (mLyricsWakelock) {
			getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
		} else {
			getWindow().clearFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
		}

		try {
			float mTransitionAnimation = Settings.System.getFloat(getContentResolver(),
					Settings.System.TRANSITION_ANIMATION_SCALE);
			if (mTransitionAnimation > 0.0) {
				mShowFadeAnimation = true;
			} else {
				mShowFadeAnimation = false;
			}

		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}

		IntentFilter f = new IntentFilter();
		f.addAction(BROADCAST_PLAYSTATE_CHANGED);
		f.addAction(BROADCAST_META_CHANGED);
		f.addAction(BROADCAST_FAVORITESTATE_CHANGED);
		registerReceiver(mStatusListener, new IntentFilter(f));

		IntentFilter s = new IntentFilter();
		s.addAction(Intent.ACTION_SCREEN_ON);
		s.addAction(Intent.ACTION_SCREEN_OFF);
		registerReceiver(mScreenTimeoutListener, new IntentFilter(s));

		long next = refreshNow();
		queueNextRefresh(next);
	}

	@Override
	public void onStop() {

		paused = true;
		if (!mIntentDeRegistered) {
			mHandler.removeMessages(REFRESH);
			unregisterReceiver(mStatusListener);
		}
		unregisterReceiver(mScreenTimeoutListener);

		getWindow().clearFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

		mUtils.unbindFromService(mToken);
		mService = null;
		super.onStop();
	}

	private void configureActivity() {

		setContentView(R.layout.music_playback);

		ActionBar mActionBar = getSupportActionBar();

		mActionBar.setCustomView(R.layout.actionbar_music_playback);
		mActionBar.setDisplayShowCustomEnabled(true);
		mActionBar.setDisplayShowTitleEnabled(false);

		View mCustomView = mActionBar.getCustomView();

		mTouchPaintView = (TouchPaintView) mCustomView.findViewById(R.id.touch_paint);
		mTouchPaintView.setEventListener(mTouchPaintEventListener);

		mTrackName = (TextView) mCustomView.findViewById(R.id.track_name);
		mTrackDetail = (TextView) mCustomView.findViewById(R.id.track_detail);

		mCurrentTime = (TextView) mCustomView.findViewById(R.id.current_time);
		mTotalTime = (TextView) mCustomView.findViewById(R.id.total_time);

		/*
		 * mAlbum.setOnClickListener(mQueueListener);
		 * mAlbum.setOnLongClickListener(mSearchAlbumArtListener);
		 */

		mPrevButton = (RepeatingImageButton) findViewById(R.id.prev);
		mPrevButton.setOnClickListener(mPrevListener);
		mPrevButton.setRepeatListener(mRewListener, 260);

		mPauseButton = (ImageButton) findViewById(R.id.pause);
		mPauseButton.requestFocus();
		mPauseButton.setOnClickListener(mPauseListener);

		mNextButton = (RepeatingImageButton) findViewById(R.id.next);
		mNextButton.setOnClickListener(mNextListener);
		mNextButton.setRepeatListener(mFfwdListener, 260);

		mDeviceHasDpad = getResources().getConfiguration().navigation == Configuration.NAVIGATION_DPAD;

		mVisualizerViewFftSpectrum = new VisualizerViewFftSpectrum(this);
		mVisualizerViewWaveForm = new VisualizerViewWaveForm(this);
		mVisualizerView = (FrameLayout) findViewById(R.id.visualizer_view);

		if (findViewById(R.id.albumart_frame) != null) {
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.albumart_frame, new AlbumArtFragment()).commit();
		}

		getSupportFragmentManager().beginTransaction()
				.replace(R.id.playback_frame, new LyricsAndQueueFragment()).commit();

	}

	private void disableVisualizer(boolean animation) {
		if (mVisualizer != null) {
			if (mVisualizerView.getVisibility() == View.VISIBLE) {
				mVisualizerView.setVisibility(View.INVISIBLE);
				if (mShowFadeAnimation) {
					mVisualizerView.startAnimation(AnimationUtils.loadAnimation(this,
							android.R.anim.fade_out));
				}
				if (animation) {
					mVisualizerHandler.sendEmptyMessageDelayed(DISABLE_VISUALIZER, AnimationUtils
							.loadAnimation(this, android.R.anim.fade_out).getDuration());
				} else {
					mVisualizerHandler.sendEmptyMessage(DISABLE_VISUALIZER);
				}

			}
		}

	}

	private void doNext() {

		if (mService == null) return;
		try {
			mService.next();
		} catch (RemoteException ex) {
		}
	}

	private void doPauseResume() {

		try {
			if (mService != null) {
				if (mService.isPlaying()) {
					mService.pause();
				} else {
					mService.play();
				}
				refreshNow();
				setPauseButtonImage();
			}
		} catch (RemoteException ex) {
		}
	}

	private void doPrev() {

		if (mService == null) return;
		try {
			if (mService.position() < 2000) {
				mService.prev();
			} else {
				mService.seek(0);
				mService.play();
			}
		} catch (RemoteException ex) {
		}
	}

	private void enableVisualizer() {
		if (mVisualizer != null) {
			if (mVisualizerView.getVisibility() != View.VISIBLE) {
				mVisualizerView.setVisibility(View.VISIBLE);
				mVisualizerHandler.sendEmptyMessage(ENABLE_VISUALIZER);
				if (mShowFadeAnimation) {
					mVisualizerView.startAnimation(AnimationUtils.loadAnimation(this,
							android.R.anim.fade_in));
				}
			}
		}
	}

	private void loadPreferences() {

		mLyricsWakelock = mPrefs.getBooleanPref(KEY_LYRICS_WAKELOCK, DEFAULT_LYRICS_WAKELOCK);
		mAutoColor = mPrefs.getBooleanPref(KEY_AUTO_COLOR, true);
		mBlurBackground = mPrefs.getBooleanPref(KEY_BLUR_BACKGROUND, false);
	}

	private void queueNextRefresh(long delay) {

		if (!paused && !mFromTouch) {
			Message msg = mHandler.obtainMessage(REFRESH);
			mHandler.removeMessages(REFRESH);
			mHandler.sendMessageDelayed(msg, delay);
		}
	}

	private long refreshNow() {
		if (mService == null) return 500;
		try {
			long pos = mPosOverride < 0 ? mService.position() : mPosOverride;
			long remaining = 1000 - pos % 1000;
			if (pos >= 0 && mDuration > 0) {
				mCurrentTime.setText(mUtils.makeTimeString(pos / 1000));

				if (mService.isPlaying()) {
					mCurrentTime.setVisibility(View.VISIBLE);
				} else {
					// blink the counter
					// If the progress bar is still been dragged, then we do not
					// want to blink the
					// currentTime. It would cause flickering due to change in
					// the visibility.
					if (mFromTouch) {
						mCurrentTime.setVisibility(View.VISIBLE);
					} else {
						int vis = mCurrentTime.getVisibility();
						mCurrentTime.setVisibility(vis == View.INVISIBLE ? View.VISIBLE
								: View.INVISIBLE);
					}
					remaining = 500;
				}

				// Normalize our progress along the progress bar's scale
				setSupportProgress((int) ((Window.PROGRESS_END - Window.PROGRESS_START) * pos / mDuration));
			} else {
				mCurrentTime.setText("--:--");
				setSupportProgress(Window.PROGRESS_END - Window.PROGRESS_START);
			}
			// return the number of milliseconds until the next full second, so
			// the counter can be updated at just the right time
			return remaining;
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return 500;
	}

	private void scanBackward(int repcnt, long delta) {

		if (mService == null) return;
		try {
			if (repcnt == 0) {
				mStartSeekPos = mService.position();
				mLastSeekEventTime = 0;
				mSeeking = false;
			} else {
				mSeeking = true;
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
					mService.prev();
					long duration = mService.duration();
					mStartSeekPos += duration;
					newpos += duration;
				}
				if (delta - mLastSeekEventTime > 250 || repcnt < 0) {
					mService.seek(newpos);
					mLastSeekEventTime = delta;
				}
				if (repcnt >= 0) {
					mPosOverride = newpos;
				} else {
					mPosOverride = -1;
				}
				refreshNow();
			}
		} catch (RemoteException ex) {
		}
	}

	private void scanForward(int repcnt, long delta) {

		if (mService == null) return;
		try {
			if (repcnt == 0) {
				mStartSeekPos = mService.position();
				mLastSeekEventTime = 0;
				mSeeking = false;
			} else {
				mSeeking = true;
				if (delta < 5000) {
					// seek at 10x speed for the first 5 seconds
					delta = delta * 10;
				} else {
					// seek at 40x after that
					delta = 50000 + (delta - 5000) * 40;
				}
				long newpos = mStartSeekPos + delta;
				long duration = mService.duration();
				if (newpos >= duration) {
					// move to next track
					mService.next();
					mStartSeekPos -= duration; // is OK to go negative
					newpos -= duration;
				}
				if (delta - mLastSeekEventTime > 250 || repcnt < 0) {
					mService.seek(newpos);
					mLastSeekEventTime = delta;
				}
				if (repcnt >= 0) {
					mPosOverride = newpos;
				} else {
					mPosOverride = -1;
				}
				refreshNow();
			}
		} catch (RemoteException ex) {
		}
	}

	private void setPauseButtonImage() {

		try {
			if (mService != null && mService.isPlaying()) {
				mPauseButton.setImageResource(R.drawable.btn_playback_ic_pause);
			} else {
				mPauseButton.setImageResource(R.drawable.btn_playback_ic_play);
			}
		} catch (RemoteException ex) {
		}
	}

	private void setUIColor(int color) {

		mVisualizerViewFftSpectrum.setColor(color);
		mVisualizerViewWaveForm.setColor(color);
		mTouchPaintView.setColor(color);
	}

	private void setVisualizerView() {
		try {
			if (mService != null && mService.isPlaying() && mDisplayVisualizer) {
				enableVisualizer();
			} else {
				disableVisualizer(false);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void toggleFavorite() {

		if (mService == null) return;
		try {
			mService.toggleFavorite();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void updateTrackInfo(boolean animation) {

		if (mService == null) {
			finish();
			return;
		}
		try {
			mTrackName.setText(mService.getTrackName());

			if (mService.getArtistName() != null
					&& !MediaStore.UNKNOWN_STRING.equals(mService.getArtistName())) {
				mTrackDetail.setText(mService.getArtistName());
			} else if (mService.getAlbumName() != null
					&& !MediaStore.UNKNOWN_STRING.equals(mService.getAlbumName())) {
				mTrackDetail.setText(mService.getAlbumName());
			} else {
				mTrackDetail.setText(R.string.unknown_artist);
			}

			if (mColorAnalyser != null) {
				mColorAnalyser.cancel(true);
			}
			mColorAnalyser = new AsyncColorAnalyser();
			mColorAnalyser.execute();

			mDuration = mService.duration();
			mTotalTime.setText(mUtils.makeTimeString(mDuration / 1000));
		} catch (RemoteException e) {
			e.printStackTrace();
			finish();
		}
	}

	private boolean useDpadMusicControl() {

		if (mDeviceHasDpad
				&& (mPrevButton.isFocused() || mNextButton.isFocused() || mPauseButton.isFocused()))
			return true;
		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

		if (resultCode != RESULT_OK) return;
		switch (requestCode) {
			case DELETE_ITEMS:
				if (resultCode == RESULT_DELETE_MUSIC) {
					finish();
				}
				break;
		}
	}

	public static class AlbumArtFragment extends SherlockFragment implements
			ViewSwitcher.ViewFactory, OnClickListener, ServiceConnection {

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

		private void setUIColor(int color) {
			mVolumeSliderRight.setColor(color);
			mVolumeSliderLeft.setColor(color);
		}
	}

	private class AsyncColorAnalyser extends AsyncTask<Void, Void, Integer> {

		@Override
		protected Integer doInBackground(Void... params) {

			if (mService != null) {
				try {
					if (mAutoColor) {
						mUIColor = ColorAnalyser.analyse(mUtils.getArtwork(mService.getAudioId(),
								mService.getAlbumId()));
					} else {
						mUIColor = mPrefs.getIntPref(KEY_CUSTOMIZED_COLOR, Color.WHITE);
					}
					return mUIColor;
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			return Color.WHITE;
		}

		@Override
		protected void onPostExecute(Integer result) {

			setUIColor(mUIColor);
		}
	}

}
