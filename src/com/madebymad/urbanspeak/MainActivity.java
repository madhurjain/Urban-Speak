package com.madebymad.urbanspeak;

import java.util.ArrayList;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import android.os.Bundle;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {
	
	private final static String TAG = MainActivity.class.getSimpleName();
	
	protected static final int RESULT_SPEECH = 101;
	protected static final int RESULT_TTS_CHECK = 102;
	 
    private ImageButton btnStart;
    private TextView txtText;
    private TextToSpeech textToSpeech;
    
    private AsyncHttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        textToSpeech = new TextToSpeech(this, this);
        btnStart = (ImageButton) findViewById(R.id.button_start);
        txtText = (TextView) findViewById(R.id.textView_spoke);
        httpClient = new AsyncHttpClient();
        
        btnStart.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View view) {
				
				if(textToSpeech != null && textToSpeech.isSpeaking())
				{
					textToSpeech.stop();
				}
				
				Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
				intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
				
				try
				{
					startActivityForResult(intent, RESULT_SPEECH);
					txtText.setText("");
				}
				catch (ActivityNotFoundException aex)
				{
					Toast toast = Toast.makeText(getApplicationContext(), 
							"Your device doesn't support Speech to Text", Toast.LENGTH_SHORT);
					toast.show();
				}
			}
		});
        
        // Text To Speech Engine Check. Buggy on ICS and Above.
        /*
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, RESULT_TTS_CHECK);
        */
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	switch(requestCode)
    	{
    		case RESULT_TTS_CHECK:
    			if(resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS)
    			{
    				// Success. Create TTS Instance
    				textToSpeech = new TextToSpeech(this, this);
    			}
    			else
    			{       				
					// Missing Data. Install Text To Speech Engine
					Log.v(TAG, "Missing Data. Re-Directing to Install Page.");
					Intent installIntent = new Intent();
					installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
					startActivity(installIntent);    				
    			}
    			break;
    		case RESULT_SPEECH:
    			if(resultCode == RESULT_OK && null != data)
    			{
    				ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
    				txtText.setText(text.get(0));
    				Log.v(TAG, "Speech Recognized: " + text.get(0));
    				getMeaningOnline(text.get(0));
    				    				
    			}
    			break;
    	}
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			
			int result = textToSpeech.setLanguage(Locale.US);
			
			if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {				
				Log.e(TAG, "Language is not supported");								
			} else {				
				speakOut("Welcome to Urban Speak!");
			}
	 
		} else {
			Log.e(TAG, "Initilization Failed!");
		}
	}
	
	private void speakOut(String text) {
		Log.v(TAG, "Speaking: " + text);
		textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }
	
	private void getMeaningOnline(String word)
	{
		if(word.length() < 2)
		{
			return;
		}
		RequestParams params = new RequestParams("term", word);
		httpClient.get("http://api.urbandictionary.com/v0/define", params, new JsonHttpResponseHandler() {
			@Override
			public void onSuccess(JSONObject jsonObj) {
				super.onSuccess(jsonObj);
				try {
					if(!jsonObj.getString("result_type").equals("no_results"))
					{
						JSONArray listArr = jsonObj.getJSONArray("list");
						JSONObject listObj = listArr.getJSONObject(0);
						String definition = listObj.getString("definition");
						speakOut(definition);
					}
					else
					{
						Log.v(TAG, "Word not found online");
					}
				} catch (JSONException jexp) {
					Log.e(TAG, jexp.getMessage());
				}				
			}
						
		});
	}
	
	@Override
	protected void onDestroy() {
		if (textToSpeech != null) {
			textToSpeech.stop();
			textToSpeech.shutdown();
		}
		super.onDestroy();		
	}
    
}
