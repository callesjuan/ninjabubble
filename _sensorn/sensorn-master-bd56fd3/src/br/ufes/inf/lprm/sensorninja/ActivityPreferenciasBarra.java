package br.ufes.inf.lprm.sensorninja;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ToggleButton;

public class ActivityPreferenciasBarra extends Activity {
	static final String TAG = "NINJA-CONF";

	private static int mServiceStatus = 0;

	private static int pLocationFreq;
	private static String pTwitter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_preferencias_numerico);

		final SeekBar vLocationFreq = (SeekBar) findViewById(R.id.seekbar_location);
		final EditText vTwitter = (EditText) findViewById(R.id.text_twitter);

		final String freqStr[] = { "n/a", "10 min", "5 min", "2 min", "1 min",
				"30 s", "10 s" };
		final int[] freq = new int[] { -1, 10 * 60 * 1000, 5 * 60 * 1000,
				2 * 60 * 1000, 60 * 1000, 30 * 1000, 10 * 1000 };

		final TextView lblLocationFreq = (TextView) findViewById(R.id.label_location_freq);

		vLocationFreq.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				lblLocationFreq.setText("(" + freqStr[progress] + ")");
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
		});

		ToggleButton toggleService = (ToggleButton) findViewById(R.id.toggleButton_service);
		toggleService.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (((ToggleButton) v).isChecked()) {

					pLocationFreq = freq[vLocationFreq.getProgress()];
					pTwitter = vTwitter.getText().toString();

					Intent intent = new Intent(ActivityPreferenciasBarra.this,
							ServiceNinja.class).putExtra("locationFreq",
							pLocationFreq).putExtra("twitter", pTwitter);
					startService(intent);
					mServiceStatus = 1;
				} else {
					stopService(new Intent(ActivityPreferenciasBarra.this,
							ServiceNinja.class));
					mServiceStatus = 0;
				}
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();

		ToggleButton toggleService = (ToggleButton) findViewById(R.id.toggleButton_service);
		if (mServiceStatus == 1) {
			toggleService.setChecked(true);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_config, menu);
		return true;
	}

}
