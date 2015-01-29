package br.ufes.inf.lprm.sensorninja;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.ToggleButton;

public class ActivityPreferencias extends Activity {
	static final String TAG = "NINJA-CONF";

	private static int mServiceStatus = 0;

	private static int pLocationFreq;
	private static String pTwitter;
	private static String pUrl;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_preferencias_numerico);

		final NumberPicker vHora = (NumberPicker) findViewById(R.id.npHH);
		final NumberPicker vMin = (NumberPicker) findViewById(R.id.npMM);
		final NumberPicker vSeg = (NumberPicker) findViewById(R.id.npSS);

		final EditText vTwitter = (EditText) findViewById(R.id.text_twitter);
		final EditText vUrl = (EditText) findViewById(R.id.text_url);

		NumberPicker.Formatter tf = new NumberPicker.Formatter() {

			@Override
			public String format(int value) {
				if (value < 10)
					return "0" + value;
				else
					return Integer.toString(value);
			}
		};

		vHora.setFormatter(tf);
		// vHora.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		vHora.setMaxValue(48);
		vHora.setMinValue(0);
		vHora.setOnLongPressUpdateInterval(50);

		vMin.setFormatter(tf);
		// vMin.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		vMin.setMaxValue(59);
		vMin.setMinValue(0);
		vMin.setOnLongPressUpdateInterval(50);

		vSeg.setFormatter(tf);
		// vSeg.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
		vSeg.setMaxValue(59);
		vSeg.setMinValue(0);
		vSeg.setOnLongPressUpdateInterval(50);

		ToggleButton toggleService = (ToggleButton) findViewById(R.id.toggleButton_service);
		toggleService.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (((ToggleButton) v).isChecked()) {

					pTwitter = vTwitter.getText().toString();
					pUrl = vUrl.getText().toString();

					int mHora = (vHora.getValue() * 60 * 60 * 1000);
					int mMin = (vMin.getValue() * 60 * 1000);
					int mSeg = (vSeg.getValue() * 1000);

					pLocationFreq = mHora + mMin + mSeg;

					Intent intent = new Intent(ActivityPreferencias.this,
							ServiceNinja.class)
							.putExtra("locationFreq", pLocationFreq)
							.putExtra("twitter", pTwitter)
							.putExtra("url", pUrl);
					startService(intent);
					mServiceStatus = 1;
				} else {
					stopService(new Intent(ActivityPreferencias.this,
							ServiceNinja.class));
					if (pUrl.isEmpty()) {
						SQLiteHelper.exportDB();
					}
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
