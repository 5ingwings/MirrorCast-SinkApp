package cn.sihao.mirrorcast.wifidirect;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.*;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener;
import android.net.wifi.p2p.WifiP2pManager.GroupInfoListener;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.text.TextUtils;

import cn.sihao.mirrorcast.utils.ARPUtil;

import com.orhanobut.logger.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import cn.sihao.mirrorcast.OnMirrorListener;


public class WiFiDirectMgr {
    private static final String TAG = "MiraWiFiDirectMgr";

    private boolean mIsGroupFormed = false;
    private boolean isGroupOwner = false;
    private int mSourcePort = 7236;
    private String mSourceIp = "";
    private String mSourceMacAddr = "";

    public static boolean wifiP2PIsConnected = false;
    private OnMirrorListener mOnMirrorListener;
    private boolean mIsStart = false;

    public WiFiDirectMgr(OnMirrorListener listener) {
        mOnMirrorListener = listener;
    }

    private boolean mIsMiraCastMgrStop = false;
    private Context mContext;
    private WifiP2pManager mWifiP2pManager;
    private Channel mChannel;


    public void start(Context context) {
        if (mIsStart) {
            return;
        }
        mIsMiraCastMgrStop = false;
        this.mContext = context;
        final IntentFilter mIntentFilter = new IntentFilter();
        // Indicates a change in the Wi-Fi P2P status.
        //mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        // Indicates a change in the list of available peers.
        //mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        // Indicates the state of Wi-Fi P2P connectivity has changed.
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        context.registerReceiver(mBroadcastReceiver, mIntentFilter, null, null);

        mWifiP2pManager = (WifiP2pManager) context.getSystemService(context.WIFI_P2P_SERVICE);
        mChannel = mWifiP2pManager.initialize(context, context.getMainLooper(), null);

        /////////////////
        setEnableWFD(mWifiP2pManager, mChannel, true, new ActionListener() {
            @Override
            public void onSuccess() {
                Logger.t(TAG).d("Successfully enabled WFD.");
            }

            @Override
            public void onFailure(int reason) {
                Logger.t(TAG).e("Failed to enable WFD with reason " + reason + ".");
            }
        });

        setP2pDeviceName(mWifiP2pManager, mChannel, getNickName(), new ActionListener() {
            @Override
            public void onSuccess() {
                Logger.t(TAG).d("Successfully set P2pDeviceName:" + getNickName());
            }

            @Override
            public void onFailure(int reason) {
                Logger.t(TAG).e("Failed to set P2pDeviceName with reason " + reason + ".");
            }
        });
        /////////////////
        startDiscovery();
    }

    public void stop() {
        if (mIsStart) {
            Logger.t(TAG).d("stop miraCast discovery.");
            stopDiscovery();
            mWifiP2pManager.removeGroup(mChannel, null);
            mContext.unregisterReceiver(mBroadcastReceiver);
            mIsMiraCastMgrStop = true;
            mIsStart = false;

            if (wifiP2PIsConnected) {
                wifiP2PIsConnected = false;
                mIsGroupFormed = false;
                Logger.t(TAG).d("wifiP2P Disconnected");
                mOnMirrorListener.onSessionEnd();
            }
        }
    }

    public boolean getIsGroupFormed() {
        return mIsGroupFormed;
    }

    public String getSourceIp() {
        return mSourceIp;
    }

    public int getSourcePort() {
        return mSourcePort;
    }

    private void startDiscovery() {
        mWifiP2pManager.discoverPeers(mChannel, new ActionListener() {
            @Override
            public void onSuccess() {
                Logger.t(TAG).d("Successfully initialed peers discovery.");
                mIsStart = true;
            }

            @Override
            public void onFailure(int reason) {
                mIsStart = false;
                Logger.t(TAG).w("Failed to initial peers discovery.");
            }
        });
    }

    private void stopDiscovery() {
        mWifiP2pManager.stopPeerDiscovery(mChannel, new ActionListener() {
            @Override
            public void onSuccess() {
                Logger.t(TAG).d("onSuccess: Successfully stopped peers discovery.");
            }

            @Override
            public void onFailure(int reason) {
                Logger.t(TAG).w("Failed to stop peers discovery.");
                mIsStart = false;
            }
        });
        mIsGroupFormed = false;
    }

    private final PeerListListener mPeerListListener = new PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peers) {
            List<WifiP2pDevice> peerList = new ArrayList<>(peers.getDeviceList());
            Logger.t(TAG).d(peerList.size() + " device(s) found");
            for (WifiP2pDevice peer : peerList) {
                Logger.t(TAG).d(String.format("peer: %s", peer.deviceName));
            }
        }
    };


    private final GroupInfoListener mGroupInfoListener = new GroupInfoListener() {
        @Override
        public void onGroupInfoAvailable(WifiP2pGroup group) {
            if (group != null) {
                String groupInfoStr = group.toString();
                Logger.t(TAG).d("\n====== Group info: ======\n"
                        + groupInfoStr + "\n==================");
                setGroupInfo(groupInfoStr);
                mWifiP2pManager.requestConnectionInfo(mChannel, mInfoListener);
            }
        }
    };

    private final ConnectionInfoListener mInfoListener = new ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo info) {
            String connectInfoStr = info.toString();
            Logger.t(TAG).d("\n====== Connection info: ======\n" + connectInfoStr + "\n==================");
            setConnInfo(connectInfoStr);
        }
    };

    /**
     * set the source deviceAddress and WFD CtrlPort when the source in p2p
     * play a role as a group client
     *
     * @param groupInfo
     */
    private void setGroupInfo(String groupInfo) {
        if (!TextUtils.isEmpty(groupInfo)) {
            mSourceMacAddr = groupInfo.substring(
                    groupInfo.lastIndexOf("deviceAddress") + 15, groupInfo.lastIndexOf("primary type")).trim();
            Logger.t(TAG).d("mSourceMacAddr:" + mSourceMacAddr);
            String sourcePortStr = groupInfo.substring(
                    groupInfo.lastIndexOf("WFD CtrlPort") + 14, groupInfo.lastIndexOf("WFD MaxThroughput")).trim();
            if (!TextUtils.isEmpty(sourcePortStr)) {
                int tmp = Integer.parseInt(sourcePortStr);
                mSourcePort = (tmp == 0) ? 7236 : tmp;
                Logger.t(TAG).d("mSourcePort:" + mSourcePort);
            }
        }
    }

    /**
     * set the groupFormed  isGroupOwner  groupOwnerAddress info
     *
     * @param connectInfo
     */
    private void setConnInfo(String connectInfo) {
        if (!TextUtils.isEmpty(connectInfo)) {
            connectInfo = connectInfo.replace(":", "");
            String[] connectInfoArr = connectInfo.split(" ");
            for (int i = 0; i < connectInfoArr.length - 1; i += 2) {
                if ("groupFormed".equals(connectInfoArr[i])) {
                    mIsGroupFormed = "true".equals(connectInfoArr[i + 1]);
                } else if ("isGroupOwner".equals(connectInfoArr[i])) {
                    isGroupOwner = "true".equals(connectInfoArr[i + 1]);
                } else if ("groupOwnerAddress".equals(connectInfoArr[i])) {
                    if (isGroupOwner) {
                        String sourceIpStr;
                        while (mIsGroupFormed && !mIsMiraCastMgrStop) {
                            sourceIpStr = ARPUtil.getIPFromMac(mSourceMacAddr);
                            if (!TextUtils.isEmpty(sourceIpStr)) {
                                mSourceIp = sourceIpStr;
                                Logger.t(TAG).d("ARPUtil.getIPFromMac ret:" + mSourceIp);
                                break;
                            } else {
                                Logger.t(TAG).d("ARPUtil.getIPFromMac ret is null");
                            }
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        mSourceIp = connectInfoArr[i + 1].substring(1);
                    }
                }
            }
            Logger.t(TAG).d("setConnInfo mIsGroupFormed:" + mIsGroupFormed
                    + ", isGroupOwner:" + isGroupOwner
                    + ", SourceIp:" + mSourceIp);
        }
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //fixme why thread in main
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                // Determine if Wifi P2P mode is enabled or not, alert
                // the Activity.
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    Logger.t(TAG).d("onReceive: WIFI_P2P_STATE_ENABLED is true");
                } else {
                    Logger.t(TAG).d("onReceive: WIFI_P2P_STATE_ENABLED is false");
                }
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

                // The peer list has changed! We should probably do something about
                // that.
                // fixme why would not get broadcast?
                Logger.t(TAG).d("onReceive: WIFI_P2P_PEERS_CHANGED_ACTION");
                if (null != mWifiP2pManager) {
                    mWifiP2pManager.requestPeers(mChannel, mPeerListListener);
                }
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                // Connection state changed! We should probably do something about that.
                Logger.t(TAG).d("onReceive: WIFI_P2P_CONNECTION_CHANGED_ACTION");
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                if (networkInfo.isConnected() && !wifiP2PIsConnected) {
                    wifiP2PIsConnected = true;
                    Logger.t(TAG).d("wifiP2P Connected");
                    mWifiP2pManager.requestGroupInfo(mChannel, mGroupInfoListener);
                    mOnMirrorListener.onSessionBegin();
                } else if (wifiP2PIsConnected) {
                    wifiP2PIsConnected = false;
                    mIsGroupFormed = false;
                    mIsStart = false;
                    Logger.t(TAG).d("wifiP2P Disconnected");
                    mOnMirrorListener.onSessionEnd();
                }
            }
        }
    };


    @SuppressLint("PrivateApi")
    private void setEnableWFD(WifiP2pManager wifiP2pManager, Channel channel, boolean enable, ActionListener listener) {
        try {
            //WifiP2pWfdInfo wifiP2pWfdInfo = new WifiP2pWfdInfo();
            Class clsWifiP2pWfdInfo = Class.forName("android.net.wifi.p2p.WifiP2pWfdInfo");
            Constructor ctorWifiP2pWfdInfo = clsWifiP2pWfdInfo.getConstructor();
            Object wifiP2pWfdInfo = ctorWifiP2pWfdInfo.newInstance();

            //wifiP2pWfdInfo.setWfdEnabled(true);
            Method mtdSetWfdEnabled = clsWifiP2pWfdInfo.getMethod("setWfdEnabled", boolean.class);
            mtdSetWfdEnabled.invoke(wifiP2pWfdInfo, enable);

            //wifiP2pWfdInfo.setDeviceType(WifiP2pWfdInfo.PRIMARY_SINK);
            Method mtdSetDeviceTypes = clsWifiP2pWfdInfo.getMethod("setDeviceType", int.class);
            mtdSetDeviceTypes.invoke(wifiP2pWfdInfo, 1);

            //wifiP2pWfdInfo.setSessionAvailable(true);
            Method mtdSetSessionAvailable = clsWifiP2pWfdInfo.getMethod("setSessionAvailable", boolean.class);
            mtdSetSessionAvailable.invoke(wifiP2pWfdInfo, enable);

            //wifiP2pWfdInfo.setMaxThroughput(MAX_THROUGHPUT);
            Method mtdSetMaxThroughput = clsWifiP2pWfdInfo.getMethod("setMaxThroughput", int.class);
            mtdSetMaxThroughput.invoke(wifiP2pWfdInfo, 50);

            if (listener != null) {
                Class clsWifiP2pManager = Class.forName("android.net.wifi.p2p.WifiP2pManager");
                Method methodSetWFDInfo = clsWifiP2pManager.getMethod("setWFDInfo",
                        Channel.class, clsWifiP2pWfdInfo, ActionListener.class);
                methodSetWFDInfo.invoke(wifiP2pManager, channel, wifiP2pWfdInfo, listener);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.t(TAG).e("Failed to setEnableWFD: " + e.getLocalizedMessage());
        }
    }

    private void setP2pDeviceName(WifiP2pManager wifiP2pManager, Channel channel, String deviceName, ActionListener listener) {
        try {
            Method m = wifiP2pManager.getClass().getMethod(
                    "setDeviceName", Channel.class, String.class, ActionListener.class);
            m.invoke(wifiP2pManager, channel, deviceName, listener);
        } catch (Exception e) {
            Logger.t(TAG).e("Failed to setP2pDeviceName:" + e.getLocalizedMessage());
        }
    }

    private String getNickName() {
        return "max投屏设备";
    }

}
