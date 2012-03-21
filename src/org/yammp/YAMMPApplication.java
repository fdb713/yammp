package org.yammp;

import org.yammp.util.LazyImageLoader;
import org.yammp.util.MediaUtils;
import org.yammp.util.ServiceInterface;

import android.app.Application;
import android.content.res.Configuration;

public class YAMMPApplication extends Application {

	private MediaUtils mUtils;
	private ServiceInterface mServiceInterface;
	private LazyImageLoader mImageLoader;

	public LazyImageLoader getLazyImageLoader() {
		return mImageLoader;
	}

	public MediaUtils getMediaUtils() {
		return mUtils;
	}

	public ServiceInterface getServiceInterface() {
		return mServiceInterface;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mUtils = new MediaUtils(this);
		mImageLoader = new LazyImageLoader(this, R.drawable.ic_mp_albumart_unknown, getResources()
				.getDimensionPixelSize(R.dimen.album_art_size) / 2);
		mServiceInterface = new ServiceInterface(this);
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		if (mImageLoader != null) {
			mImageLoader.clearMemoryCache();
		}
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		mUtils = null;
	}

}
