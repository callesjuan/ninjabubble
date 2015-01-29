package br.ufes.inf.lprm.ninjabubble;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.premnirmal.Magnet.IconCallback;
import com.premnirmal.Magnet.Magnet;


public class MainActivityBackup extends ActionBarActivity {

    private String TAG = "NinjaBubble";
    private Context mContext = this;
    private Magnet mMagnet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        ImageView iconView = new ImageView(this);
        iconView.setImageResource(R.drawable.ic_launcher);
        mMagnet = new Magnet.Builder(this)
                .setIconView(iconView) // required
                .setIconCallback(mIconCallback)
                .setRemoveIconResId(R.drawable.trash)
                .setRemoveIconShadow(R.drawable.bottom_shadow)
                .setShouldFlingAway(true)
                .setShouldStickToWall(true)
                .setRemoveIconShouldBeResponsive(true)
                .build();
        mMagnet.show();
    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            mMagnet.destroy();
        }
        catch(IllegalArgumentException e) {
            Log.i(TAG, "mIconView already removed");
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

    private IconCallback mIconCallback = new IconCallback() {
        @Override
        public void onFlingAway() {
            Log.i(TAG, "onFlingAway");
        }

        @Override
        public void onMove(float x, float y) {
            // Log.i(TAG, "onMove(" + x + "," + y + ")");
        }

        @Override
        public void onIconClick(View icon, float iconXPose, float iconYPose) {
            Log.i(TAG, "onIconClick(..)");

            mMagnet.destroy();

            ImageView trashView = new ImageView(mContext);
            trashView.setImageResource(R.drawable.trash);

            mMagnet = new Magnet.Builder(mContext)
                    .setIconView(trashView) // required
                    .setIconCallback(mTrashCallback)
                    .setRemoveIconResId(R.drawable.trash)
                    .setRemoveIconShadow(R.drawable.bottom_shadow)
                    .setShouldFlingAway(true)
                    .setShouldStickToWall(true)
                    .setRemoveIconShouldBeResponsive(true)
                    .build();
            mMagnet.show();
        }

        @Override
        public void onIconDestroyed() {
            Log.i(TAG, "onIconDestroyed()");
        }
    };

    private IconCallback mTrashCallback = new IconCallback() {
        @Override
        public void onFlingAway() {}

        @Override
        public void onMove(float v, float v2) {}

        @Override
        public void onIconClick(View view, float v, float v2) {
            Log.i(TAG, "onTrashClick(..)");
        }

        @Override
        public void onIconDestroyed() {}
    };
}
