package anthillsocial.wifungi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import anthillsocial.wifungi.R;
import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;

public class WiFungi extends IOIOActivity {
	/***************************************************** 
	* SET VARIABLES
	*****************************************************/
	// Manage all the tasks
	private Handler taskHandler;
	private String ioiostatus_ = "RECIEVER";
	private String poststatus_ = "Nothing sent";
	
	// UI vars
	private TextView textView_;
	private TextView calc_;
	private TextView network_;
	private TextView recording_txt_;
	private TextView transreciever_txt_;
	private SeekBar seekBar_;
	private ToggleButton toggleButton_;
	
	// Audio vars
	private MediaPlayer track1_;
	private AudioRecorder recordObj;
	
	// Http vars
	private String posturl = "http://resources.theanthillsocial.co.uk/wifungi/index.php";
	AsyncHttpTask postObj;
	
	/***************************************************** 
	* WHEN THE ACTIVITY HAS STARTED
	*****************************************************/
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Make connection to UI elements
		setContentView(R.layout.main);
		textView_ = (TextView) findViewById(R.id.TextView);
		calc_ = (TextView) findViewById(R.id.Calc);
		network_ = (TextView) findViewById(R.id.Network);
		recording_txt_ = (TextView) findViewById(R.id.RecordingTxt);
		transreciever_txt_ = (TextView) findViewById(R.id.TransRecieverTxt);
		seekBar_ = (SeekBar) findViewById(R.id.SeekBar);
		toggleButton_ = (ToggleButton) findViewById(R.id.ToggleButton);
		enableUi(false);
		
		// Setup audio playback
		//track1_ = MediaPlayer.create(getApplicationContext(), R.raw.track1);
		//track1_.start();
		
		// Setup audio recorder, files
		String dir = "WiFungi";
		recordObj = new AudioRecorder(dir);
		//recordObj.start();
		
		// Start grabbing remote json		
		postObj = new AsyncHttpTask();
		postObj.execute(posturl, "sent this string");
		
		//Now manage: Playback, Recording, httpPOST and httpGET
		taskManager();
	}
	
	/***************************************************** 
	* TASK MANAGER: Check status of: Audio, IOIO, HTTP
	******************************************************/
	private void taskManager(){
		int start_in = 0;     // milliseconds until starting the timer
		int interval = 250;  // millisecond interval between calling the method
		Timer t = new Timer();
		t.scheduleAtFixedRate(new TimerTask() {
			private int i;
		    @Override
		    public void run() {
		        //Called each time when 1000 milliseconds (1 second) (the period parameter)
		    	String msg = "On UI";
				setText("recording_label", msg+String.valueOf(i));
				setText("transreciever_label", ioiostatus_);
				setText("network_label", poststatus_);
				i++;
		    }         
		},start_in, interval);
	}
	
	/***************************************************** 
	* ASYNC SOUND RECORDING
	******************************************************/
	
	/***************************************************** 
	* MANAGE ASYNC HTTP CONNECTIONS
	* From: http://mobiledevtuts.com/android/android-http-with-asynctask-example
	* Upload with progress bar: 
	* http://toolongdidntread.com/android/android-multipart-post-with-progress-bar/
	******************************************************/	
	private class AsyncHttpTask extends AsyncTask<String, Integer, Double>{
		 
		@Override
		protected Double doInBackground(String... params) {
			// TODO Auto-generated method stub
			postData(params[0], params[1]);
			return null;
		}
		
		// The post has been made
		protected void onPostExecute(Double result){
			Log.v("WiFungi HTTP", "executed http post");
			poststatus_ = "Trying to post data";
		}
		
		protected void onProgressUpdate(Integer... progress){
			String msg = String.valueOf(progress[0]);
			Log.v("WiFungi HTTP", "http progress: " + msg);
			setText("network_label", msg);
		}
 
		public void postData(String myUrl, String valueIWantToSend) {
			// Create a new HttpClient and Post Header
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(myUrl);
			// Now try and send the data
			try {
				// Add the data
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
				nameValuePairs.add(new BasicNameValuePair("myHttpData", valueIWantToSend));
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs)); 
				// Execute HTTP Post Request
				HttpResponse response = httpclient.execute(httppost);
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
			} catch (IOException e) {
				// TODO Auto-generated catch block
			}
		}
	}
	
	/***************************************************** 
	* IOIO Thread (Based on IOIO example)
	******************************************************/
	class Looper extends BaseIOIOLooper {
		private AnalogInput input_;
		private PwmOutput pwmOutput_;
		private DigitalOutput led_;

		@Override
		public void setup() throws ConnectionLostException {
			led_ = ioio_.openDigitalOutput(IOIO.LED_PIN, true);
			input_ = ioio_.openAnalogInput(44);
			pwmOutput_ = ioio_.openPwmOutput(38, 100);
			enableUi(true);
		}

		@Override
		public void loop() throws ConnectionLostException, InterruptedException {
			// Let the task manager know we are connected to an IOIO device 			
			ioiostatus_ = "TRANSMITTER";
			
			// Read IOIO varibales
			final float reading = input_.read();
			float calc = (reading*1000)*2;
			//int calc = 500 + seekBar_.getProgress() * 2;
			
			// Set IOIO outputs
			pwmOutput_.setPulseWidth( (int) calc);
			led_.write(!toggleButton_.isChecked());
			
			// Set UI outputs
			setText("light_label", Float.toString(reading) );
			setText("calc_label", Float.toString(calc) );
			
			// Sleep the thread
			Thread.sleep(10);
		}

		@Override
		public void disconnected() {
			ioiostatus_ = "RECIEVER";
			enableUi(false);
		}
	}
	
	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}
	
	/***************************************************** 
	* MANAGE AUDIO PLAYBACK
	******************************************************/
	public void onPrepared(MediaPlayer player) {
		track1_.start();
	}
	   
	public void stop(){
	    if(track1_.isPlaying()) track1_.stop();
	}
	
	public void pause(){
	    if(track1_.isPlaying()) track1_.pause();
	}

	/***************************************************** 
	* UI COMMUNICATIONS
	******************************************************/
	// Make connection to UI elements
	private void enableUi(final boolean enable) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				seekBar_.setEnabled(enable);
				toggleButton_.setEnabled(enable);
				//String msg = recordObj.status();
				//
			}
		});
	}
	
	// Set the text output on the UI
	private void setText(final String which_txt, final String str) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if(which_txt=="light_label") textView_.setText(str);
				if(which_txt=="calc_label") calc_.setText(str);
				if(which_txt=="network_label") network_.setText(str);
				if(which_txt=="recording_label") recording_txt_.setText(str);
				if(which_txt=="transreciever_label") transreciever_txt_.setText(str);
			}
		});
	}
	
	

}