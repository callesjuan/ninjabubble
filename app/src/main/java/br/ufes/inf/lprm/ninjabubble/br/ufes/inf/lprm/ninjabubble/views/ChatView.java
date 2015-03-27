package br.ufes.inf.lprm.ninjabubble.br.ufes.inf.lprm.ninjabubble.views;

import android.content.Context;
import android.graphics.Color;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import java.util.List;

import br.ufes.inf.lprm.ninjabubble.R;

/**
 * Created by Juan on 11/02/2015.
 */
public class ChatView extends ContentView {
    public final String TAG = "NinjaBubbleMagic/ChatView";

    public ListView mListView;
    public ArrayAdapter<String> mAdapter;

    public LinearLayout mInputLayout;
    public EditText mInput;
    public Button mSend;

    public ChatView(Context context, OverlayView overlayView) {
        super(context, overlayView);

        mContentLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        mContentLayout.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    public void show() {
        super.show();

        if (super.mHasLoaded) {
            super.showLoaded();
            if(mListView.getCount() > 0)
                mListView.setSelection(mListView.getCount()-1);
            Log.i(TAG, "AAAAA:" + mOverlayView.vChat.mAdapter.toString());
            return;
        } else {
            super.mHasLoaded = true;
        }

        mOverlayView.mService.runConcurrentThread(new Runnable() {
            @Override
            public void run() {
                mAdapter = new ArrayAdapter<>(getContext(), R.layout.simple_list_item_1, android.R.id.text1);
                List<String> history = mOverlayView.mService.retrieveChatHistory();
                mAdapter.addAll(history);

                final int contentWidth = (int) (mOverlayView.mWidth);
                final int contentHeightList = (int) (mOverlayView.mHeight * 0.7);
                final int contentHeightInput = (int) (mOverlayView.mHeight * 0.3);

                mListView = new ListView(getContext());
                mListView.setAdapter(mAdapter);
                mListView.setLayoutParams(new LinearLayout.LayoutParams(contentWidth, contentHeightList));

                mInputLayout = new LinearLayout(getContext());
                mInputLayout.setGravity(Gravity.CENTER);
                mInputLayout.setLayoutParams(new LayoutParams(contentWidth, contentHeightInput));

                mInput = new EditText(getContext());
                mInput.setWidth((int) (contentWidth * 0.8));
                //mInput.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                mInput.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                mInput.setMinLines(3);
                mInput.setGravity(Gravity.TOP | Gravity.LEFT);
                mInput.setBackgroundColor(Color.WHITE);
                mInput.setTextColor(Color.BLACK);

                mInputLayout.addView(mInput);

                mSend = new Button(getContext());
                mSend.setText("send");
                mSend.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.i(TAG, "ChatView:Value:"+mInput.getText().toString());
                        Log.i(TAG, "ChatView:Input:"+mInput.toString());
                        Log.i(TAG, "ChatView:Context:"+v.getContext().toString());
                        if (mInput.getText().toString().length() > 0) {
                            mOverlayView.mService.mPartyChannel.chatMessageOut(mInput.getText().toString());
                            mInput.setText("");
                        }
                    }
                });
                mInputLayout.addView(mSend);

                mOverlayView.mService.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mContentLayout.addView(mListView);
                        mContentLayout.addView(mInputLayout);
                        ChatView.super.showLoaded();

                        if(mListView.getCount() > 0)
                            mListView.setSelection(mListView.getCount()-1);
                    }
                });


            }
        });
    }
}
