package org.yammp.dialog;

import org.yammp.R;
import org.yammp.YAMMPApplication;
import org.yammp.util.MediaUtils;

import com.actionbarsherlock.app.SherlockDialogFragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

public class DeleteDialogFragment extends SherlockDialogFragment implements OnClickListener {

	private long[] mItems = new long[] {};
	private MediaUtils mUtils;
	private static boolean mIsDeleteLyrics = false;
	private static long mId;
	private static int mType;
	private static String mName;
	private static String mMessage;
	public final static int ARTIST = 1;
	public final static int ALBUM = 2;
	public final static int TRACK = 3;

	public static DeleteDialogFragment getInstance(boolean is_delete_lyrics, long id, int type) {
		mId = id;
		mIsDeleteLyrics = is_delete_lyrics;
		mType = type;
		return new DeleteDialogFragment();
	}

	@Override
	public void onActivityCreated(Bundle saveInstanceState) {
		super.onActivityCreated(saveInstanceState);
		mUtils = ((YAMMPApplication)getSherlockActivity().getApplication()).getMediaUtils();
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which) {

		switch (which) {
			case DialogInterface.BUTTON_POSITIVE:
				if (mIsDeleteLyrics) {
					mUtils.deleteLyrics(mItems);
				} else {
					mUtils.deleteTracks(mItems);
				}
				break;
		}

	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		switch (mType) {
			case ARTIST:
				mItems = new long[] { mId };
				mName = mUtils.getTrackName(mId);
				mMessage = getString(mIsDeleteLyrics ? R.string.delete_song_lyrics
						: R.string.delete_song_track, mName);
				break;
			case ALBUM:
				mItems = mUtils.getSongListForAlbum(mId);
				mName = mUtils.getAlbumName(mId, true);
				mMessage = getString(mIsDeleteLyrics ? R.string.delete_album_lyrics
						: R.string.delete_album_tracks, mName);
				break;
			case TRACK:
				mItems = mUtils.getSongListForArtist(mId);
				mName = mUtils.getArtistName(mId, true);
				mMessage = getString(mIsDeleteLyrics ? R.string.delete_artist_lyrics
						: R.string.delete_artist_tracks, mName);
				break;
		}

		return new AlertDialog.Builder(getSherlockActivity()) {

			{
				setIcon(android.R.drawable.ic_dialog_alert);
				setTitle(R.string.delete);
				setMessage(mMessage);
				setNegativeButton(android.R.string.cancel, DeleteDialogFragment.this);
				setPositiveButton(android.R.string.ok, DeleteDialogFragment.this);
			}
		}.create();

	}

}
