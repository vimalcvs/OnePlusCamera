package camera.vimal.vk;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

class AudioListener {
	private static final String TAG = "AudioListener";
	private volatile boolean is_running = true;
	private int buffer_size = -1;
	private AudioRecord ar;
	private Thread thread;

	public interface AudioListenerCallback {
		void onAudio(int level);
	}

	AudioListener(final AudioListenerCallback cb) {
		if( MyDebug.LOG )
			Log.d(TAG, "new AudioListener");
		final int sample_rate = 8000;
		int channel_config = AudioFormat.CHANNEL_IN_MONO;
		int audio_format = AudioFormat.ENCODING_PCM_16BIT;
		try {
			buffer_size = AudioRecord.getMinBufferSize(sample_rate, channel_config, audio_format);

			if( MyDebug.LOG )
				Log.d(TAG, "buffer_size: " + buffer_size);
			if( buffer_size <= 0 ) {
				if( MyDebug.LOG ) {
					if( buffer_size == AudioRecord.ERROR )
						Log.e(TAG, "getMinBufferSize returned ERROR");
					else if( buffer_size == AudioRecord.ERROR_BAD_VALUE )
						Log.e(TAG, "getMinBufferSize returned ERROR_BAD_VALUE");
				}
				return;
			}

			synchronized(AudioListener.this) {
				ar = new AudioRecord(MediaRecorder.AudioSource.MIC, sample_rate, channel_config, audio_format, buffer_size);
				AudioListener.this.notifyAll();
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			Log.e(TAG, "failed to create audiorecord");
			return;
		}


		synchronized(AudioListener.this) {
			if( ar.getState() == AudioRecord.STATE_INITIALIZED ) {
				if( MyDebug.LOG )
					Log.d(TAG, "audiorecord is initialised");
			}
			else {
				Log.e(TAG, "audiorecord failed to initialise");
				ar.release();
				ar = null;
				AudioListener.this.notifyAll();
				return;
			}
		}

		final short[] buffer = new short[buffer_size];
		ar.startRecording();

		this.thread = new Thread() {
			@Override
			public void run() {


				while( is_running ) {

					try {
					    int n_read = ar.read(buffer, 0, buffer_size);
					    if( n_read > 0 ) {
						    int average_noise = 0;
						    int max_noise = 0;
						    for(int i=0;i<n_read;i++){
						    	int value = Math.abs(buffer[i]);
						    	average_noise += value;
						    	max_noise = Math.max(max_noise, value);
						    }
						    average_noise /= n_read;

							cb.onAudio(average_noise);
					    }
					    else {
							if( MyDebug.LOG ) {
								Log.d(TAG, "n_read: " + n_read);
								if( n_read == AudioRecord.ERROR_INVALID_OPERATION )
									Log.e(TAG, "read returned ERROR_INVALID_OPERATION");
								else if( n_read == AudioRecord.ERROR_BAD_VALUE )
									Log.e(TAG, "read returned ERROR_BAD_VALUE");
							}
					    }
					}
					catch(Exception e) {
						e.printStackTrace();
						if( MyDebug.LOG )
							Log.e(TAG, "failed to read from audiorecord");
					}
				}
				if( MyDebug.LOG )
					Log.d(TAG, "stopped running");
				synchronized(AudioListener.this) {
					if( MyDebug.LOG )
						Log.d(TAG, "release ar");
					ar.release();
					ar = null;
					AudioListener.this.notifyAll();
				}
			}
		};
			}


	boolean status() {
		boolean ok;
		synchronized(AudioListener.this) {
			ok = ar != null;
		}
		return ok;
	}


	void start() {
		if( MyDebug.LOG )
			Log.d(TAG, "start");
		if( thread != null ) {
			thread.start();
		}
	}

	void release(boolean wait_until_done) {
		if( MyDebug.LOG ) {
			Log.d(TAG, "release");
			Log.d(TAG, "wait_until_done: " + wait_until_done);
		}
		is_running = false;
		thread = null;
		if( wait_until_done ) {
			if( MyDebug.LOG )
				Log.d(TAG, "wait until audio listener is freed");
			synchronized(AudioListener.this) {
				while( ar != null ) {
					if( MyDebug.LOG )
						Log.d(TAG, "ar still not freed, so wait");
					try {
						AudioListener.this.wait();
					}
					catch(InterruptedException e) {
						e.printStackTrace();
						if( MyDebug.LOG )
							Log.e(TAG, "interrupted while waiting for audio recorder to be freed");
					}
				}
			}
			if( MyDebug.LOG )
				Log.d(TAG, "audio listener is now freed");
		}
	}
}
