package anthillsocial.wifungi;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Environment;
import android.util.Log;
import com.loopj.android.http.*;

/* POST DATA 
 * This class uses the fantastic Android Asynchronous Http Client:
 * http://loopj.com/android-async-http/ 
 * */
class HttpPostGet{
	public String status = "Paused";
	private String responseStr;
	public JSONObject JsonResponse;
	private String newFilePath;
	
	AsyncHttpClient client = new AsyncHttpClient();
	
	// Get some data from the server and return a string
	public void get(String url){
		if(status!="Paused") return;
		status = "Fetching";
		responseStr = "";
		JsonResponse = null;
		client.get(url, new AsyncHttpResponseHandler() {
		    @Override
		    public void onSuccess(String response) {
		    	success(response);
		        Log.d("WiFungi HTTP", "Http Success: "+response); 
		    }
		    @Override
		    public void onFailure(Throwable error, String response) {
		    	failure(response);
		        Log.d("WiFungi HTTP", "Http error: "+response); 
		    }
		    @Override
		    public void onFinish() {
		    	finish();
		        Log.d("WiFungi HTTP", "Http finished "); 
		    }
		});
	}
	
	// Get some data from the server and return a json object
	public void getjson(String url){
		if(status!="Paused") return;
		status = "Fetching";
		responseStr = "";
		client.get(url, null, new JsonHttpResponseHandler() {
		    @Override
		    public void onSuccess(JSONObject response) {
		    	JsonResponse = response;
				String responseStr = "No response";
				String remoteStatus = "Failure";
				try { 
					remoteStatus = response.getString("status"); 
				}catch (JSONException e) { 
					remoteStatus = "Failure";
					responseStr = "No status";
				}
				try { 
					responseStr = response.getString("response"); 
				}catch (JSONException e) { 
					remoteStatus = "Failure";
					responseStr = "No response string";
				}
				if(remoteStatus=="Failure"){
					failure(responseStr);
				}else{
					success(responseStr);
				}
		        Log.d("WiFungi HTTP", "Http Success: "+response); 
		    }
		    @Override
		    public void onFailure(Throwable error, JSONObject response) {
		    	String thisresponse = "Lets fail"+error.getStackTrace();
		    	thisresponse += error.toString();
		    	failure(thisresponse);
		        Log.d("WiFungi HTTP", "Http error: "+response); 
		    }	
		    @Override
		    public void onFinish() {
		    	finish();
		        Log.d("WiFungi HTTP", "Http finished "); 
		    }
		});
	}
	
	// Download a file from the server
	public void getfile(String url, String filePath){
		newFilePath =  getPath(filePath);
		final String fetchUrl = url;
		String[] allowedContentTypes = new String[] { 
				"image/png", "image/jpeg", "text/plain","text/plain; charset=UTF-8","audio/mpeg"
		};
		if(status!="Paused") return;
		status = "Fetching";
		responseStr = "";
		client.get(url, null, new BinaryHttpResponseHandler(allowedContentTypes) {
		    @Override
		    public void onSuccess(byte[] fileData) {
		        // make sure the directory we plan to store the recording in exists
		        File directory = new File(newFilePath).getParentFile();
		        if (!directory.exists() && !directory.mkdirs()) {
		        	failure("Couldn't create directory: "+newFilePath);
		        	Log.e("WiFungi HTTP", "Http SaveFail: Couldn't create directory: "+newFilePath); 
		        	return;
		        }
		    	// Now lets save the bytes to a new file
		    	try {
					writeFile(fileData, newFilePath);
					success("Saved file");
					Log.d("WiFungi HTTP", "Http Getfile Success: "+responseStr);
				} catch (IOException e) {
					failure("Couldn't save file: "+e);
					// TODO Auto-generated catch block
					Log.e("WiFungi HTTP", "Http SaveFail: Could not write to file:"+e); 
				}		    	
		    }
		    @Override
		    public void onFailure(Throwable error, byte[] fileData) {
		    	String response = error.toString();
		    	failure(response);
		        Log.e("WiFungi HTTP", "Http Getfile error: "+error); 
		    }	
		    @Override
		    public void onFinish() {
		    	finish();
		        //Log.d("WiFungi HTTP", "Http Getfile finished "+fetchUrl); 
		    }
		});
	}

	
	// POST data to the server
	public void post(String url, String path){
		if(status!="Paused") return;
		status = "Fetching";
		String filename = path.replace('/', '_');
		String filepath = getPath(path);
	    File myFile = new File(filepath);
		//byte[] myByteArray = new byte[(int) myFile.length()]; 
		RequestParams params = new RequestParams();
		try {
			params.put("audiofile", myFile);
		} catch(FileNotFoundException e) {}
		
		//params.put("audiofile", new ByteArrayInputStream(myByteArray), path);
		params.put("filename", filename);
		Log.d("WiFungi HTTP", "Http SIZE: "+myFile.length()+" PATH:"+filepath); 
		params.put("json", "value");
		client.post(url, params, new AsyncHttpResponseHandler() {
		    @Override
		    public void onSuccess(String response) {
		    	status = "Success";
		    	responseStr = response;
		        Log.d("WiFungi HTTP", "Http Success: "+response); 
		    }
		    @Override
		    public void onFailure(Throwable error, String content) {
		    	status = "Failure";
		    	responseStr = content;
		        Log.d("WiFungi HTTP", "Http error: "+content); 
		        error.printStackTrace();
		    }	
		    @Override
		    public void onFinish() {
		    	finish();
		        Log.d("WiFungi HTTP", "Http finished "); 
		    }
		});
	}
	
	// Get the data out
	public String getData(){
		status = "Paused";
		return responseStr;
	}
	
	// Success
	private void success(String response){
    	status = "Success";
    	responseStr = response;
	}
	
	// Failure
	private void failure(String response){
    	status = "Failure";
    	responseStr = response;
	}
	
	// Success
	private void successjson(String response){
    	status = "Success";
    	responseStr = response;
	}
	
	// Failure
	private void failurejson(String response){
    	status = "Failure";
    	responseStr = response;
	}
	
	// Finished
	private void finish(){
		if(status!="Failure" && status != "Success"){
			status="Failure";
			responseStr = "Possible server error";
		}
	}
	
	// Get the full path 
	private String getPath(String path) {
		return Environment.getExternalStorageDirectory().getAbsolutePath() +"/"+path;
	}
	
	// Write to a file
	public void writeFile(byte[] data, String fileName) throws IOException{
		  FileOutputStream out = new FileOutputStream(fileName);
		  out.write(data);
		  out.close();
		}
}
