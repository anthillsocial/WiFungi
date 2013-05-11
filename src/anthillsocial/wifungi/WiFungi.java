package anthillsocial.wifungi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
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
import org.json.JSONException;
import org.json.JSONObject;

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
	* SETUP VARIABLES
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
	private TextView playback_txt_;
	private SeekBar seekBar_;
	private ToggleButton toggleButton_;
	
	// Audio vars
	private AudioPlayer lastRecorded;
	private AudioRecorder recordObj;
	
	// Http vars
	private String posturl = "http://resources.theanthillsocial.co.uk/wifungi/index.php";
	AsyncHttpTask postObj;
	
	/***************************************************** 
	* START THE: UI, AUDIO, HTTP
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
		playback_txt_ = (TextView) findViewById(R.id.PlaybackTxt);
		seekBar_ = (SeekBar) findViewById(R.id.SeekBar);
		toggleButton_ = (ToggleButton) findViewById(R.id.ToggleButton);
		enableUi(false);

		// Setup audio playback
		lastRecorded = new AudioPlayer(getApplicationContext());
		
		// Setup audio recorder
		recordObj = new AudioRecorder();
		
		// Start grabbing remote json		
		postObj = new AsyncHttpTask();
		postObj.execute(posturl, "sent this string");
		
		//Now manage: Playback, Recording, httpPOST and httpGET
		taskManager();
		
	}
	
	/***************************************************** 
	* TASK MANAGER: Manage: Audio, IOIO, HTTP
	******************************************************/
	private void taskManager(){
		int start_in = 0;     // Milliseconds until starting the timer
		int interval = 250;   // Millisecond interval between calling the method
		Timer t = new Timer();
		
		t.scheduleAtFixedRate(new TimerTask() {
			// Which file do we record the micInput to
			String recordFile = "WiFungi/MicInput.mp4";
			int recordlen = 0;
			int i;
			boolean recording = false;
			boolean playback = false;
			
		    @Override
		    public void run() {
		    	
		    	// Manage mic recording and playback
		    	if(recording==false && playback == false){
		    		recordObj.start(recordFile);
		    		recording = true;
		    	}
		    	recordlen = recordObj.recordinglen();
		    	if(recordlen>=10){
		    		recordObj.stop();
		    		recording = false;
		    	}	
		    	if(recording==false && playback==false){
		    		playback = true;
		    		lastRecorded.play(recordFile, false);
		    	}
		    	if(lastRecorded.status()=="Ready" && playback==true){
		    		playback = false;
				}
		    	
		    	// Set UI output
				setText("recording_label", recordObj.status()+" ("+String.valueOf(recordlen)+" secs)");
				setText("transreciever_label", ioiostatus_);
				setText("network_label", poststatus_);
				setText("playback_label", lastRecorded.status()+" | "+i);
				i++;
		    }         
		},start_in, interval);
	}
	
	/***************************************************** 
	* MANAGE MULTI-TRACK AUDIO
	******************************************************/	
	
	
	/***************************************************** 
	* MANAGE ASYNC HTTP CONNECTIONS
	* From: http://mobiledevtuts.com/android/android-http-with-asynctask-example
	* Upload with progress bar: 
	* http://toolongdidntread.com/android/android-multipart-post-with-progress-bar/
	******************************************************/	
	/* POST DATA */
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
	
	/* GRAB JSON FROM URL */
	public class JsonReader {
		private String readAll(Reader rd) throws IOException {
		    StringBuilder sb = new StringBuilder();
		    int cp;
		    while ((cp = rd.read()) != -1) {
		      sb.append((char) cp);
		    }
		    return sb.toString();
		}

		public JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		    InputStream is = new URL(url).openStream();
		    try {
		      BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
		      String jsonText = readAll(rd);
		      JSONObject json = new JSONObject(jsonText);
		      return json;
		    } finally {
		      is.close();
		    }
		}

		public void main(String[] args) throws IOException, JSONException {
		    JSONObject json = readJsonFromUrl("https://graph.facebook.com/19292868552");
		    System.out.println(json.toString());
		    System.out.println(json.get("id"));
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
	* UI COMMUNICATIONS
	******************************************************/
	// Make connection to UI elements
	private void enableUi(final boolean enable) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				seekBar_.setEnabled(enable);
				toggleButton_.setEnabled(enable);
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
				if(which_txt=="playback_label") playback_txt_.setText(str);
			}
		});
	}
	
	

}