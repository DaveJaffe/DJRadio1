package com.example.djradio1;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import static com.example.djradio1.MainActivity.n_preset_buttons;
import static com.example.djradio1.MainActivity.preset_freq;

public class SettingsActivity extends AppCompatActivity {
  private static final String TAG = SettingsActivity.class.getName();
  String ip_address;
  private static final int[] preset_edit_text_ids = {R.id.preset1EditText, R.id.preset2EditText,
      R.id.preset3EditText, R.id.preset4EditText};
  private String[] temp_preset_freq = new String[n_preset_buttons];

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.settings);

    // Set up edit for ip address
    EditText ipAddressEdit = (EditText)findViewById(R.id.ipAddressEditText);
    ipAddressEdit.setText("192.168.x.y");
    ipAddressEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
          ip_address = view.getText().toString();
//        Toast.makeText(SettingsActivity.this, ip_address, Toast.LENGTH_SHORT).show();
          return true;
        }
        return false;
      }
    });

    // Set up edit for presets
    for (int i=0; i < n_preset_buttons; i++) {
      EditText presetEdit = (EditText) findViewById(preset_edit_text_ids[i]);
      presetEdit.setText(preset_freq[i]);
      presetEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
          if (actionId == EditorInfo.IME_ACTION_DONE) {
            int textview_id = view.getId();
            int textview_index = 0;
            while (textview_id != preset_edit_text_ids[textview_index++]) ;
            textview_index--;
            Log.i(TAG, "textview_id = " + textview_id + " textview_index = " + textview_index);
            temp_preset_freq[textview_index] = view.getText().toString();
//          Toast.makeText(SettingsActivity.this, temp_preset_freq[textview_index], Toast.LENGTH_SHORT).show();
            return true;
          }
          return false;
        }
      });
    }

    Button cancelButton = findViewById(R.id.cancelButton);
    cancelButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent cancelIntent = new Intent();
        setResult(RESULT_CANCELED, cancelIntent);
        finish();
      }
    });
    
    Button setButton = findViewById(R.id.setButton);
    setButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Intent setIntent = new Intent();
        // Check ip_address
        boolean ip_address_good = true;
        if (ip_address != null) {
          String[] octet_str = ip_address.split("\\.", 4);
          for (int i = 0; i < 4; i++) {
//            Log.i(TAG, "octet_str[" + i + "]=" + octet_str[i]);
            int octet_int = Integer.parseInt(octet_str[i]);
//            Log.i(TAG, "octet_int=" + octet_int);
            if (octet_int < 0 | octet_int > 255) ip_address_good = false;
          }
        }
        // Check frequency
        List<String> legal_freqs = new ArrayList<String>();
        for (double freq = 87.7; freq < 108; freq += .2) {
          legal_freqs.add(String.format("%.1f", freq));
        }
        boolean preset_freq_good = true;
        for (int i=0; i<n_preset_buttons; i++) {
          if (temp_preset_freq[i] != null) preset_freq_good = legal_freqs.contains(temp_preset_freq[i]);
        }

        if (!ip_address_good) {
          Toast.makeText(SettingsActivity.this, "Bad IP address", Toast.LENGTH_SHORT).show();
        }
        else if(!preset_freq_good) {
          Toast.makeText(SettingsActivity.this, "Bad FM frequency", Toast.LENGTH_SHORT).show();
        }
        else{
          setIntent.putExtra("ip_address", ip_address);
          setIntent.putExtra("preset_freq", temp_preset_freq);
          setResult(RESULT_OK, setIntent);
          finish();
        }
      }
    });

  }
} // End SettingsActivity