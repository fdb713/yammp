package org.yammp.util;

import org.yammp.IMusicPlaybackService;

import android.os.RemoteException;


public class ServiceInterface {

	private IMusicPlaybackService mService;
	
	public ServiceInterface(IMusicPlaybackService service) {
		mService = service;
	}
	
	public void play() {
		try {
			mService.play();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
}
