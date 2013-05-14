package anthillsocial.wifungi;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.View;

/***************************************************** 
* MANAGE AUDIO PLAYBACK
******************************************************/
public class AudioPlayer {
	// Setup vars
	private String TAG = "AudioPlayer";
	private MediaPlayer audiofile = null;
	private Context context;
	private String status = "Ready";
	private String currentpath;
	private boolean islooped;
	private int started = 0;
	
	public AudioPlayer(Context applicationContext) {
		context = applicationContext;
	}

	// Attempt to start playing
	public void play(String newPath, boolean loopme){
		if(audiofile!=null) return;
		audiofile = new MediaPlayer();
		islooped = loopme;
		try {
			currentpath = newPath;
			start();
			Log.d(TAG, "start()");
			status = "Playing";
		}  catch (IOException e) {
			// TODO Auto-generated catch block
			Log.e(TAG, "start() Failed");
			status = "Failed to play "+currentpath;
			e.printStackTrace();
		}	
	}
	
	// Create a Uri from a path
	private Uri createUri(String path){
		String fullpathstr = Environment.getExternalStorageDirectory().getAbsolutePath()+'/'+path;
		File fullPath = new File(fullpathstr);
		return Uri.fromFile(fullPath);
	}
	
	// Start playing a local file
	public void start() throws IOException {
		Uri fullUri = createUri(currentpath);
		Log.d(TAG, "start() playpack Uri: "+fullUri);
		audiofile.setAudioStreamType(AudioManager.STREAM_MUSIC);
		audiofile.setDataSource(context, fullUri);
		if(islooped){
			audiofile.setLooping(true);
		}else{
			audiofile.setOnCompletionListener(new OnCompletionListener() {
				public void onCompletion(MediaPlayer audiofile) {
					stop();
					Log.d(TAG, "Audo setOnCompletionListener() stop");
				}
			});	
		}
		audiofile.setOnPreparedListener(new OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer arg0) {
				started = currenttime();
				audiofile.start();
				Log.d(TAG, "Audo setOnCompletionListener() stop");
			}
		});	
		audiofile.prepareAsync();
	}

	public String status() {
		return status;
	}
	
	// Stop playing
	public void stop(){
		Log.d(TAG, "Audo stop() stopped playback");
		status = "Ready";
	    audiofile.stop();
	    audiofile.release();
	    audiofile=null;
	}
	
	// Pause playing
	public void pause(){
		status = "Paused";
	    if(audiofile.isPlaying()) audiofile.pause();
	}
	
	public int elapsed(){
		  if(status == "Playing"){
			  return currenttime()-started;
		  }else{
			  return 0;
		  }
	}
	
	private int currenttime(){
		 return  (int) (System.currentTimeMillis() / 1000L);
	}
}
