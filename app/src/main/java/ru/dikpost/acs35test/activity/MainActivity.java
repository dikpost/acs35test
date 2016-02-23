package ru.dikpost.acs35test.activity;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import ru.dikpost.acs35test.R;
import ru.dikpost.acs35test.reader.ReaderController;
import ru.dikpost.acs35test.reader.ReaderController.ReaderControllerListener;

public class MainActivity extends Activity implements OnClickListener,
		ReaderControllerListener, SharedPreferences.OnSharedPreferenceChangeListener {

	ReaderController controller;
	
	Button resetButton;
	TextView outputText;
	TextView atrText;
	SharedPreferences prefs;
	String authKey;
	String keyType;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		controller = new ReaderController( this );
	
		resetButton = (Button) findViewById(R.id.reset_button);
		resetButton.setOnClickListener( this );
		outputText = (TextView) findViewById(R.id.output_text);
		atrText = (TextView) findViewById(R.id.atr_text);
		
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
		registerReceiver(headsetPlugReceiver, filter);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		Log.d(ReaderController.TAG, "MainActivity.onCreate()");

	}

	@Override
	protected void onResume() {
		super.onResume();
		authKey = prefs.getString("auth_key", "FF FF FF FF FF FF");
		keyType = prefs.getString("key_type", "0x60");
		
		controller.getReader().start();
		outputText.setText("Status: READY");
	}
	
	@Override
	protected void onPause() {

		controller.getReader().stop();
		super.onPause();
	}

	@Override
    protected void onDestroy() {

        /* Unregister the headset plug receiver. */
        unregisterReceiver(headsetPlugReceiver);
        Log.d(ReaderController.TAG, "MainActivity.onDestroy()");

        super.onDestroy();
    }
    
    
    private final BroadcastReceiver headsetPlugReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {

                boolean plugged = (intent.getIntExtra("state", 0) == 1);
                Log.d(ReaderController.TAG, "headsetPlugReceiver.onReceive()");

                /* Mute the audio output if the reader is unplugged. */
                controller.getReader().setMute(!plugged);
            }
        }
    };

	@Override
	public void onClick(View v) {
		switch(v.getId()){

		case R.id.reset_button:
			outputText.setText("Status: READ CARD");
			controller.reset( authKey, keyType );
		break;
		}
	}

	@Override
	public void onOutputStrings(List<String> out) {

	//	handler.ob
		
		String s = new String();
		for( String str : out){
			s += str + "\n";
		}
		outputText.setText(s);

	}

	@Override
	public void onAtrString(String atr) {
		// TODO Auto-generated method stub
		atrText.setText(atr);
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
      // TODO Auto-generated method stub
      
      menu.add(1, 1, 100, "Settings");
      
      return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch( item.getItemId()){
    	case 1:
    		Intent i = new Intent(MainActivity.this, SettingsActivity.class);
    		startActivity(i);
    		break;
    	}
      return super.onOptionsItemSelected(item);
    }

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

		String val = sharedPreferences.getString(key,"");
		Log.d(ReaderController.TAG, "onSharedPrefrenceChanged() key = " + key + " value = " + val);

	}
}
