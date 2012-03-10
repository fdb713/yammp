package org.yammp.util;

public class VisualizerWrapper {

	public static VisualizerCompat getInstance(int audioSessionId, int fps) {
		if (isAudioFXSupported())
			return new VisualizerCompatAudioFX(audioSessionId, fps);
		else if (isScoopSupported()) return new VisualizerCompatScoop(audioSessionId, fps);
		return null;
	}

	private static boolean isAudioFXSupported() {
		try {
			Class.forName("android.media.audiofx.Visualizer");
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	private static boolean isScoopSupported() {
		try {
			Class.forName("android.media.MediaPlayer").getMethod("snoop", short[].class, int.class);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public interface OnDataChangedListener {

		public void onFftDataChanged(byte[] data, int len);

		public void onWaveDataChanged(byte[] data, int len, boolean scoop);
	}

}
