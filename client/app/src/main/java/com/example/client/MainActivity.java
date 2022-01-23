package com.example.client;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private DeviceAdapter deviceAdapter;
    String TAG = "client";
    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    BroadcastReceiver receiver;
    IntentFilter intentFilter ;
    private List<WifiP2pDevice> wifiP2pDeviceList;
    WifiP2pInfo GOInfo;
    private WifiP2pDevice mWifiP2pDevice;

    InetAddress localIP;
    InetAddress remoteIP;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        RecyclerView rv_deviceList = findViewById(R.id.rv_deviceList);

        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        wifiP2pDeviceList = new ArrayList<>();
        deviceAdapter = new DeviceAdapter(wifiP2pDeviceList);
        deviceAdapter.setClickListener(position -> {
            mWifiP2pDevice = wifiP2pDeviceList.get(position);
            startWifiP2p();
        });

        rv_deviceList.setAdapter(deviceAdapter);
        rv_deviceList.setLayoutManager(new LinearLayoutManager(this));

        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.e("client","discover success");
                // remaining code
            }

            @Override
            public void onFailure(int reasonCode) {

            }
        });

    }

    public void onClick(View view){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(remoteIP,13100);
                    OutputStream out = socket.getOutputStream();
                    String msg = "syn";
                    out.write(msg.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    public void updateList(){
        manager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList wifiList) {
                wifiP2pDeviceList.clear();
                wifiP2pDeviceList.addAll(wifiList.getDeviceList());
                deviceAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }


    public void startWifiP2p(){
        WifiP2pConfig config = new WifiP2pConfig();
        if (config.deviceAddress != null && mWifiP2pDevice != null) {
            config.deviceAddress = mWifiP2pDevice.deviceAddress;
            config.wps.setup = WpsInfo.PBC;

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("等待对方确认");
            //builder.show();
            manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {

                    manager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {
                        @Override
                        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    TextView view = findViewById(R.id.rmtIPView);
                                    remoteIP = wifiP2pInfo.groupOwnerAddress;
                                    view.setText(remoteIP.getCanonicalHostName());
                                }
                            }).start();

                        }
                    });

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            TextView view = findViewById(R.id.locIPView);
                            try {
                                Enumeration nis = NetworkInterface.getNetworkInterfaces();
                                InetAddress ia = null;
                                while (nis.hasMoreElements()) {
                                    NetworkInterface ni = (NetworkInterface) nis.nextElement();
                                    Enumeration<InetAddress> ias = ni.getInetAddresses();
                                    while (ias.hasMoreElements()) {
                                        ia = ias.nextElement();
                                        if (ia instanceof Inet6Address) {
                                            continue;// skip ipv6
                                        }
                                        String ip = ia.getHostAddress();
                                        if (!"127.0.0.1".equals(ip)) {
                                            localIP = ia;
                                            view.setText(localIP.getHostAddress());
                                            break;
                                        }
                                    }
                                }
                            } catch (SocketException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }

                @Override
                public void onFailure(int reason) {
                }
            });
        }

    }
}