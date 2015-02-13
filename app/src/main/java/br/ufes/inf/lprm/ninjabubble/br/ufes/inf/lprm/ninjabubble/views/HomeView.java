package br.ufes.inf.lprm.ninjabubble.br.ufes.inf.lprm.ninjabubble.views;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import br.ufes.inf.lprm.ninjabubble.MapperChannel;
import br.ufes.inf.lprm.ninjabubble.R;

/**
 * Created by Juan on 09/02/2015.
 */
public class HomeView extends LinearLayout {
    public final String TAG = "NinjaBubbleMagic/HomeView";

    public OverlayView mOverlayView;

    public MinimapView vMinimap;
    public ChatView vChat;
    public PartyView vParty;

    public Button bStreamInit;
    public Button bStreamPause;
    public Button bStreamResume;
    public Button bStreamClose;

    public Button bGroupMatch;  // works both for streamInit (new streams) and groupJoin (running streams)
    public Button bGroupLeave;

    public long mLastLookup = 0;
    public final long LOOKUP_INTERVAL = 1000 * 30;
    public Dialog mLookupDialog;

    public HomeView(Context context, OverlayView overlayView) {
        super(context);
        mOverlayView = overlayView;

        Log.i(TAG, "created");

        setGravity(Gravity.CENTER);
        setOrientation(VERTICAL);

        setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        bStreamInit = new Button(getContext());
        bStreamInit.setText(R.string.btn_stream_init);
        bStreamInit.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mOverlayView.mService.mMapperChannel.streamInit();

                    bStreamInit.setVisibility(GONE);
                    bStreamPause.setVisibility(VISIBLE);
                    bStreamClose.setVisibility(VISIBLE);

                    bGroupLeave.setVisibility(VISIBLE);

                    mOverlayView.enableMenu();
                } catch (Exception e) {
                    Log.e(TAG, "streamInit", e);
                }
            }
        });
        addView(bStreamInit);

        bStreamPause = new Button(getContext());
        bStreamPause.setText(R.string.btn_stream_pause);
        bStreamPause.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mOverlayView.mService.mMapperChannel.streamPause();

                    bStreamPause.setVisibility(GONE);
                    bStreamResume.setVisibility(VISIBLE);

                    bGroupMatch.setVisibility(GONE);
                    bGroupLeave.setVisibility(GONE);

                    mOverlayView.disableMenu();
                } catch (Exception e) {
                    Log.e(TAG, "streamPause", e);
                }
            }
        });
        addView(bStreamPause);

        bStreamResume = new Button(getContext());
        bStreamResume.setText(R.string.btn_stream_resume);
        bStreamResume.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mOverlayView.mService.mMapperChannel.streamResume();

                    bStreamPause.setVisibility(VISIBLE);
                    bStreamResume.setVisibility(GONE);

                    bGroupMatch.setVisibility(VISIBLE);
                    bGroupLeave.setVisibility(VISIBLE);

                    mOverlayView.enableMenu();
                } catch (Exception e) {
                    Log.e(TAG, "streamResume", e);
                }
            }
        });
        addView(bStreamResume);

        bStreamClose = new Button(getContext());
        bStreamClose.setText(R.string.btn_stream_close);
        bStreamClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mOverlayView.mService.mMapperChannel.streamClose();

                    bStreamInit.setVisibility(VISIBLE);
                    bStreamPause.setVisibility(GONE);
                    bStreamResume.setVisibility(GONE);
                    bStreamClose.setVisibility(GONE);

                    bGroupMatch.setVisibility(VISIBLE);
                    bGroupLeave.setVisibility(GONE);

                    mOverlayView.enableMenu();
                } catch (Exception e) {
                    Log.e(TAG, "streamClose", e);
                }
            }
        });
        addView(bStreamClose);

        bGroupMatch = new Button(getContext());
        bGroupMatch.setText(R.string.btn_group_match);
        bGroupMatch.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hide();
                mOverlayView.loading();
                mOverlayView.mService.runConcurrentThread(new Runnable() {
                    @Override
                    public void run() {
                        try {

                            if (System.currentTimeMillis() - mLastLookup > LOOKUP_INTERVAL) {

                                try {
                                    mOverlayView.mService.mMapperChannel.groupMatch(5);
                                    mLastLookup = System.currentTimeMillis();
                                } catch (Exception e) {
                                    Log.e(TAG, "groupMatch", e);
                                }
                            }

                            JSONArray matchedGroups = mOverlayView.mService.mMatchedGroups;

                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setTitle(R.string.alert_matched_groups);

                            ListView modeList = new ListView(getContext());

//                            String[] stringArray = new String[matchedGroups.length()];
//                            for (int i = 0; i < matchedGroups.length(); i++) {
//                                JSONObject group = matchedGroups.getJSONObject(i);
//                                String hashtags = group.getString("hashtags");
//                                String numMembers = group.getString("num_members");
//                                stringArray[i] = String.format("%s (%s)", hashtags, numMembers);
//                            }

                            String[] stringArray = new String[]{"group 1", "group 2", "group 3", "group 4", "group 5"};

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
                                            // group_init OR group_join
                                            Log.i(TAG, String.format("list index is %d", position));
                                            mLookupDialog.dismiss();
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
                        } catch (Exception e) {
                            Log.e(TAG, "groupMatch", e);
                        }

                        mOverlayView.mService.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mOverlayView.loaded();
                                show();
                            }
                        });
                    }
                });
            }
        });
        addView(bGroupMatch);

        bGroupLeave = new Button(getContext());
        bGroupLeave.setText(R.string.btn_group_leave);
        bGroupLeave.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {

                } catch (Exception e) {
                    Log.e(TAG, "groupLeave", e);
                }
            }
        });
        addView(bGroupLeave);

        Log.i(TAG, mOverlayView.mService.mStream.toString());

        if (mOverlayView.mService.mStream.has("stream_id")) {
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
    }

    public void setFamily(MinimapView minimapView, ChatView chatView, PartyView partyView) {
        vMinimap = minimapView;
        vChat = chatView;
        vParty = partyView;
    }

    public void show() {
        mOverlayView.mContentLayout.removeAllViews();
        mOverlayView.mContentLayout.addView(this);
    }

    public void hide() {
        mOverlayView.mContentLayout.removeAllViews();
    }
}
