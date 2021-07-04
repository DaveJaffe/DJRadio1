// DJ Radio 1
// Communicates with Raspberry Pi - based dual radio to display RDS info
// Displays 5 most recent stations showing RDS info as well as 4 presets
// Click on any button to play station

package com.example.djradio1;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.AsyncTask;
import android.os.Build;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import android.util.Log;
import android.util.TypedValue;

import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {

  static int n_rds_buttons = 5;
  static int n_preset_buttons = 4;
  private static final String TAG = MainActivity.class.getName();
  private RequestQueue queue;
  private StringRequest stringRequest;
  private String last_rds_info="", previous_rds_info="";
  private String[] rds_info = new String[n_rds_buttons];
  private String ip_address, url;
  private static final int[] rds_button_ids = {R.id.rds1Button, R.id.rds2Button,
      R.id.rds3Button, R.id.rds4Button, R.id.rds5Button};
  private static final int[] preset_button_ids = {R.id.preset1Button, R.id.preset2Button,
      R.id.preset3Button, R.id.preset4Button};
  private static final int[] preset_text_ids = {R.string.preset1Text, R.string.preset2Text,
      R.string.preset3Text, R.string.preset4Text};
  static String[] preset_freq = new String[n_preset_buttons];
  private Button rds_button, rds_button_current=null, preset_button, preset_button_current=null;
  private int index = -1;
  private Integer currentStation = null;
  private int primaryColor, primaryColorVariant;

  String ip_address_default = "192.168.2.101";

  final static int ACTIVITY_SETTINGS = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    BackgroundGetRDS getlatestrds = new BackgroundGetRDS();

    //Find IP address of Raspberry Pi
    //region
    if (Build.FINGERPRINT.contains("generic"))
      ip_address = ip_address_default;   // Emulator
    else {   // Real device
      // Find IP address assigned to Raspberry Pi from Pixel Hotspot
      // ip neigh returns all IPs in neighborhood
      // One we want is 192.168.x.187 dev wlan1
      try {
        Process process = Runtime.getRuntime().exec("ip neigh");
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        int read;
        char[] buffer = new char[4096];
        StringBuffer output = new StringBuffer();
        while ((read = reader.read(buffer)) > 0) {
          output.append(buffer, 0, read);
        }
        reader.close();
        process.waitFor();
        String out = output.toString();
        int end_ind = out.indexOf(" dev wlan1");
        if (end_ind != -1) {
          out = out.substring(0, end_ind);
          int start_ind = out.lastIndexOf("192");
          if (start_ind != -1)
          ip_address = out.substring(start_ind);
          else{
            Toast.makeText(getApplicationContext(), "Can't determine IP address, check Wifi enabled", Toast.LENGTH_LONG).show();
            ip_address = ip_address_default;
          }
        }
        else{
          Toast.makeText(getApplicationContext(), "Can't determine IP address, check wifi enabled", Toast.LENGTH_LONG).show();
          ip_address = ip_address_default;
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    Log.i(TAG, "ip address of Raspberry Pi = " + ip_address);
    //endregion Find IP address of Raspberry Pi

    // Set colors from theme
    final TypedValue value = new TypedValue();
    getTheme().resolveAttribute(R.attr.colorPrimary, value, true);
    primaryColor = value.data;
    getTheme().resolveAttribute(R.attr.colorPrimaryVariant, value, true);
    primaryColorVariant = value.data;

    // Set up presets
    //region
    for (int i=0; i < n_preset_buttons; i++) {
      preset_button = (Button) findViewById(preset_button_ids[i]);
      preset_freq[i] = getResources().getString(preset_text_ids[i]);
      Log.i(TAG, "preset_freq[" + i + "] = " + preset_freq[i]);
      preset_button.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          int button_id = view.getId();
          int button_clicked_index = 0;
          while (button_id != preset_button_ids[button_clicked_index++]) ;
          button_clicked_index--;
          Log.i(TAG, "button_id = " + button_id + " button_clicked_index = " + button_clicked_index);
          url = "http://" + ip_address + ":5000/playradio";
          String freq_vol = preset_freq[button_clicked_index] + " 15";
          Log.i(TAG, "In preset onClick: url = " + url + "freq_vol = " + freq_vol);
          sendPost(url, freq_vol);
          // Set color of new selected button to primaryColorVariant, set previous selected button back to primaryColor
          if (currentStation != null) {
            if (currentStation < n_rds_buttons) {
              rds_button_current = (Button) findViewById(rds_button_ids[currentStation]);
              Log.i(TAG, "current_station = " + currentStation + " rds_button_current_id = " + rds_button_ids[currentStation]);
              rds_button_current.setBackgroundColor(primaryColor);
            } else {
              preset_button_current = (Button) findViewById(preset_button_ids[currentStation - n_rds_buttons]);
              Log.i(TAG, "current_station = " + currentStation + " preset_button_current_id = " + preset_button_ids[currentStation - n_rds_buttons]);
              preset_button_current.setBackgroundColor(primaryColor);
            }
          }
          preset_button = (Button) findViewById(button_id);
          preset_button.setBackgroundColor(primaryColorVariant);
          currentStation = button_clicked_index + n_rds_buttons;
        }
      });
    }
    //endregion Set up presets

    // Set up Exit button
    Button exitButton = findViewById(R.id.exitButton);
    exitButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        getlatestrds.cancel(true);
        Log.i(TAG, "cancel getlatestrds sent");
      }
    });

    // Start queue of network requests
    queue = Volley.newRequestQueue(this);

    // Send GET Request to start RDS scan program
    url = "http://" + ip_address + ":5000/startrds";
    sendGet(url);
    try {
      sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    // Launch process to read RDS data from RaspberryPi
    getlatestrds.execute();

  } // End OnCreate

  class BackgroundGetRDS extends AsyncTask<Void, Integer, String> {
    @Override
    protected String doInBackground(Void... voids) {
      while(!isCancelled()){
        url = "http://" + ip_address + ":5000/getrds";
        sendGet(url);
        try {
          sleep(5000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        publishProgress();
      }
      return "Executed";
    }

    @Override
    protected void onProgressUpdate(Integer... param){
      // Called with publishProgress() from doInBackground
      Log.i(TAG, "In onProgressUpdate: last_rds_info = " + last_rds_info + " index = " + index);
      if (!last_rds_info.equals(previous_rds_info)) {
        index = ++index % n_rds_buttons;
        previous_rds_info = last_rds_info;
        rds_info[index] = last_rds_info;
        rds_button = (Button) findViewById(rds_button_ids[index]);
        rds_button.setText(last_rds_info);
        if (last_rds_info.contains("*")) {
          rds_button.setTextColor(Color.YELLOW);
          ToneGenerator beep = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
          beep.startTone(ToneGenerator.TONE_PROP_BEEP);
        }
        else rds_button.setTextColor(Color.WHITE);
        rds_button.setBackgroundColor(primaryColor); // In case it is currentStation
        rds_button.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            int button_id = view.getId();
            int button_clicked_index = 0;
            while (button_id != rds_button_ids[button_clicked_index++]); button_clicked_index--;
            Log.i(TAG, "button_id = " +  button_id + " button_clicked_index = " + button_clicked_index);
            String freq_vol = rds_info[button_clicked_index].substring(0, 5) + " 15";
            Log.i(TAG, "In OnProgressUpdate: freq_vol = " + freq_vol);
            url = "http://" + ip_address + ":5000/playradio";
            sendPost(url, freq_vol);
            // Set color of new selected button to primaryColorVariant, set previous selected button back to primaryColor
            if (currentStation != null) {
              if (currentStation < n_rds_buttons) {
                rds_button_current = (Button) findViewById(rds_button_ids[currentStation]);
                Log.i(TAG, "current_station = " + currentStation + " rds_button_current_id = " + rds_button_ids[currentStation]);
                rds_button_current.setBackgroundColor(primaryColor);
              } else {
                preset_button_current = (Button) findViewById(preset_button_ids[currentStation - n_rds_buttons]);
                Log.i(TAG, "current_station = " + currentStation + " preset_button_current_id = " + preset_button_ids[currentStation - n_rds_buttons ]);
                preset_button_current.setBackgroundColor(primaryColor);
              }
            }
            rds_button = (Button) findViewById(button_id);
            rds_button.setBackgroundColor(primaryColorVariant);
            currentStation = button_clicked_index;
          }
        });
      }
    }

    @Override
    protected void onCancelled(){
      Log.i(TAG, "In onCancelled");
      queue.stop();
      System.exit(0);
    }
  } // End class BackgroundGetRDS

  private void sendGet(String url) {
    stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
      @Override
      public void onResponse(String response) {
        Log.i(TAG,"Get Response: " + response.toString());
        last_rds_info = response.toString();
      }
    }, new Response.ErrorListener() {
      @Override
      public void onErrorResponse(VolleyError error) {
        Log.i(TAG,"Get Response Error: " + error.toString());
      }
    });

    queue.add(stringRequest);
  }

  private void sendPost(String url, String freqvol_string) {
    stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
      @Override
      public void onResponse(String response) {
        Log.i(TAG,"Post Response: " + response.toString());
      }
    }, new Response.ErrorListener() {
      @Override
      public void onErrorResponse(VolleyError error) {
        Log.i("Info","Post Response Error: " + error.toString());
      }
    }){
      @Override
      protected Map<String, String> getParams() {
        Map<String, String> postData = new HashMap<String, String>();
        postData.put("freqvol", freqvol_string);
        return postData;
      }
    };

    queue.add(stringRequest);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();

    if (id == R.id.action_settings) {
      Intent intent = new Intent(this, SettingsActivity.class);
      startActivityForResult(intent, ACTIVITY_SETTINGS);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == ACTIVITY_SETTINGS) {
      if (resultCode == RESULT_OK) {
        if (data.getStringExtra("ip_address") != null){
          ip_address= data.getStringExtra("ip_address");
          Log.i(TAG, "Return from SettingsActivity ip_address=" + ip_address);
        }
        String[] temp_preset_freq = data.getStringArrayExtra("preset_freq");
        for (int i=0; i<n_preset_buttons; i++){
          if (temp_preset_freq[i] != null) {
            preset_freq[i] = temp_preset_freq[i];
            Log.i(TAG, "Return from SettingsActivity preset_freq[" + i + "]=" + preset_freq[i]);
            preset_button = (Button) findViewById(preset_button_ids[i]);
            preset_button.setText(preset_freq[i]);
          }
        }
      }
    }
  } // End OnActivityResult
}  // End MainActivity
