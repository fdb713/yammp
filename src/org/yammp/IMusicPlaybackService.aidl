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

package org.yammp;

import android.graphics.Bitmap;
import android.net.Uri;

interface IMusicPlaybackService {

	void reloadLyrics();
	void refreshLyrics();
	void openFile(String path);
	void open(in long [] list, int position);
	int getQueuePosition();
	boolean isPlaying();
	void stop();
	void pause();
	void play();
	void prev();
	void next();
	void cycleRepeat();
	void toggleShuffle();
	long duration();
	long position();
	long seek(long pos);
	String getTrackName();
	String getMediaPath();
	String getAlbumName();
	long getAlbumId();
	String getArtistName();
	long getArtistId();
	void enqueue(in long [] list, int action);
	long [] getQueue();
	void moveQueueItem(int from, int to);
	void setQueuePosition(int index);
	void setQueueId(long id);
	String getPath();
	long getAudioId();
	Bitmap getAlbumArt();
	Uri getArtworkUri();
	String [] getLyrics();
	int getLyricsStatus();
	int getCurrentLyricsId();
	long getPositionByLyricsId(int id);
	void setShuffleMode(int shufflemode);
	int getShuffleMode();
	int removeTracks(int first, int last);
	int removeTrack(long id);
	void setRepeatMode(int repeatmode);
	int getRepeatMode();
	int getMediaMountedCount();
	int getAudioSessionId();
	void startSleepTimer(long millisecond, boolean gentle);
	void stopSleepTimer();
	long getSleepTimerRemained();
	void reloadSettings();
	void reloadEqualizer();
	void toggleFavorite();
	void addToFavorites(long id);
	void removeFromFavorites(long id);
	boolean isFavorite(long id);

}