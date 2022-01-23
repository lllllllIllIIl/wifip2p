package com.example.server;


import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;

import java.util.Collection;

public interface DirectActionListener extends WifiP2pManager.ChannelListener {


    void onConnectionInfoAvailable(WifiP2pInfo info);

    void onPeersAvailable(Collection<WifiP2pDevice> deviceList);
}
