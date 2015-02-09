package br.ufes.inf.lprm.ninjabubble;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

//import com.premnirmal.Magnet.IconCallback;
//import com.premnirmal.Magnet.Magnet;


public class MainActivity extends Activity {

    private String TAG = "NinjaBubble";

    private boolean mServiceRunning;
    private DataUpdateReceiver mDataUpdateReceiver;

    private String mJID;
    private String mPWD;
    private String mChannel;
    private String mMedia;

    public final static String ERROR_XMPP_CONNECTION = "ERROR_XMPP_CONNECTION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SharedPreferences settings = getSharedPreferences("prefs", 0);
        if (settings.contains("serviceRunning")) {
//            Log.i(TAG, "__getSharedPreferences");
            mServiceRunning = settings.getBoolean("serviceRunning", false);
            mJID = settings.getString("jid", "");
            mPWD = settings.getString("pwd", "");
            mChannel = settings.getString("channel", "");
            mMedia = settings.getString("media", "");
            settings.edit().clear().commit();
        }

        final EditText vJID = (EditText) findViewById(R.id.txt_jid);
        final EditText vPWD = (EditText) findViewById(R.id.txt_pwd);
        final EditText vChannel = (EditText) findViewById(R.id.txt_channel);

        final Spinner vMedia = (Spinner) findViewById(R.id.spin_media);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.spin_media, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vMedia.setAdapter(adapter);

        final ToggleButton toggleService = (ToggleButton) findViewById(R.id.toggle_magic);
        toggleService.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (((ToggleButton) v).isChecked()) {

                    try {
                        mJID = vJID.getText().toString();
                        mPWD = vPWD.getText().toString();
                        mChannel = vChannel.getText().toString();
                        mMedia = vMedia.getSelectedItem().toString();

                        if (mJID.isEmpty() || mPWD.isEmpty() || mChannel.isEmpty()) {
                            Log.e(TAG, "required fields missing");
                            Toast.makeText(MainActivity.this, "All inputs are required", Toast.LENGTH_SHORT).show();
                            toggleService.setChecked(false);
                            return;
                        }

                        try {

                            /**
                             * Connect to XMPP server
                             */

                            Log.i(TAG, "passed through XMPP server authentication");

                            /**
                             * Start Service
                             */

                            Intent intent = new Intent(MainActivity.this,
                                NinjaBubbleMagic.class)
                                .putExtra("jid", mJID)
                                .putExtra("pwd", mPWD)
                                .putExtra("channel", mChannel)
                                .putExtra("media", mMedia);
                            startService(intent);
                            mServiceRunning = true;

                            Log.i(TAG, "NinjaBubbleMagic has started");

                            vJID.setFocusable(false);
                            vPWD.setFocusable(false);
                            vChannel.setFocusable(false);
                            vMedia.setClickable(false);
                        } catch (Exception e) {
                            Log.e(TAG, "failed to authenticate at the XMPP server");
                        }
                    } catch (Exception e) {

                    }
                } else {
                    stopService(new Intent(MainActivity.this,
                            NinjaBubbleMagic.class));
                    mServiceRunning = false;

                    Log.i(TAG, "NinjaBubbleMagic has stopped");

                    vJID.setFocusable(true);
                    vPWD.setFocusable(true);
                    vChannel.setFocusable(true);
                    vMedia.setClickable(true);
                }
            }
        });

        vJID.setText("src1@juancalles.ddns.net");
        vPWD.setText("123");
        vChannel.setText("callesjuan");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mJID != null)
            ((EditText) findViewById(R.id.txt_jid)).setText(mJID);
        if (mPWD != null)
            ((EditText) findViewById(R.id.txt_pwd)).setText(mPWD);
        if (mChannel != null)
            ((EditText) findViewById(R.id.txt_channel)).setText(mChannel);
        if (mMedia != null) {
            try {
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.spin_media, android.R.layout.simple_spinner_item);
                ((Spinner) findViewById(R.id.spin_media)).setSelection(adapter.getPosition(mMedia));
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, String.format("index for \"%s\" does not exist in the media spinner"));
            }
        }

        if (mServiceRunning) {
            ((ToggleButton) findViewById(R.id.toggle_magic)).setChecked(true);

            ((EditText) findViewById(R.id.txt_jid)).setFocusable(false);
            ((EditText) findViewById(R.id.txt_pwd)).setFocusable(false);
            ((EditText) findViewById(R.id.txt_channel)).setFocusable(false);
            ((Spinner) findViewById(R.id.spin_media)).setClickable(false);
        } else {
            ((ToggleButton) findViewById(R.id.toggle_magic)).setChecked(false);

            ((EditText) findViewById(R.id.txt_jid)).setFocusable(true);
            ((EditText) findViewById(R.id.txt_pwd)).setFocusable(true);
            ((EditText) findViewById(R.id.txt_channel)).setFocusable(true);
            ((Spinner) findViewById(R.id.spin_media)).setClickable(true);
        }

        if (mDataUpdateReceiver == null) {
            mDataUpdateReceiver = new DataUpdateReceiver();
        }
        IntentFilter intentFilter = new IntentFilter(ERROR_XMPP_CONNECTION);
        registerReceiver(mDataUpdateReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "__onPause");
        if (mDataUpdateReceiver != null) {
            unregisterReceiver(mDataUpdateReceiver);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "__onStop");

        if (mServiceRunning) {
            SharedPreferences settings = getSharedPreferences("prefs", 0);
            SharedPreferences.Editor editor = settings.edit();

            editor.putBoolean("serviceRunning", mServiceRunning);
            editor.putString("jid", mJID);
            editor.putString("pwd", mPWD);
            editor.putString("channel", mChannel);
            editor.putString("media", mMedia);
            editor.commit();

            Log.i(TAG, "__putSharedPreferences");
        }
    }

    private class DataUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ERROR_XMPP_CONNECTION)) {
                Log.e(TAG, intent.getStringExtra("value"));
                Toast.makeText(MainActivity.this, intent.getStringExtra("value"), Toast.LENGTH_SHORT).show();

                ((ToggleButton) findViewById(R.id.toggle_magic)).setChecked(false);

                ((EditText) findViewById(R.id.txt_jid)).setFocusable(true);
                ((EditText) findViewById(R.id.txt_pwd)).setFocusable(true);
                ((EditText) findViewById(R.id.txt_channel)).setFocusable(true);
                ((Spinner) findViewById(R.id.spin_media)).setClickable(true);

                mServiceRunning = false;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
