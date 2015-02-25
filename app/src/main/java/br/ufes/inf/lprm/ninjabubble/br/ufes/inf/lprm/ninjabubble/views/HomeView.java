package br.ufes.inf.lprm.ninjabubble.br.ufes.inf.lprm.ninjabubble.views;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import br.ufes.inf.lprm.ninjabubble.R;

/**
 * Created by Juan on 09/02/2015.
 */
public class HomeView extends ContentView {
    public final String TAG = "NinjaBubbleMagic/HomeView";

    public Button bStreamInit;
    public Button bStreamPause;
    public Button bStreamResume;
    public Button bStreamClose;

    public Button bGroupMatch;  // works both for streamInit (new streams) and groupJoin (running streams)
    public Button bGroupLeave;

    public long mLastLookup = 0;
    public final long LOOKUP_INTERVAL = 1000 * 10;
    public Dialog mLookupDialog;
    private double mLookupRadius = 5.0;

    public HomeView(Context context, final OverlayView overlayView) {
        super(context, overlayView);

        Log.i(TAG, "created");
    }

    @Override
    public void show() {
        super.show();

        if (mHasLoaded) {
            showLoaded();
            return;
        }
        else {
            mHasLoaded = true;
        }

        /**
         * streamInit
         */
        {
            bStreamInit = new Button(getContext());
            bStreamInit.setText(R.string.btn_stream_init);
            bStreamInit.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOverlayView.mService.getLastKnownLocation();
                    if (mOverlayView.mService.mLatlng == null) {
                        Toast.makeText(getContext(), R.string.error_location, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(R.string.btn_stream_init);
                    builder.setMessage(R.string.alert_stream_init);

                    final EditText txtHashtags = new EditText(getContext());
                    builder.setView(txtHashtags);

                    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (txtHashtags.getText().toString().isEmpty()) {
                                Toast.makeText(getContext(), R.string.error_streaminit_hashtags, Toast.LENGTH_SHORT).show();
                                return;
                            }

                            mOverlayView.mService.mHashtags = txtHashtags.getText().toString();

                            showLoading();
                            mOverlayView.mService.runConcurrentThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        mOverlayView.mService.mMapperChannel.connect();
                                        mOverlayView.mService.mMapperChannel.streamStatus();

                                        mOverlayView.mService.startLocationListener();
                                        mOverlayView.mService.startOrientationListener();

                                        mOverlayView.mService.mMapperChannel.streamInit(mOverlayView.mService.mMedia, null);

                                        try {
                                            mOverlayView.mService.mPartyChannel.join(mOverlayView.mService.mStream.getString("group_jid"));
                                        } catch (Exception e) {
                                            Log.e(TAG, "streamInit", e);
                                            Toast.makeText(getContext(), R.string.error_groupjoin, Toast.LENGTH_SHORT).show();
                                        }

                                        mOverlayView.mService.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                bStreamInit.setVisibility(GONE);
                                                bStreamPause.setVisibility(VISIBLE);
                                                bStreamClose.setVisibility(VISIBLE);

                                                bGroupLeave.setVisibility(VISIBLE);

                                                mOverlayView.imHome.setImageBitmap(mOverlayView.mBmpOn);

                                                mOverlayView.enableMenu();
                                                showLoaded();
                                            }
                                        });
                                    } catch (Exception e) {
                                        Log.e(TAG, "streamInit", e);
                                        Toast.makeText(getContext(), R.string.error_streaminit, Toast.LENGTH_SHORT).show();
                                        mOverlayView.mService.mMapperChannel.disconnect();

                                        mOverlayView.mService.stopLocationListener();
                                        mOverlayView.mService.stopOrientationListener();

                                        mOverlayView.mService.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                showLoaded();
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    });
                    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            return;
                        }
                    });

                    Dialog dialog = builder.create();
                    dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                    dialog.show();
                }
            });
            mContentLayout.addView(bStreamInit);
        }

        /**
         * streamPause
         */
        {
            bStreamPause = new Button(getContext());
            bStreamPause.setText(R.string.btn_stream_pause);
            bStreamPause.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showLoading();
                    mOverlayView.mService.runConcurrentThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mOverlayView.mService.mMapperChannel.streamPause();
                                mOverlayView.mService.mPartyChannel.leave();
                                mOverlayView.mService.mMapperChannel.disconnect();

                                mOverlayView.mService.stopLocationListener();
                                mOverlayView.mService.stopOrientationListener();

                                mOverlayView.mService.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        bStreamPause.setVisibility(GONE);
                                        bStreamResume.setVisibility(VISIBLE);

                                        bGroupMatch.setVisibility(GONE);
                                        bGroupLeave.setVisibility(GONE);

                                        mOverlayView.imHome.setImageBitmap(mOverlayView.mBmpOff);

                                        mOverlayView.disableMenu();
                                        showLoaded();
                                    }
                                });
                            } catch (Exception e) {
                                Log.e(TAG, "streamPause", e);
                                Toast.makeText(getContext(), R.string.error_streampause, Toast.LENGTH_SHORT).show();
                                mOverlayView.mService.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        showLoaded();
                                    }
                                });
                            }
                        }
                    });
                }
            });
            mContentLayout.addView(bStreamPause);
        }

        /**
         * streamResume
         */
        {
            bStreamResume = new Button(getContext());
            bStreamResume.setText(R.string.btn_stream_resume);
            bStreamResume.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOverlayView.mService.getLastKnownLocation();
                    if (mOverlayView.mService.mLatlng == null) {
                        Toast.makeText(getContext(), R.string.error_location, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showLoading();
                    mOverlayView.mService.runConcurrentThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mOverlayView.mService.mMapperChannel.connect();
                                mOverlayView.mService.mMapperChannel.streamStatus();

                                mOverlayView.mService.startLocationListener();
                                mOverlayView.mService.startOrientationListener();

                                mOverlayView.mService.mMapperChannel.streamResume();

                                try {
                                    mOverlayView.mService.mPartyChannel.join(mOverlayView.mService.mStream.getString("group_jid"));

                                    /*

                                     */
                                } catch (Exception e) {
                                    Log.e(TAG, "streamResume", e);
                                    Toast.makeText(getContext(), R.string.error_groupjoin, Toast.LENGTH_SHORT).show();
                                }

                                mOverlayView.mService.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        bStreamPause.setVisibility(VISIBLE);
                                        bStreamResume.setVisibility(GONE);

                                        bGroupMatch.setVisibility(VISIBLE);
                                        bGroupLeave.setVisibility(VISIBLE);

                                        mOverlayView.imHome.setImageBitmap(mOverlayView.mBmpOn);

                                        mOverlayView.enableMenu();
                                        showLoaded();
                                    }
                                });
                            } catch (Exception e) {
                                Log.e(TAG, "streamResume", e);
                                Toast.makeText(getContext(), R.string.error_streamresume, Toast.LENGTH_SHORT).show();
                                mOverlayView.mService.mMapperChannel.disconnect();

                                mOverlayView.mService.stopLocationListener();
                                mOverlayView.mService.stopOrientationListener();

                                mOverlayView.mService.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        showLoaded();
                                    }
                                });
                            }
                        }
                    });
                }
            });
            mContentLayout.addView(bStreamResume);
        }

        /**
         * streamClose
         */
        {
            bStreamClose = new Button(getContext());
            bStreamClose.setText(R.string.btn_stream_close);
            bStreamClose.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(R.string.alert_stream_close);
                    builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showLoading();
                            mOverlayView.mService.runConcurrentThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if(mOverlayView.mService.mStream.getString("status").equals("paused")) {
                                            mOverlayView.mService.mMapperChannel.connect();
                                        }

                                        mOverlayView.mService.mMapperChannel.streamClose();
                                        mOverlayView.mService.mPartyChannel.leave();
                                        mOverlayView.mService.mMapperChannel.disconnect();

                                        mOverlayView.mService.stopLocationListener();
                                        mOverlayView.mService.stopOrientationListener();

                                        mOverlayView.mService.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                bStreamInit.setVisibility(VISIBLE);
                                                bStreamPause.setVisibility(GONE);
                                                bStreamResume.setVisibility(GONE);
                                                bStreamClose.setVisibility(GONE);

                                                bGroupMatch.setVisibility(VISIBLE);
                                                bGroupLeave.setVisibility(GONE);

                                                mOverlayView.imHome.setImageBitmap(mOverlayView.mBmpOff);

                                                mOverlayView.disableMenu();
                                                showLoaded();
                                            }
                                        });
                                    } catch (Exception e) {
                                        Log.e(TAG, "streamClose", e);
                                        Toast.makeText(getContext(), R.string.error_streamclose, Toast.LENGTH_SHORT).show();
                                        mOverlayView.mService.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                showLoaded();
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    });
                    builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    Dialog dialog = builder.create();
                    dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                    dialog.show();
                }
            });
            mContentLayout.addView(bStreamClose);
        }

        /**
         * groupMatch
         */
        {
            bGroupMatch = new Button(getContext());
            bGroupMatch.setText(R.string.btn_group_match);
            bGroupMatch.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOverlayView.mService.getLastKnownLocation();
                    if (mOverlayView.mService.mLatlng == null) {
                        Toast.makeText(getContext(), R.string.error_location, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showLoading();
                    mOverlayView.mService.runConcurrentThread(new Runnable() {
                        @Override
                        public void run() {

                            boolean msgError = false;
                            boolean jsonError = false;

                            if (System.currentTimeMillis() - mLastLookup > LOOKUP_INTERVAL) {

                                try {
                                    if (mOverlayView.mService.mStream != null) {
                                        mOverlayView.mService.mMapperChannel.groupMatch(mLookupRadius, mOverlayView.mService.mStream.getString("group_jid"));
                                    }
                                    else {
                                        mOverlayView.mService.mMapperChannel.connect();
                                        mOverlayView.mService.mMapperChannel.groupMatch(mLookupRadius, null);
                                        mOverlayView.mService.mMapperChannel.disconnect();
                                    }
                                    mLastLookup = System.currentTimeMillis();
                                } catch (Exception e) {
                                    Log.e(TAG, "groupMatch", e);
                                    Toast.makeText(getContext(), R.string.error_groupmatch, Toast.LENGTH_SHORT).show();
                                }
                            }

                            if (!msgError) {
                                JSONArray matchedGroups = mOverlayView.mService.mMatchedGroups;
                                if (matchedGroups != null && matchedGroups.length() > 0) {
                                    ListView modeList = new ListView(getContext());
                                    String[] stringArray = new String[matchedGroups.length()];
                                    for (int i = 0; i < matchedGroups.length(); i++) {
                                        try {
                                            JSONObject group = matchedGroups.getJSONObject(i);
                                            String hashtags = group.getString("hashtags");
                                            String numMembers = group.getString("num_members");
                                            stringArray[i] = String.format("%s (%s)", hashtags, numMembers);
                                        } catch (Exception e) {
                                            Log.e(TAG, "groupMatch", e);
                                            Toast.makeText(getContext(), R.string.error_groupmatch, Toast.LENGTH_SHORT).show();
                                            jsonError = true;
                                            break;
                                        }
                                    }
                                    if (!jsonError) {
                                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                        builder.setTitle(R.string.alert_matched_groups);

                                        ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, android.R.id.text1, stringArray);
                                        modeList.setAdapter(modeAdapter);
                                        modeList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                            @Override
                                            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                                builder.setTitle(R.string.alert_group_join);
                                                builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        // stream_init OR group_join
                                                        final String groupJid;
                                                        try {
                                                            JSONObject group = mOverlayView.mService.mMatchedGroups.getJSONObject(position);
                                                            groupJid = group.getString("group_jid");
                                                        } catch (Exception e) {
                                                            if (mOverlayView.mService.mStream != null) {
                                                                Toast.makeText(getContext(), R.string.error_groupjoin, Toast.LENGTH_SHORT).show();
                                                            } else {
                                                                Toast.makeText(getContext(), R.string.error_streaminit, Toast.LENGTH_SHORT).show();
                                                            }
                                                            return;
                                                        }

                                                        mLookupDialog.dismiss();

                                                        if (mOverlayView.mService.mStream != null) {
                                                            // group_join
                                                            mOverlayView.mService.runOnUiThread(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    groupMatchGroupJoin(groupJid);
                                                                }
                                                            });
                                                        } else {
                                                            // stream_init
                                                            mOverlayView.mService.runOnUiThread(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    groupMatchStreamInit(groupJid);
                                                                }
                                                            });
                                                        }
                                                    }
                                                });
                                                builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        Log.i(TAG, String.format("cancel", position));
                                                    }
                                                });
                                                Dialog dialog = builder.create();
                                                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                                                dialog.show();
                                            }
                                        });
                                        builder.setView(modeList);

                                        mLookupDialog = builder.create();
                                        mLookupDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                                        mLookupDialog.show();
                                    }
                                } else {
                                    Toast.makeText(getContext(), R.string.error_groupmatch_empty, Toast.LENGTH_SHORT).show();
                                }
                            }

                            mOverlayView.mService.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showLoaded();
                                }
                            });
                        }
                    });
                }
            });
            mContentLayout.addView(bGroupMatch);
        }

        /**
         * groupLeave
         */
        {
            bGroupLeave = new Button(getContext());
            bGroupLeave.setText(R.string.btn_group_leave);
            bGroupLeave.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOverlayView.mService.mLatlng == null) {
                        Toast.makeText(getContext(), R.string.error_location, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle(R.string.alert_group_leave);
                    builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            showLoading();
                            mOverlayView.mService.runConcurrentThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        mOverlayView.mService.mMapperChannel.groupLeave();
                                        Toast.makeText(getContext(), R.string.success_groupleave, Toast.LENGTH_SHORT).show();

                                        try {
                                            mOverlayView.mService.mPartyChannel.join(mOverlayView.mService.mStream.getString("group_jid"));
                                        } catch (Exception e) {
                                            Log.e(TAG, "groupLeave", e);
                                            Toast.makeText(getContext(), R.string.error_groupjoin, Toast.LENGTH_SHORT).show();
                                        }

                                        mOverlayView.mService.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                showLoaded();
                                            }
                                        });
                                    } catch (Exception e) {
                                        Log.e(TAG, "groupLeave", e);
                                        Toast.makeText(getContext(), R.string.error_groupleave, Toast.LENGTH_SHORT).show();
                                        mOverlayView.mService.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                showLoaded();
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    });
                    builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    Dialog dialog = builder.create();
                    dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                    dialog.show();
                }
            });
            mContentLayout.addView(bGroupLeave);
        }

        if (mOverlayView.mService.mStream != null) {
            bStreamInit.setVisibility(GONE);
            bStreamPause.setVisibility(GONE);
            bStreamResume.setVisibility(VISIBLE);
            bStreamClose.setVisibility(VISIBLE);

            bGroupMatch.setVisibility(GONE);
            bGroupLeave.setVisibility(GONE);
        } else {
            bStreamInit.setVisibility(VISIBLE);
            bStreamPause.setVisibility(GONE);
            bStreamResume.setVisibility(GONE);
            bStreamClose.setVisibility(GONE);

            bGroupMatch.setVisibility(VISIBLE);
            bGroupLeave.setVisibility(GONE);
        }

        showLoaded();
    }

    public void groupMatchStreamInit (final String groupJid) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.btn_stream_init);
        builder.setMessage(R.string.alert_stream_init);

        final EditText txtHashtags = new EditText(getContext());
        builder.setView(txtHashtags);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (txtHashtags.getText().toString().isEmpty()) {
                    Toast.makeText(getContext(), R.string.error_streaminit_hashtags, Toast.LENGTH_SHORT).show();
                    return;
                }

                mOverlayView.mService.mHashtags = txtHashtags.getText().toString();

                showLoading();
                mOverlayView.mService.runConcurrentThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            mOverlayView.mService.mMapperChannel.connect();
                            mOverlayView.mService.mMapperChannel.streamStatus();

                            mOverlayView.mService.startLocationListener();
                            mOverlayView.mService.startOrientationListener();

                            mOverlayView.mService.mMapperChannel.streamInit(mOverlayView.mService.mMedia, groupJid);

                            try {
                                mOverlayView.mService.mPartyChannel.join(mOverlayView.mService.mStream.getString("group_jid"));
                            } catch (Exception e) {
                                Log.e(TAG, "streamInit", e);
                                Toast.makeText(getContext(), R.string.error_groupjoin, Toast.LENGTH_SHORT).show();
                            }

                            mOverlayView.mService.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mOverlayView.enableMenu();

                                    bStreamInit.setVisibility(GONE);
                                    bStreamPause.setVisibility(VISIBLE);
                                    bStreamClose.setVisibility(VISIBLE);

                                    bGroupLeave.setVisibility(VISIBLE);

                                    mOverlayView.imHome.setImageBitmap(mOverlayView.mBmpOn);

                                    showLoaded();
                                }
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "streamInit", e);
                            Toast.makeText(getContext(), R.string.error_streaminit, Toast.LENGTH_SHORT).show();
                            mOverlayView.mService.mMapperChannel.disconnect();

                            mOverlayView.mService.stopLocationListener();
                            mOverlayView.mService.stopOrientationListener();

                            mOverlayView.mService.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showLoaded();
                                }
                            });
                        }
                    }
                });
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });

        Dialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
    }

    public void groupMatchGroupJoin(final String groupJid) {
        showLoading();
        mOverlayView.mService.runConcurrentThread(new Runnable() {
            @Override
            public void run() {
                try {
                    mOverlayView.mService.mMapperChannel.groupJoin(groupJid);
                    Toast.makeText(getContext(), R.string.success_groupjoin, Toast.LENGTH_SHORT).show();

                    try {
                        mOverlayView.mService.mPartyChannel.join(mOverlayView.mService.mStream.getString("group_jid"));
                    } catch (Exception e) {
                        Log.e(TAG, "groupLeave", e);
                        Toast.makeText(getContext(), R.string.error_groupjoin, Toast.LENGTH_SHORT).show();
                    }

                    mOverlayView.mService.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showLoaded();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "groupLeave", e);
                    Toast.makeText(getContext(), R.string.error_groupleave, Toast.LENGTH_SHORT).show();
                    mOverlayView.mService.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showLoaded();
                        }
                    });
                }
            }
        });
    }
}
