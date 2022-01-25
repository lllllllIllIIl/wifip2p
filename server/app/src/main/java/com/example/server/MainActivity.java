package com.example.server;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Collection;
import java.util.Enumeration;

public class MainActivity extends AppCompatActivity {
    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    BroadcastReceiver receiver;
    IntentFilter intentFilter ;
    TextView groupState;
    TextView wifiState;
    InetAddress localIP;
    InetAddress remoteIP;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 100) {
                wifiState.setText(remoteIP.getHostAddress());
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        groupState = findViewById(R.id.groupState);
        wifiState = findViewById(R.id.wifiState);
        wifiState.setText("unknown");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("server","no permission");
            return;
        }

        manager.createGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                groupState.setText("成为新群主");
            }

            @Override
            public void onFailure(int i) {
                manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
                    @Override
                    public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {
                        if (wifiP2pGroup!= null){
                            if(wifiP2pGroup.isGroupOwner()){
                                groupState.setText("已是群主");

                            }else{
                                groupState.setText("成为群主失败1");
                            }
                        }
                        else{
                            groupState.setText("成为群主失败2");
                        }
                    }
                });
                Log.e("server","" + i);
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket socket = new ServerSocket();
                    socket.setReuseAddress(true);
                    socket.bind(new InetSocketAddress(13200));
                    Log.e("server","start recv");
                    Socket client = socket.accept();
                    InetSocketAddress clientAddress = (InetSocketAddress) client.getRemoteSocketAddress();
                    remoteIP = clientAddress.getAddress();
                    mHandler.sendEmptyMessage(100);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        manager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {
            @Override
            public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
                if(wifiP2pInfo!=null)
                {
                    localIP = wifiP2pInfo.groupOwnerAddress;
                    TextView view = findViewById(R.id.wifiState);
                }
            }
        });

        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.e("client","discover peers success");
            }

            @Override
            public void onFailure(int i) {

            }
        });
    }
}