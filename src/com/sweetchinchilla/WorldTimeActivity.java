package com.sweetchinchilla;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import com.sweetchinchilla.R.string;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class WorldTimeActivity extends Activity {

	final String api = "http://www.worldweatheronline.com/feed/tz.ashx?key=a0f50938ea211241120602&q=%s&format=json";

	Button getTime;
	EditText locationInput;
	TextView locationResult;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		getTime = (Button)findViewById(R.id.button_getTime);
		locationInput = (EditText)findViewById(R.id.editText_location);
		locationResult = (TextView)findViewById(R.id.textView_timeResponse);



		getTime.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {

				// check if any input
				String location = locationInput.getText().toString();
				if(location.length() == 0) {
					Toast.makeText(getApplicationContext(), "Please enter a location.", Toast.LENGTH_SHORT).show();
					return;
				}

				InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(locationInput.getWindowToken(), 0);
				
				getTime.setText(R.string.button_getting_time);
				getTime.setEnabled(false);

				locationResult.setText("");
				
				new GetTimeTask().execute(location);
			}
		});
	}

	private class GetTimeTask extends AsyncTask<String,Integer,String> {

		protected void onPostExecute(String result) {
			locationResult.setText(result);

			getTime.setText(R.string.button_get_time);
			getTime.setEnabled(true);
		}

		@Override
		protected String doInBackground(String... params) {
			String location = params[0];
			String req_url = String.format(api, URLEncoder.encode(location));
			StringBuilder builder = new StringBuilder();
			HttpClient client = new DefaultHttpClient();
			HttpGet httpGet = new HttpGet(req_url);
			try {
				Log.d(WorldTimeActivity.class.toString(), "Hitting API: " + req_url);
				HttpResponse response = client.execute(httpGet);
				StatusLine statusLine = response.getStatusLine();
				int statusCode = statusLine.getStatusCode();
				if (statusCode == 200) {
					HttpEntity entity = response.getEntity();
					InputStream content = entity.getContent();
					BufferedReader reader = new BufferedReader(
							new InputStreamReader(content));
					String line;
					while ((line = reader.readLine()) != null) {
						builder.append(line);
					}

				} else {
					Log.e(WorldTimeActivity.class.toString(), "Failed to download file");
				}

				try {
					JSONObject res = new JSONObject(builder.toString());
					JSONObject data = res.getJSONObject("data");
					
					if(data.has("error")) {
						return data.getJSONArray("error").getJSONObject(0).getString("msg");
					}
					
					JSONObject time_zone = data.getJSONArray("time_zone").getJSONObject(0);
					JSONObject request = data.getJSONArray("request").getJSONObject(0);

					DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm"); 
					Date date;
					try {
						date = formatter.parse(time_zone.getString("localtime"));
					} catch (ParseException e) {
						e.printStackTrace();
						return "Sorry, there was a problem.";
					} 

					DateFormat df = DateFormat.getDateInstance(DateFormat.FULL);
					DateFormat tf = DateFormat.getTimeInstance(DateFormat.DEFAULT);

					// format response
					return String.format("It is currently %s on %s in the %s of %s.",
							tf.format(date),
							df.format(date),
							request.getString("type"),
							request.getString("query"));


				} catch (JSONException e) {
					e.printStackTrace();
					return "Sorry, there was a problem.";
				}

			} catch (ClientProtocolException e) {
				e.printStackTrace();
				return "Sorry, there was a problem.";
			} catch (IOException e) {
				e.printStackTrace();
				return "Sorry, there was a problem.";
			} 
		}


	}

}