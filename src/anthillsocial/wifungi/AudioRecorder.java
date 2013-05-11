package anthillsocial.wifungi;

import java.io.File;
import java.io.IOException;

import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

/**
 * Class @author tomkeene <a href="http://www.theanthillsocial.co.uk">Tom Keene</a>
 */
public class AudioRecorder {
  private String TAG = "AudioRecorder";
  private MediaRecorder recorder = null;
  private String path;
  private String status = "Ready";
  private int startedrecording = 0;
  private String audioformat;

  /**
   * Creates a new audio recording at the given path (relative to root of SD card).
   */
  //public AudioRecorder(String path) {
    //this.path = sanitizePath(path);
  //}

  private int currenttime(){
	  return  (int) (System.currentTimeMillis() / 1000L);
  }
  
  private String sanitizePath(String path) {
    if (!path.startsWith("/")) path = "/" + path;
    if (!path.contains(".")) path += ".3gp";
    if (path.contains(".3gp")) audioformat = ".3gp";
    if (path.contains(".mp4")) audioformat = ".mp4";
    return Environment.getExternalStorageDirectory().getAbsolutePath() + path;
  }
  
  public void start(String newpath){
	  if(recorder!=null) return;
	  recorder = new MediaRecorder();
	  path = sanitizePath(newpath);
	  try {
		startrecording();
		status = "Recording";
		Log.d(TAG, "start() recording");
	  } catch (IOException e) {
		// TODO Auto-generated catch block
		//e.printStackTrace();
		status = "Failed";
		Log.e(TAG, "start() failed");
	 }
  }
  
  /**
   * Starts a new recording.
   */
  private void startrecording() throws IOException {
	String msg;
    String state = android.os.Environment.getExternalStorageState();
    if(!state.equals(android.os.Environment.MEDIA_MOUNTED))  {
    	msg = "SD Card is not mounted.  It is " + state + ".";
    	status = "Failed";
        throw new IOException(msg);
    }

    // make sure the directory we plan to store the recording in exists
    File directory = new File(path).getParentFile();
    if (!directory.exists() && !directory.mkdirs()) {
      msg = "Path to file could not be created.";
      status = "Failed";
      throw new IOException(msg);
    }
    startedrecording = currenttime();
    recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
    if(audioformat==".3gp") recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
    if(audioformat==".mp4") recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
    recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
    recorder.setOutputFile(path);
    
    try {
    	recorder.prepare();
    	Log.d(TAG, "prepare() recording");
    } catch (IOException e) {
        Log.e(TAG, "prepare() failed");
        status = "Failed";
    }
    recorder.start();
  }
  
  /**
   * Stops a recording that has been previously started.
   */
  public void stop(){
	  try {
		stoprecording();
		Log.d("audiorecorder", "stop() recording");
	  } catch (IOException e) {
		// TODO Auto-generated catch block
		//e.printStackTrace();
		Log.e("audiorecorder", "stop() failed");
	  }
  }
  
  public void stoprecording() throws IOException {
    recorder.stop();
    recorder.release();
    recorder = null;
    status = "Ready";
    startedrecording = 0;
  }
  
  /**
   * Returns the current recording status.
   */
  public String status() {
    return status;
  }
  
  public int recordinglen(){
	  if(status == "Recording"){
		  return currenttime()-startedrecording;
	  }else{
		  return 0;
	  }
  }
}

