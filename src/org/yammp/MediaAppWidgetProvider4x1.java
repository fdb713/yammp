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

import org.yammp.app.MusicBrowserActivity;
import org.yammp.app.MusicPlaybackActivity;
import org.yammp.util.MusicUtils;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import android.widget.RemoteViews;

/**
 * Simple widget to show currently playing album art along with play/pause and
 * next track buttons.
 */
public class MediaAppWidgetProvider4x1 extends AppWidgetProvider implements Constants {

	static final String TAG = "MusicAppWidgetProvider";

	public static final String CMDAPPWIDGETUPDATE = "appwidgetupdate";

	private static MediaAppWidgetProvider4x1 sInstance;

	static synchronized MediaAppWidgetProvider4x1 getInstance() {

		if (sInstance == null) {
			sInstance = new MediaAppWidgetProvider4x1();
		}
		return sInstance;
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

		defaultAppWidget(context, appWidgetIds);

		// Send broadcast intent to any running MediaPlaybackService so it can
		// wrap around with an immediate update.
		Intent updateIntent = new Intent(MusicPlaybackService.SERVICECMD);
		updateIntent.putExtra(MusicPlaybackService.CMDNAME,
				MediaAppWidgetProvider4x1.CMDAPPWIDGETUPDATE);
		updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
		updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
		context.sendBroadcast(updateIntent);
	}

	/**
	 * Initialize given widgets to default state, where we launch Music on
	 * default click and hide actions if service not running.
	 */
	private void defaultAppWidget(Context context, int[] appWidgetIds) {

		final Resources res = context.getResources();
		final RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.album_appwidget4x1);

		views.setViewVisibility(R.id.title, View.GONE);
		views.setTextViewText(R.id.artist, res.getText(R.string.widget_initial_text));
		views.setViewVisibility(R.id.albumart, View.GONE);

		linkButtons(context, views, false /* not playing */);
		pushUpdate(context, appWidgetIds, views);
	}

	private void pushUpdate(Context context, int[] appWidgetIds, RemoteViews views) {

		// Update specific list of appWidgetIds if given, otherwise default to
		// all
		final AppWidgetManager gm = AppWidgetManager.getInstance(context);
		if (appWidgetIds != null) {
			gm.updateAppWidget(appWidgetIds, views);
		} else {
			gm.updateAppWidget(new ComponentName(context, this.getClass()), views);
		}
	}

	/**
	 * Check against {@link AppWidgetManager} if there are any instances of this
	 * widget.
	 */
	private boolean hasInstances(Context context) {

		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, this
				.getClass()));
		return (appWidgetIds.length > 0);
	}

	/**
	 * Handle a change notification coming over from
	 * {@link MusicPlaybackService}
	 */
	void notifyChange(MusicPlaybackService service, String what) {

		if (hasInstances(service)) {
			if (BROADCAST_META_CHANGED.equals(what) || BROADCAST_PLAYSTATE_CHANGED.equals(what)) {
				performUpdate(service, null);
			}
		}
	}

	/**
	 * Update all active widget instances by pushing changes
	 */
	void performUpdate(MusicPlaybackService service, int[] appWidgetIds) {

		final Resources res = service.getResources();
		final RemoteViews views = new RemoteViews(service.getPackageName(),
				R.layout.album_appwidget4x1);

		CharSequence titleName = service.getTrackName();
		CharSequence artistName = service.getArtistName();
		long albumId = service.getAlbumId();
		long songId = service.getAudioId();
		CharSequence errorState = null;

		// Format title string with track number, or show SD card message
		String status = Environment.getExternalStorageState();
		if (status.equals(Environment.MEDIA_SHARED) || status.equals(Environment.MEDIA_UNMOUNTED)) {
			errorState = res.getText(R.string.sdcard_busy_title);
		} else if (status.equals(Environment.MEDIA_REMOVED)) {
			errorState = res.getText(R.string.sdcard_missing_title);
		} else if (titleName == null) {
			errorState = res.getText(R.string.emptyplaylist);
		}

		if (errorState != null) {
			// Show error state to user
			views.setViewVisibility(R.id.title, View.GONE);
			views.setTextViewText(R.id.artist, errorState);
			views.setViewVisibility(R.id.albumart, View.GONE);
		} else {
			// No error, so show normal titles and artwork
			views.setViewVisibility(R.id.title, View.VISIBLE);
			views.setTextViewText(R.id.title, titleName);
			views.setTextViewText(R.id.artist, artistName);
			views.setViewVisibility(R.id.albumart, View.VISIBLE);
			// Set album art
			Uri uri = MusicUtils.getArtworkUri(service, songId, albumId);
			if (uri != null) {
				views.setImageViewUri(R.id.albumart, uri);
			} else {
				views.setImageViewResource(R.id.albumart, R.drawable.ic_mp_albumart_unknown);
			}
		}

		// Set correct drawable for pause state
		final boolean playing = service.isPlaying();
		if (playing) {
			views.setImageViewResource(R.id.control_play, R.drawable.btn_playback_ic_pause);
		} else {
			views.setImageViewResource(R.id.control_play, R.drawable.btn_playback_ic_play);
		}

		// Link actions buttons to intents
		linkButtons(service, views, playing);

		pushUpdate(service, appWidgetIds, views);
	}

	/**
	 * Link up various button actions using {@link PendingIntents}.
	 * 
	 * @param playerActive
	 *            True if player is active in background, which means widget
	 *            click will launch {@link MusicPlaybackActivity}, otherwise we
	 *            launch {@link MusicBrowserActivity}.
	 */
	private void linkButtons(Context context, RemoteViews views, boolean playerActive) {

		// Connect up various buttons and touch events
		Intent intent;
		PendingIntent pendingIntent;

		final ComponentName serviceName = new ComponentName(context, MusicPlaybackService.class);

		if (playerActive) {
			intent = new Intent(context, MusicPlaybackActivity.class);
			pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
			views.setOnClickPendingIntent(R.id.album_appwidget, pendingIntent);
		} else {
			intent = new Intent(context, MusicBrowserActivity.class);
			pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
			views.setOnClickPendingIntent(R.id.album_appwidget, pendingIntent);
		}

		intent = new Intent(TOGGLEPAUSE_ACTION);
		intent.setComponent(serviceName);
		pendingIntent = PendingIntent.getService(context, 0, intent, 0);
		views.setOnClickPendingIntent(R.id.control_play, pendingIntent);

		intent = new Intent(NEXT_ACTION);
		intent.setComponent(serviceName);
		pendingIntent = PendingIntent.getService(context, 0, intent, 0);
		views.setOnClickPendingIntent(R.id.control_next, pendingIntent);
	}
}
