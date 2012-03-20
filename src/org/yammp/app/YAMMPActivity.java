package org.yammp.app;

import org.yammp.R;
import org.yammp.YAMMPApplication;
import org.yammp.util.MediaUtils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ViewSwitcher.ViewFactory;

import com.actionbarsherlock.app.SherlockFragmentActivity;


public class YAMMPActivity extends SherlockFragmentActivity implements ViewFactory {

	private AsyncBackgroundEffect mBackgroundEffectTask;
	private ImageSwitcher mBackground;
	private MediaUtils mUtils;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mUtils = ((YAMMPApplication)getApplication()).getMediaUtils();
	}
	
	@Override
	public View makeView() {
		ImageView view = new ImageView(this);
		view.setScaleType(ImageView.ScaleType.FIT_XY);
		view.setLayoutParams(new ImageSwitcher.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT));
		return view;
	}
	
	public void setBackground(long song_id, long album_id) {
		if (mBackgroundEffectTask != null) {
			mBackgroundEffectTask.cancel(true);
		}
		mBackgroundEffectTask = new AsyncBackgroundEffect();
		mBackgroundEffectTask.execute(song_id, album_id);
	}
	
	public void setContentView(int layoutResId) {
		FrameLayout layout = new FrameLayout(this);
		mBackground = new ImageSwitcher(this);
		mBackground.setFactory(this);
		mBackground.setInAnimation(this, android.R.anim.fade_in);
		mBackground.setOutAnimation(this, android.R.anim.fade_out);
		layout.addView(mBackground, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
		layout.addView(getLayoutInflater().inflate(layoutResId, null), new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
		super.setContentView(layout);
	}
	
	private class AsyncBackgroundEffect extends AsyncTask<Long, Void, Drawable> {

		@Override
		public Drawable doInBackground(Long... params) {
			Bitmap bitmap;
			if (params == null || params.length != 2) {
				bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_music);
			} else {
				bitmap = mUtils.getArtwork(params[0], params[1]);
			}

			if (bitmap == null) {
				bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_music);
			}
			float density = getResources().getDisplayMetrics().density;
			Drawable drawable = mUtils.getBackgroundImage(bitmap,
					mBackground.getWidth(), mBackground.getHeight(), 1.0f / 32 / density);
			return drawable;
		}

		@Override
		public void onPostExecute(Drawable result) {
			if (result != null) {
				mBackground.setImageDrawable(result);
			} else {
				mBackground.setImageResource(R.drawable.ic_launcher_music);
			}
			mBackgroundEffectTask = null;
		}
	}
}
