package org.yammp.fragment;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.xmlpull.v1.XmlPullParserException;
import org.yammp.Constants;
import org.yammp.R;
import org.yammp.util.LyricsDownloader;
import org.yammp.util.LyricsDownloader.SearchResultItem;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;

public class LyricsSearchFragment extends SherlockListFragment implements Constants,
		OnClickListener, TextWatcher {

	private LyricsDownloader mDownloader = new LyricsDownloader();
	private SearchResultAdapter mAdapter;
	private EditText mTrackName, mArtistName;
	private Button mSearchButton;
	private String mTrack, mArtist, mPath;
	private ProgressBar mProgress;
	private LyricsSearchTask mSearchTask;

	@Override
	public void afterTextChanged(Editable s) {

	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Bundle args = savedInstanceState == null ? getArguments() : savedInstanceState;
		mTrack = args.getString(INTENT_KEY_TRACK);
		mArtist = args.getString(INTENT_KEY_ARTIST);
		mPath = args.getString(INTENT_KEY_PATH);

		View view = getView();
		mTrackName = (EditText) view.findViewById(R.id.track_name);
		mTrackName.addTextChangedListener(this);
		mTrackName.setText(mTrack);
		mArtistName = (EditText) view.findViewById(R.id.artist_name);
		mArtistName.setText(mArtist);
		mSearchButton = (Button) view.findViewById(R.id.search);
		mSearchButton.setOnClickListener(this);
		mProgress = (ProgressBar) view.findViewById(R.id.progress);
		// mAdapter = new SearchResultAdapter(getSherlockActivity(), null);
		// setListAdapter(mAdapter);

	}

	@Override
	public void onClick(View view) {
		if (mSearchTask != null) {
			mSearchTask.cancel(true);
		} else {
			mSearchTask = new LyricsSearchTask(mTrackName.getText().toString(), mArtistName
					.getText().toString());
			mSearchTask.execute();
		}

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.search_lyrics, container, false);
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		if (mSearchButton != null) {
			mSearchButton.setEnabled(s != null
					&& s.toString().trim().replaceAll("[\\p{P} ]", "").length() > 0);
		}
	}

	private class LyricsSearchTask extends AsyncTask<Void, Void, SearchResultItem[]> {

		private String title, artist;

		public LyricsSearchTask(String title, String artist) {
			this.title = title;
			this.artist = artist;
		}

		@Override
		public SearchResultItem[] doInBackground(Void... params) {

			try {
				return mDownloader.search(artist, title);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (XmlPullParserException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public void onPostExecute(SearchResultItem[] result) {
			if (result == null) {

			} else if (result.length > 0) {
				mAdapter = new SearchResultAdapter(getSherlockActivity(), result);
				setListAdapter(mAdapter);
			} else {
				Toast.makeText(getSherlockActivity(), R.string.search_noresult, Toast.LENGTH_SHORT)
						.show();
			}
			mSearchTask = null;
			mSearchButton.setText(android.R.string.search_go);
			mProgress.setVisibility(View.GONE);
		}

		@Override
		public void onPreExecute() {
			mSearchButton.setText(android.R.string.cancel);
			mProgress.setVisibility(View.VISIBLE);
		}

	}

	private class SearchResultAdapter extends ArrayAdapter<SearchResultItem> {

		public SearchResultAdapter(Context context, SearchResultItem[] objects) {
			super(context, android.R.layout.two_line_list_item, android.R.id.text1, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			TextView title = (TextView) view.findViewById(android.R.id.text1);
			TextView artist = (TextView) view.findViewById(android.R.id.text2);
			title.setSingleLine(true);
			title.setText(getItem(position).title);
			artist.setSingleLine(true);
			artist.setText(getItem(position).artist);
			return view;
		}

	}
}
