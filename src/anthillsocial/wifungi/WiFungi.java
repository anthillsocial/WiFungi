package anthillsocial.wifungi;

import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
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
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.loopj.android.http.*;

public class WiFungi extends IOIOActivity {
	/***************************************************** 
	* SETUP VARIABLES
	*****************************************************/
	// Data
	private String datadir = "WiFungi";
	
	// Manage all the tasks
	private String ioiostatus_ = "RECIEVER";
	private String poststatus_ = "Paused";
	private String getstatus_ = "Paused";
	private String audiostatus_ = "Paused";
	
	// UI vars
	private TextView textView_;
	private TextView calc_;
	private TextView networkget_;
	private TextView networkpost_;
	private TextView networkaudio_; 
	private TextView recording_txt_;
	private TextView transreciever_txt_;
	private TextView playback_txt_;
	private SeekBar seekBar_;
	private ToggleButton toggleButton_;
	
	// Audio vars
	private AudioPlayer lastRecorded;
	private AudioRecorder recordObj;
	
	// Http vars
	private String jsonurl;
	private String posturl;
	private HttpPostGet getobj;
	private HttpPostGet postobj;
	private HttpPostGet audioobj;
	
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
		networkget_ = (TextView) findViewById(R.id.NetworkGet);
		networkpost_ = (TextView) findViewById(R.id.NetworkPost);
		networkaudio_ = (TextView) findViewById(R.id.NetworkAudio);
		
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
		int currentime = (int) (System.currentTimeMillis());
		
		// Object used to grab JSON data from the server
		jsonurl = "http://resources.theanthillsocial.co.uk/wifungi/index.php?json";
		getobj = new HttpPostGet();
		
		// Object used to POST data to the server
		posturl = "http://resources.theanthillsocial.co.uk/wifungi/index.php?post";
		postobj  = new HttpPostGet(); 
		
		// Object used to download audio files
		audioobj  = new HttpPostGet(); 
		
		//Now manage: Playback, Recording, httpPOST and httpGET
		taskManager();
		
	}
	
	/***************************************************** 
	* TASK MANAGER: ANSYNC Manage: Audio, IOIO, HTTP
	******************************************************/
	private void taskManager(){
		int start_in = 0;     		// Milliseconds until starting the timer
		final int interval = 250;   // Millisecond interval between calling the method
		Timer t = new Timer();
		
		t.scheduleAtFixedRate(new TimerTask() {
			// Which file do we record the micInput to
			String recordFile = "mic.mp4";
			int recordlen = 0;
			int i;
			double showpost = 0;
			double getjsonInterval = 10.0; // Every # seconds grab a new json data
			double getJsonCounter = 0.0;
			double getAudioCounter = 0.0;
			boolean recording = false;
			boolean playback = false;
			boolean getdata = true;
			String newaudiofile;
			String newaudiourl;
			
		    @Override
		    public void run() {
		    	
				// TRANSMITTER: POST DATA TO THE SERVER
		    	if(ioiostatus_=="TRANSMITTER"){
			    	// Manage mic recording and playback
			    	if(recording==false && playback == false){
			    		recordObj.start(datadir+"/"+recordFile);
			    		recording = true;
			    	}
		    	}
		    	recordlen = recordObj.recordinglen();
		    	if(recordlen>=11){
		    		recordObj.stop();
		    		recording = false;
		    		postobj.post(posturl, recordFile);
		    	}	
		    	if(recording==false && playback==false){
		    		playback = true;
		    		//lastRecorded.play(recordFile, false);
		    	}
		    	if(lastRecorded.status()=="Ready" && playback==true){
		    		playback = false;
				}
		    	if(postobj.status=="Success" || postobj.status=="Failure"){
		    		poststatus_ = postobj.status+": "+postobj.getData();
		    		showpost = 0;
		    	}
		    	if(showpost>10){
		    		poststatus_ = postobj.status;
		    	}
		    	showpost = showpost+((double)interval/1000);
		    	
		    	// RECIEVER: GET DATA FROM THE SERVER
		    	if(ioiostatus_=="RECIEVER" && getJsonCounter>=getjsonInterval){
		    		getobj.getjson(jsonurl);
		    	}
		    	if(getobj.status=="Success" || getobj.status=="Failure"){
		    		getstatus_ = getobj.status+": "+getobj.getData();
		    		getJsonCounter = 0.0;
		    	}
		    	if(getJsonCounter>2) getstatus_ = getobj.status+" | "+(int)getJsonCounter;
				getJsonCounter = getJsonCounter+((double)interval/1000);

				// DOWNLOAD AUDIO IF ITS AVAILABLE
				if (getobj.JsonResponse != null && audioobj.status!="Fetching"){
					try {newaudiofile = getobj.JsonResponse.getString("audiofile"); }catch (JSONException e) {newaudiofile="";}
					try {newaudiourl = getobj.JsonResponse.getString("audiourl"); }catch (JSONException e) {newaudiourl="";}
					if(newaudiofile!="" && newaudiourl!=""){
						audiostatus_ = "Downloading: "+newaudiofile+"|"+newaudiourl;
						String newFilePath = datadir+"/tmp/"+newaudiofile;
						audioobj.getfile(newaudiourl, newFilePath);
						//audioobj.get(newaudiourl);
						//ExampleUsage example = new ExampleUsage();
						//example.makeRequest();
						getAudioCounter = 0.0;
					}
					getobj.JsonResponse = null;
				}
		    	if(audioobj.status=="Success" || audioobj.status=="Failure"){
		    		audiostatus_ = audioobj.status+": "+audioobj.getData();
		    		getAudioCounter = 0.0;
		    	}
				if(getAudioCounter>2) audiostatus_ = audioobj.status+" | "+(int)getAudioCounter;
				getAudioCounter = getAudioCounter+((double)interval/1000);
				
		    	// SET UI OUTPUT
				setText("recording_label", recordObj.status()+" ("+String.valueOf(recordlen)+" secs)");
				setText("transreciever_label", ioiostatus_);
				setText("networkget_label", getstatus_);
				setText("networkpost_label", poststatus_);
				setText("networkaudio_label", audiostatus_);
				setText("playback_label", lastRecorded.status()+" | "+i);

				i++;
		    }         
		},start_in, interval);
	}
	
	public class ExampleUsage {
	    public void makeRequest() {
			String[] allowedContentTypes = new String[] { 
					"image/png", "image/jpeg", 
					"audio/mp3" , "video/mp3" ,
					"audio/mp4",  "video/mp4", 
					"audio/3gp4", "video/3gp4", 
					"audio/3gp4", "video/3gp4", 
					"audio/3gpp", "video/3gpp" ,
					"audio/3gp",  "video/3gp",
					"audio/3gpp2", "video/3gpp2",
					"audio/isom3gp4", "video/isom3gp4",
					"ftypisom/isom3gp4"
					
			};
	    	String url = "http://resources.theanthillsocial.co.uk/wifungi/data/WiFungi_mic.mp4";
	        AsyncHttpClient client = new AsyncHttpClient();
	        /*client.get(url, null, new BinaryHttpResponseHandler(allowedContentTypes) {
			    @Override
			    public void onSuccess(byte[] fileData) {
			    	
			    }
			 */
	        client.get(url, null, new BinaryHttpResponseHandler(allowedContentTypes)  {
	       // client.get(, new AsyncHttpResponseHandler() {
	            @Override
	            public void onSuccess(byte[] fileData) {
	            	 Log.d("WiFungi HTTP", "Http sucess google: "); 
	            }
	            @Override
	            public void onFailure(Throwable error, byte[] fileData) {
	            	//String error = error.getMessage();
	            	// error =  error.getMessage().equals("Error");
	            	error.getStackTrace ();
	            	Log.e("WiFungi HTTP", "Http failure google: "+error); 
	            }
	        });
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
				if(which_txt=="networkget_label") networkget_.setText(str);
				if(which_txt=="networkpost_label") networkpost_.setText(str);
				if(which_txt=="networkaudio_label") networkaudio_.setText(str);
				if(which_txt=="recording_label") recording_txt_.setText(str);
				if(which_txt=="transreciever_label") transreciever_txt_.setText(str);
				if(which_txt=="playback_label") playback_txt_.setText(str);
			}
		});
	}
	
	

}