package org.yammp;

import org.yammp.util.MediaUtils;
import org.yammp.util.ServiceInterface;
import org.yammp.util.ServiceToken;

import android.app.Application;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.IBinder;


public class YAMMPApplication extends Application implements ServiceConnection{

	private MediaUtils mUtils;
	private ServiceInterface mServiceInterface;
	private ServiceToken mToken;
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
	
	@Override
	public void onTerminate() {
		super.onTerminate();
		mUtils.unbindFromService(mToken);
		mUtils = null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		mUtils = new MediaUtils(this);
		mToken = mUtils.bindToService(this);
	}
	
	public MediaUtils getMediaUtils() {
		return mUtils;
	}
	
	public ServiceInterface getServiceInterface() {
		return mServiceInterface;
	}

	@Override
	public void onServiceConnected(ComponentName service, IBinder obj) {
		IMusicPlaybackService mService = IMusicPlaybackService.Stub.asInterface(obj);
		mServiceInterface = new ServiceInterface(mService);
	}

	@Override
	public void onServiceDisconnected(ComponentName service) {
		mServiceInterface = null;
	}
	
}
