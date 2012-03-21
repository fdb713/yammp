package org.yammp.util;

import java.util.ArrayList;
import java.util.List;

import org.yammp.Constants;
import org.yammp.IMusicPlaybackService;
import org.yammp.YAMMPApplication;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;

public class ServiceInterface implements Constants, ServiceConnection {

	private IMusicPlaybackService mService;

	private BroadcastReceiver mMediaStatusReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BROADCAST_META_CHANGED.equals(action)) {
				for (MediaStateListener listener : mListeners) {
					listener.onMetaChanged();
				}
			} else if (BROADCAST_PLAYSTATE_CHANGED.equals(action)) {
				for (MediaStateListener listener : mListeners) {
					listener.onPlayStateChanged();
				}
			} else if (BROADCAST_FAVORITESTATE_CHANGED.equals(action)) {
				for (MediaStateListener listener : mListeners) {
					listener.onPlayStateChanged();
				}
			} else if (BROADCAST_SHUFFLEMODE_CHANGED.equals(action)) {
				for (MediaStateListener listener : mListeners) {
					listener.onShuffleModeChanged();
				}
			} else if (BROADCAST_REPEATMODE_CHANGED.equals(action)) {
				for (MediaStateListener listener : mListeners) {
					listener.onRepeatModeChanged();
				}
			} else if (BROADCAST_QUEUE_CHANGED.equals(action)) {
				for (MediaStateListener listener : mListeners) {
					listener.onQueueChanged();
				}
			}

		}

	};

	private List<MediaStateListener> mListeners = new ArrayList<MediaStateListener>();

	public ServiceInterface(Context context) {
		((YAMMPApplication) context.getApplicationContext()).getMediaUtils().bindToService(this);
		IntentFilter filter = new IntentFilter();
		filter.addAction(BROADCAST_PLAYSTATE_CHANGED);
		filter.addAction(BROADCAST_META_CHANGED);
		filter.addAction(BROADCAST_FAVORITESTATE_CHANGED);
		filter.addAction(BROADCAST_SHUFFLEMODE_CHANGED);
		filter.addAction(BROADCAST_REPEATMODE_CHANGED);
		filter.addAction(BROADCAST_QUEUE_CHANGED);
		context.registerReceiver(mMediaStatusReceiver, filter);
	}

	public void addMediaStateListener(MediaStateListener listener) {
		mListeners.add(listener);
	}

	public void addToFavorites(long id) {
		if (mService == null) return;
		try {
			mService.addToFavorites(id);
		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}

	public long duration() {
		if (mService == null) return 0;
		try {
			return mService.duration();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return 0;
	}

	public void enqueue(long[] list, int action) {
		if (mService == null) return;
		try {
			mService.enqueue(list, action);
		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}

	public int eqGetBand(int frequency) {
		if (mService == null) return 0;
		try {
			return mService.eqGetBand(frequency);
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return 0;
	}

	public int[] eqGetBandFreqRange(int band) {
		if (mService == null) return null;
		try {
			return mService.eqGetBandFreqRange((short) band);
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return null;
	}

	public int eqGetBandLevel(int band) {
		if (mService == null) return 0;
		try {
			return mService.eqGetBandLevel(band);
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return 0;
	}

	public int[] eqGetBandLevelRange() {
		if (mService == null) return null;
		try {
			return mService.eqGetBandLevelRange();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return null;
	}

	public int eqGetCenterFreq(int band) {
		if (mService == null) return 0;
		try {
			return mService.eqGetCenterFreq((short) band);
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return 0;
	}

	public int eqGetCurrentPreset() {
		if (mService == null) return 0;
		try {
			return mService.eqGetCurrentPreset();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return 0;
	}

	public int eqGetNumberOfBands() {
		if (mService == null) return 0;
		try {
			return mService.eqGetNumberOfBands();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return 0;
	}

	public int eqGetNumberOfPresets() {
		if (mService == null) return 0;
		try {
			return mService.eqGetNumberOfPresets();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return 0;
	}

	public String eqGetPresetName(int preset) {
		if (mService == null) return null;
		try {
			return mService.eqGetPresetName((short) preset);
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return null;
	}

	public void eqRelease() {
		if (mService == null) return;
		try {
			mService.eqRelease();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}

	public void eqReset() {
		if (mService == null) return;
		try {
			mService.eqReset();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}

	public void eqSetBandLevel(int band, int level) {
		if (mService == null) return;
		try {
			mService.eqSetBandLevel((short) band, (short) level);
		} catch (RemoteException e) {

			e.printStackTrace();
		}

	}

	public int eqSetEnabled(boolean enabled) {
		if (mService == null) return 0;
		try {
			return mService.eqSetEnabled(enabled);
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return 0;
	}

	public void eqUsePreset(int preset) {
		if (mService == null) return;
		try {
			mService.eqUsePreset((short) preset);
		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}

	public long getAlbumId() {
		if (mService == null) return 0;
		try {
			return mService.getAlbumId();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return -1;
	}

	public String getAlbumName() {
		if (mService == null) return null;
		try {
			return mService.getAlbumName();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return null;
	}

	public long getArtistId() {
		if (mService == null) return 0;
		try {
			return mService.getArtistId();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return -1;
	}

	public String getArtistName() {
		if (mService == null) return null;
		try {
			return mService.getArtistName();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return null;
	}

	public Uri getArtworkUri() {
		if (mService == null) return null;
		try {
			return mService.getArtworkUri();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return null;
	}

	public long getAudioId() {
		if (mService == null) return 0;
		try {
			return mService.getAudioId();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return 0;
	}

	public int getAudioSessionId() {
		if (mService == null) return 0;
		try {
			return mService.getAudioSessionId();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return 0;
	}

	public int getCurrentLyricsId() {
		if (mService == null) return 0;
		try {
			return mService.getCurrentLyricsId();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return 0;
	}

	public String[] getLyrics() {
		if (mService == null) return null;
		try {
			return mService.getLyrics();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return null;
	}

	public int getLyricsStatus() {
		if (mService == null) return 0;
		try {
			return mService.getLyricsStatus();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return 0;
	}

	public int getMediaMountedCount() {
		if (mService == null) return 0;
		try {
			return mService.getMediaMountedCount();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return 0;
	}

	public String getMediaPath() {
		if (mService == null) return null;
		try {
			return mService.getMediaPath();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return null;
	}

	public String getPath() {
		if (mService == null) return null;
		try {
			return mService.getPath();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return null;
	}

	public long getPositionByLyricsId(int id) {
		if (mService == null) return 0;
		try {
			return mService.getPositionByLyricsId(id);
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return 0;
	}

	public long[] getQueue() {
		if (mService == null) return null;
		try {
			return mService.getQueue();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return null;
	}

	public int getQueuePosition() {
		if (mService == null) return 0;
		try {
			return mService.getQueuePosition();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return 0;
	}

	public int getRepeatMode() {
		if (mService == null) return 0;
		try {
			return mService.getRepeatMode();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return 0;
	}

	public int getShuffleMode() {
		if (mService == null) return 0;
		try {
			return mService.getShuffleMode();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return 0;
	}

	public long getSleepTimerRemained() {
		if (mService == null) return 0;
		try {
			return mService.getSleepTimerRemained();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return 0;
	}

	public String getTrackName() {
		if (mService == null) return null;
		try {
			return mService.getTrackName();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return null;
	}

	public boolean isFavorite(long id) {
		if (mService == null) return false;
		try {
			return mService.isFavorite(id);
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return false;
	}

	public boolean isPlaying() {
		if (mService == null) return false;
		try {
			return mService.isPlaying();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return false;
	}

	public void moveQueueItem(int from, int to) {
		if (mService == null) return;
		try {
			mService.moveQueueItem(from, to);
		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}

	public void next() {
		if (mService == null) return;
		try {
			mService.next();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}

	@Override
	public void onServiceConnected(ComponentName service, IBinder obj) {
		mService = IMusicPlaybackService.Stub.asInterface(obj);
	}

	@Override
	public void onServiceDisconnected(ComponentName service) {
		mService = null;
	}

	public void open(long[] list, int position) {
		if (mService == null) return;
		try {
			mService.open(list, position);
		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}

	public void openFile(String path) {
		if (mService == null) return;
		try {
			mService.openFile(path);
		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}

	public void pause() {
		if (mService == null) return;
		try {
			mService.pause();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}

	public void play() {
		if (mService == null) return;
		try {
			mService.play();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}

	public long position() {
		if (mService == null) return 0;
		try {
			return mService.position();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return 0;
	}

	public void prev() {
		if (mService == null) return;
		try {
			mService.prev();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}

	public void refreshLyrics() {
		if (mService == null) return;
		try {
			mService.refreshLyrics();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}

	public void reloadEqualizer() {
		if (mService == null) return;
		try {
			mService.reloadEqualizer();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}

	public void reloadLyrics() {
		if (mService == null) return;
		try {
			mService.reloadLyrics();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}

	public void reloadSettings() {
		if (mService == null) return;
		try {
			mService.reloadSettings();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}

	public void removeFromFavorites(long id) {
		if (mService == null) return;
		try {
			mService.removeFromFavorites(id);
		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}

	public void removeMediaStateListener(MediaStateListener listener) {
		mListeners.remove(listener);
	}

	public int removeTrack(long id) {
		if (mService == null) return 0;
		try {
			return mService.removeTrack(id);
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return 0;
	}

	public int removeTracks(int first, int last) {
		if (mService == null) return 0;
		try {
			return mService.removeTracks(first, last);
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return 0;
	}

	public long seek(long pos) {
		if (mService == null) return pos;
		try {
			return mService.seek(pos);
		} catch (RemoteException e) {

			e.printStackTrace();
		}
		return pos;
	}

	public void setQueueId(long id) {
		if (mService == null) return;
		try {
			mService.setQueueId(id);
		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}

	public void setQueuePosition(int index) {
		if (mService == null) return;
		try {
			mService.setQueuePosition(index);
		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}

	public void setRepeatMode(int repeatmode) {
		if (mService == null) return;
		try {
			mService.setRepeatMode(repeatmode);
		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}

	public void setShuffleMode(int shufflemode) {
		if (mService == null) return;
		try {
			mService.setShuffleMode(shufflemode);
		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}

	public void startSleepTimer(long milliseconds, boolean gentle) {
		if (mService == null) return;
		try {
			mService.startSleepTimer(milliseconds, gentle);
		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}

	public void stop() {
		if (mService == null) return;

		try {
			mService.stop();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}

	public void stopSleepTimer() {
		if (mService == null) return;

		try {
			mService.stopSleepTimer();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}

	public void toggleFavorite() {
		if (mService == null) return;
		try {
			mService.toggleFavorite();
		} catch (RemoteException e) {

			e.printStackTrace();
		}
	}

	public boolean togglePause() {
		if (mService == null) return false;
		try {
			return mService.togglePause();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void toggleRepeat() {
		if (mService == null) return;
		try {
			mService.toggleRepeat();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void toggleShuffle() {
		if (mService == null) return;
		try {
			mService.toggleShuffle();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public interface MediaStateListener {

		void onFavoriteStateChanged();

		void onMetaChanged();

		void onPlayStateChanged();

		void onQueueChanged();

		void onRepeatModeChanged();

		void onShuffleModeChanged();
	}
}
