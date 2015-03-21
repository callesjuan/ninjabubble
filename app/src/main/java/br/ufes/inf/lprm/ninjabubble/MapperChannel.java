package br.ufes.inf.lprm.ninjabubble;

import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Juan on 10/02/2015.
 */
public class MapperChannel implements ChatMessageListener {

    private String TAG = "NinjaBubbleMagic/MapperListener";

    public NinjaBubbleMagic mService;
    public Chat mChat;

    public Object mLocker = new Object();
    public boolean mWaitingReply = false;
    public boolean mReplyError = false;
    public long mTimeout = 1000 * 30;

    final String DATE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    final String STAMP_PATTERN = "yyyyMMddHHmmss";

    public MapperChannel(NinjaBubbleMagic service) {
        mService = service;
    }

    public void connect() throws Exception {
        try {
            // Connect to XMPP server

//            AndroidSmackInitializer androidSmackInitializer = new AndroidSmackInitializer();
//            androidSmackInitializer.initialize();
//            TCPInitializer tcpInitializer = new TCPInitializer();
//            tcpInitializer.initialize();
//            ExtensionsInitializer extensionsInitializer= new ExtensionsInitializer();
//            extensionsInitializer.initialize();

            XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
            configBuilder.setUsernameAndPassword(mService.mNick, mService.mPWD);
            configBuilder.setServiceName(mService.mDomain);
            configBuilder.setResource("device");
            configBuilder.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);

            mService.mXmppConnection = new XMPPTCPConnection(configBuilder.build());

            ReconnectionManager.getInstanceFor(mService.mXmppConnection).enableAutomaticReconnection();
            mService.mXmppConnection.addConnectionListener(new MyConnectionListener());

            mService.mXmppConnection.connect();
            mService.mXmppConnection.login();
            Log.i(TAG, "logged into XMPP server");

            Log.i(TAG, String.format("reconnection enabled: %s", ReconnectionManager.getInstanceFor(mService.mXmppConnection).isAutomaticReconnectEnabled()));

            ChatManager chatManager = ChatManager.getInstanceFor(mService.mXmppConnection);
            mChat = chatManager.createChat(mService.mMapperJID, this);

        } catch (Exception e) {
            throw e;
        }
    }

    public void disconnect() {
        try {
            mService.mXmppConnection.disconnect();
        } catch (Exception e) {
        }
    }

    public void lock() throws Exception {
        synchronized (mLocker) {
            mWaitingReply = true;
            try {
                mLocker.wait(mTimeout);
                if (mWaitingReply) {
                    mWaitingReply = false;
                    throw new Exception("message timed out");
                }
                if (mReplyError) {
                    mReplyError = false;
                    throw new Exception("trouble handling reply");
                }
            }
            catch (Exception e) {
                throw e;
            }
        }
    }

    private void unlock() {
        synchronized (mLocker) {
            mWaitingReply = false;
            mLocker.notify();
        }
    }

    private void unlock(boolean replyError) {
        if (replyError) {
            mReplyError = true;
        }
        unlock();
    }

    @Override
    public void processMessage(Chat chat, Message message) {
        try {
            JSONObject parsed = new JSONObject(message.getBody());
            JSONObject args = parsed.getJSONObject("args");
            args.put("from", message.getFrom());
            args.put("to", message.getTo());
            if (parsed.getString("func").equals("message_exception_reply")) {
                messageExceptionReply(args);
            }
            else if (parsed.getString("func").equals("stream_status_reply")) {
                streamStatusReply(args);
            }
            else if (parsed.getString("func").equals("stream_init_reply")) {
                streamInitReply(args);
            }
            else if (parsed.getString("func").equals("stream_pause_reply")) {
                streamPauseReply(args);
            }
            else if (parsed.getString("func").equals("stream_resume_reply")) {
                streamResumeReply(args);
            }
            else if (parsed.getString("func").equals("stream_close_reply")) {
                streamCloseReply(args);
            }
            else if (parsed.getString("func").equals("group_match_reply")) {
                groupMatchReply(args);
            }
            else if (parsed.getString("func").equals("group_join_reply")) {
                groupJoinReply(args);
            }
            else if (parsed.getString("func").equals("group_leave_reply")) {
                groupLeaveReply(args);
            }
            else if (parsed.getString("func").equals("group_fetch_members_reply")) {
                groupFetchMembersReply(args);
            }
            else if (parsed.getString("func").equals("group_fetch_pings_reply")) {
                groupFetchPingsReply(args);
            }
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public void messageExceptionReply (JSONObject args) {
        Log.e(TAG, "messageExceptionReply");
        try {
            Log.e(TAG, args.get("exception").toString());
        } catch (Exception e) {}
        unlock(true);
    }

    /**
     * streamStatus
     */
    public void streamStatus() throws Exception {
        Log.i(TAG, "streamStatus");

        try {
            JSONObject msg = new JSONObject()
                .put("func", "stream_status")
                .put("args", new JSONObject());

            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());

            lock();
        }
        catch (Exception e) {
            Log.e(TAG, "streamStatus", e);
            throw e;
        }
    }

    public void streamStatusReply(JSONObject args) {
        if (!mWaitingReply) {
            return;
        }
        try {

            Log.i(TAG, "streamStatusReply");
            Log.i(TAG, args.toString());
            mService.mSource = args.getJSONObject("source");
            if (args.has("stream")) {
                mService.mStream = args.getJSONObject("stream");
            }

            unlock();
        }
        catch (Exception e) {
            Log.e(TAG, "streamStatusReply", e);
            unlock(true);
        }
    }

    /**
     * streamInit
     */
    public void streamInit(String media, String groupJid)throws Exception {
        Log.i(TAG, "streamInit");

        try {
            Date now = new Date();
            SimpleDateFormat stampFormat = new SimpleDateFormat(STAMP_PATTERN);

            String streamId = mService.mNick + "__" + stampFormat.format(now);
            if (groupJid == null) {
                groupJid = streamId + "__" + stampFormat.format(now);
            }
            String stamp = stampFormat.format(now);

            JSONObject msg = new JSONObject()
                .put("func", "stream_init")
                .put("args", new JSONObject()
                    .put("stream_id", streamId)
                    .put("group_jid", groupJid)
                    .put("hashtags", mService.mHashtags)
                    .put("latlng", mService.mLatlng)
                    .put("media", mService.mMedia)
                    .put("stamp", stamp)
                )
            ;

            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());

            lock();
        }
        catch (Exception e) {
            Log.e(TAG, "streamInit", e);
            throw e;
        }
    }

    public void streamInitReply(JSONObject args) {
        if (!mWaitingReply) {
            return;
        }
        try {
            mService.mStream = args.getJSONObject("stream");

            if (args.has("members")) {
                mService.mParty = args.getJSONArray("members");
            }

            unlock();
        }
        catch (Exception e) {
            Log.e(TAG, "streamInitReply", e);
            unlock(true);
        }
    }

    /**
     * streamPause
     */
    public void streamPause() throws Exception {
        Log.i(TAG, "streamPause");

        try {
            JSONObject msg = new JSONObject()
                .put("func", "stream_pause")
                .put("args", new JSONObject()
                    .put("stream_id", mService.mStream.getString("stream_id"))
                )
            ;

            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());

            lock();
        }
        catch (Exception e) {
            Log.e(TAG, "streamPause", e);
            throw e;
        }
    }

    public void streamPauseReply(JSONObject args) {
        if (!mWaitingReply) {
            return;
        }
        try {
            mService.mStream = args.getJSONObject("stream");
            unlock();
        }
        catch (Exception e) {
            Log.e(TAG, "streamPauseReply", e);
            unlock(true);
        }
    }

    /**
     * streamResume
     */
    public void streamResume() throws Exception {
        Log.i(TAG, "streamResume");

        try {
            JSONObject msg = new JSONObject()
                .put("func", "stream_resume")
                .put("args", new JSONObject()
                    .put("stream_id", mService.mStream.getString("stream_id"))
                )
            ;

            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());

            lock();
        }
        catch (Exception e) {
            Log.e(TAG, "streamResume", e);
            throw e;
        }
    }

    public void streamResumeReply(JSONObject args) {
        Log.i(TAG, "streamResumeReply:"+args.toString());
        if (!mWaitingReply) {
            return;
        }
        try {
            mService.mStream = args.getJSONObject("stream");

            if (args.has("members")) {
                mService.mParty = args.getJSONArray("members");
            }

            unlock();
        }
        catch (Exception e) {
            Log.e(TAG, "streamResumeReply", e);
            unlock(true);
        }
    }

    /**
     * streamClose
     */
    public void streamClose () throws Exception{
        Log.i(TAG, "streamClose");

        try {
            JSONObject msg = new JSONObject()
                .put("func", "stream_close")
                .put("args", new JSONObject()
                    .put("stream_id", mService.mStream.getString("stream_id"))
                )
            ;

            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());

            lock();
        }
        catch (Exception e) {
            Log.e(TAG, "streamClose", e);
            throw e;
        }
    }

    public void streamCloseReply(JSONObject args) {
        if (!mWaitingReply) {
            return;
        }
        try {
            if (args.has("stream")) {
                mService.mStream = null;
                unlock();
            }
            else {
                throw new Exception();
            }
        }
        catch (Exception e) {
            Log.e(TAG, "streamCloseReply", e);
            unlock(true);
        }
    }

    /**
     * updateTwitcastingId
     */
    public void updateTwitcastingId(String twitcastingId) throws Exception {
        Log.i(TAG, "updateTwitcastingId");

        try {

            JSONObject msg = new JSONObject()
                .put("func", "update_twitcasting_id")
                .put("args", new JSONObject()
                    .put("twitcasting_id", twitcastingId)
                );

            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());
        }
        catch (Exception e) {
            Log.e(TAG, "updateTwitcastingId", e);
            throw e;
        }
    }

    /**
     * updateLatlng
     */
    public void updateLatlng(JSONArray latlng) throws Exception {
        Log.i(TAG, "updateLatlng");

        try {

            JSONObject msg = new JSONObject()
                .put("func", "update_latlng")
                .put("args", new JSONObject()
                    .put("stream_id", mService.mStream.getString("stream_id"))
                    .put("latlng", latlng)
                );

            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());
        }
        catch (Exception e) {
            Log.e(TAG, "updateLatlng", e);
            throw e;
        }
    }

    /**
     * updateHashtags
     */
    public void updateHashtags(String hashtags) throws Exception {
        Log.i(TAG, "updateHashtags");

        try {

            JSONObject msg = new JSONObject()
                    .put("func", "update_hashtags")
                    .put("args", new JSONObject()
                                    .put("stream_id", mService.mStream.getString("stream_id"))
                                    .put("hashtags", hashtags)
                    );

            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());
        }
        catch (Exception e) {
            Log.e(TAG, "updateHashtags", e);
            throw e;
        }
    }

    /**
     * updateDeviceStatus
     */
    public void updateDeviceStatus(String status) throws Exception {
        Log.i(TAG, "updateDeviceStatus");

        try {

            JSONObject msg = new JSONObject()
                    .put("func", "update_device_status")
                    .put("args", new JSONObject()
                                    .put("stream_id", mService.mStream.getString("stream_id"))
                                    .put("device_status", status)
                    );

            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());
        }
        catch (Exception e) {
            Log.e(TAG, "updateDeviceStatus", e);
            throw e;
        }
    }

    /**
     * updateGeneralStatus
     */
    public void updateGeneralStatus(String status) throws Exception {
        Log.i(TAG, "updateGeneralStatus");

        try {

            JSONObject msg = new JSONObject()
                    .put("func", "update_general_status")
                    .put("args", new JSONObject()
                                    .put("stream_id", mService.mStream.getString("stream_id"))
                                    .put("general_status", status)
                    );

            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());
        }
        catch (Exception e) {
            Log.e(TAG, "updateGeneralStatus", e);
            throw e;
        }
    }

    /**
     * groupMatch
     */
    public void groupMatch (double radius, String groupJid) throws Exception {
        Log.i(TAG, "groupMatch");

        try {
            JSONObject msg;

            if (groupJid != null) {
                msg = new JSONObject()
                    .put("func", "group_match")
                    .put("args", new JSONObject()
                        .put("hashtags", mService.mHashtags)
                        .put("latlng", mService.mLatlng)
                        .put("radius", radius)
                        .put("group_jid", groupJid)
                    );
            } else {
                msg = new JSONObject()
                    .put("func", "group_match")
                    .put("args", new JSONObject()
                        .put("hashtags", mService.mHashtags)
                        .put("latlng", mService.mLatlng)
                        .put("radius", radius)
                );
            }


            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());

            lock();
        } catch (Exception e) {
            Log.e(TAG, "groupMatch", e);
            throw e;
        }
    }

    public void groupMatchReply(JSONObject args) {
        if (!mWaitingReply) {
            return;
        }
        try {
            mService.mMatchedGroups = args.getJSONArray("matched_groups");
            unlock();
        }
        catch (Exception e) {
            Log.e(TAG, "groupMatchReply", e);
            unlock(true);
        }
    }

    /**
     * groupJoin
     */
    public void groupJoin (String groupJid) throws Exception {
        Log.i(TAG, "groupJoin");

        try {
            JSONObject msg = new JSONObject()
                .put("func", "group_join")
                .put("args", new JSONObject()
                    .put("stream_id", mService.mStream.getString("stream_id"))
                    .put("group_jid", groupJid)
                );

            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());

            lock();
        } catch (Exception e) {
            Log.e(TAG, "groupJoin", e);
            throw e;
        }
    }

    public void groupJoinReply(JSONObject args) {
        if (!mWaitingReply) {
            return;
        }
        try {
            mService.mStream = args.getJSONObject("stream");
            mService.mParty = args.getJSONArray("members");
            unlock();
        }
        catch (Exception e) {
            Log.e(TAG, "groupJoinReply", e);
            unlock(true);
        }
    }

    /**
     * groupLeave
     */
    public void groupLeave () throws Exception {
        Log.i(TAG, "groupLeave");

        try {
            Date now = new Date();
            SimpleDateFormat stampFormat = new SimpleDateFormat(STAMP_PATTERN);

            String groupJid = mService.mStream.getString("stream_id") + "__" + stampFormat.format(now);

            JSONObject msg = new JSONObject()
                .put("func", "group_leave")
                .put("args", new JSONObject()
                                .put("stream_id", mService.mStream.getString("stream_id"))
                                .put("group_jid", groupJid)
                );

            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());

            lock();
        } catch (Exception e) {
            Log.e(TAG, "groupLeave", e);
            throw e;
        }
    }

    public void groupLeaveReply(JSONObject args) {
        if (!mWaitingReply) {
            return;
        }
        try {
            mService.mStream = args.getJSONObject("stream");
            mService.mParty = null;
            unlock();
        }
        catch (Exception e) {
            Log.e(TAG, "groupLeaveReply", e);
            unlock(true);
        }
    }

    /**
     * groupFetchMembers
     */
    public void groupFetchMembers () throws Exception {
        Log.i(TAG, "groupFetchMembers");

        try {
            JSONObject msg = new JSONObject()
                .put("func", "group_fetch_members")
                .put("args", new JSONObject()
                                .put("stream_id", mService.mStream.getString("stream_id"))
                                .put("group_jid", mService.mStream.getString("group_jid"))
                );

            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());

            lock();
        } catch (Exception e) {
            Log.e(TAG, "groupFetchMembers", e);
            throw e;
        }
    }

    public void groupFetchMembersReply(JSONObject args) {
        if (!mWaitingReply) {
            return;
        }
        try {
            mService.mParty = args.getJSONArray("members");
            unlock();
        }
        catch (Exception e) {
            Log.e(TAG, "groupFetchMembersReply", e);
            unlock(true);
        }
    }

    /**
     * groupFetchPings
     */
    public void groupFetchPings (long since) throws Exception {
        Log.i(TAG, "groupFetchPings");

        try {
            Date sinceDate = new Date(System.currentTimeMillis() - since);
            SimpleDateFormat stampFormat = new SimpleDateFormat(STAMP_PATTERN);
            String stamp = stampFormat.format(sinceDate);

            JSONObject msg = new JSONObject()
                .put("func", "group_fetch_pings")
                .put("args", new JSONObject()
                    .put("group_jid", mService.mStream.getString("group_jid"))
                    .put("since", stamp)
                );

            Log.i(TAG, msg.toString());
            mChat.sendMessage(msg.toString());

            lock();
        } catch (Exception e) {
            Log.e(TAG, "groupFetchPings", e);
            throw e;
        }
    }

    public void groupFetchPingsReply(JSONObject args) {
        Log.i(TAG, "groupFetchPingsReply:"+args.toString());
        if (!mWaitingReply) {
            return;
        }
        try {
            mService.mPings = args.getJSONArray("pings");
            Log.i(TAG, mService.mPings.toString());
            unlock();
        }
        catch (Exception e) {
            Log.e(TAG, "groupFetchPings", e);
            unlock(true);
        }
    }

    public class MyConnectionListener implements ConnectionListener {

        @Override
        public void connected(XMPPConnection connection) {
        }

        @Override
        public void authenticated(XMPPConnection connection, boolean resumed) {
        }

        @Override
        public void connectionClosed() {
        }

        @Override
        public void connectionClosedOnError(Exception e) {
            Log.e(TAG, "connectionClosedOnError");

            String status;
            try {
                status = mService.mStream.getString("status");
            } catch (Exception f) {
                return;
            }

            if (status.equals("streaming")) {
                mService.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mService.mOverlayView.vHome.showLoading();

                        try {
                            mService.mStream.put("status", "paused");
                        } catch (Exception e) {
                            mService.mOverlayView.vHome.showLoaded();
                            return;
                        }

                        mService.stopLocationListener();
                        mService.stopOrientationListener();

                        mService.mOverlayView.vHome.bStreamPause.setVisibility(View.GONE);
                        mService.mOverlayView.vHome.bStreamResume.setVisibility(View.VISIBLE);

                        mService.mOverlayView.vHome.bGroupMatch.setVisibility(View.GONE);
                        mService.mOverlayView.vHome.bGroupLeave.setVisibility(View.GONE);

                        mService.mOverlayView.vHome.mOverlayView.imHome.setImageBitmap(mService.mOverlayView.mBmpOff);

                        Toast.makeText(mService, R.string.error_connection, Toast.LENGTH_SHORT);

                        mService.mOverlayView.disableMenu();
                        mService.mOverlayView.vHome.show();
                    }
                });
            }
        }

        @Override
        public void reconnectionSuccessful() {
            Log.i(TAG, "reconnectionSuccessful");
            String status;
            try {
                status = mService.mStream.getString("status");
                Log.i(TAG, String.format("stream_status = %s", status));
            } catch (Exception f) {
                return;
            }

            if (status.equals("paused")) {
                mService.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mService.mOverlayView.vHome.showLoading();
                    }
                });
                try {
                    streamStatus();

                    mService.startLocationListener();
                    mService.startOrientationListener();

                    streamResume();

                    try {
                        mService.mPartyChannel.join(mService.mStream.getString("group_jid"));
                    } catch (Exception e) {
                        Log.e(TAG, "streamResume", e);
                        Toast.makeText(mService, R.string.error_groupjoin, Toast.LENGTH_SHORT).show();
                    }

                    mService.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mService.mOverlayView.vHome.bStreamPause.setVisibility(View.VISIBLE);
                            mService.mOverlayView.vHome.bStreamResume.setVisibility(View.GONE);

                            mService.mOverlayView.vHome.bGroupMatch.setVisibility(View.VISIBLE);
                            mService.mOverlayView.vHome.bGroupLeave.setVisibility(View.VISIBLE);

                            mService.mOverlayView.imHome.setImageBitmap(mService.mOverlayView.mBmpOn);

                            mService.mOverlayView.enableMenu();
                            mService.mOverlayView.vHome.showLoaded();
                        }
                    });
                } catch (Exception e) {
                    mService.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mService, R.string.error_reconnection, Toast.LENGTH_SHORT);
                            mService.mOverlayView.vHome.showLoaded();
                        }
                    });
                }
            }
        }

        @Override
        public void reconnectingIn(int seconds) {
        }

        @Override
        public void reconnectionFailed(Exception e) {
        }
    }

    /**
     * IMAGE TIMER
     */

    public Timer mTimer;
    public final long TIMER_RATE = 1000 * 10;
    public Camera mCamera;

    public void startTimer() {
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
                && ((Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP))) {
            // your code here - is between15-21

            mCamera = Camera.open();
            try {
                Camera.Parameters parameters = mCamera.getParameters();

                List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
                Camera.Size lowestSize = null;
                for (Camera.Size size : sizes) {
                    if (lowestSize == null || (size.width <= lowestSize.width && size.height <= lowestSize.height)) {
                        lowestSize = size;
                    }
                }

                parameters.setPictureSize(lowestSize.width, lowestSize.height);

                mCamera.setPreviewDisplay(null);
                mCamera.startPreview();

                mTimer = new Timer();
                mTimer.scheduleAtFixedRate(new MyTimerTask(), TIMER_RATE, TIMER_RATE);

            } catch (Exception e) {}

        } else if(Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
            // your code here - is api 21
        }
    }

    public void stopTimer() {
        try {
            mTimer.cancel();
            mTimer = null;

            mCamera.stopPreview();
            mCamera.release();
        } catch (Exception e) {}
    }

    public class MyTimerTask extends TimerTask {

        @Override
        public void run() {
            mCamera.takePicture(shutterCallback, rawCallback, jpegCallback);
        }

        Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
            public void onShutter() {
                Log.d(TAG, "onShutter'd");
            }
        };

        Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                Log.d(TAG, "onPictureTaken - raw");
            }
        };

        Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera camera) {
                FileOutputStream outStream = null;
                try {
                    File outputDir = mService.getCacheDir(); // context being the Activity pointer
                    File outputFile = File.createTempFile("temp", "jpg", outputDir);

                    // write to local sand box file system
                    //outStream = CameraDemo.this.openFileOutput(String.format("%d.jpg", System.currentTimeMillis()), 0);
                    // Or write to s d card
                    // outStream = new FileOutputStream(String.format("/sdcard/%d.jpg", System.currentTimeMillis()));
                    outStream = new FileOutputStream(outputFile);
                    outStream.write(data);
                    outStream.close();
                    Log.d(TAG, "wrote bytes: " + data.length);

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {}
                Log.d(TAG, "onPictureTaken - jpeg");
            }
        };
    }
}
