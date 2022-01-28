/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wifidirect;



import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.sip.SipSession;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.ChannelListener;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import androidmads.library.qrgenearator.QRGContents;
import androidmads.library.qrgenearator.QRGEncoder;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

/**
 * An activity that uses WiFi Direct APIs to discover and connect with available
 * devices. WiFi Direct APIs are asynchronous and rely on callback mechanism
 * using interfaces to notify the application of operation success or failure.
 * The application should also register a BroadcastReceiver for notification of
 * WiFi state related events.
 */
public class WiFiDirectActivity<QRGEncoder> extends Activity implements ChannelListener, DeviceListFragment.DeviceActionListener {

    public static final String TAG = "wifidirectdemo";
    private Channel channel;
    private static final int PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 1001;
    String ip;
    private WifiP2pManager manager;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;
    public static String code;
    ServerSocket serverSocket;
    Thread Thread1 = null;
    private final IntentFilter intentFilter = new IntentFilter();
    public  String SERVER_IP = "";
    private BroadcastReceiver receiver = null;
    public static final String EXTRAS_FILE_PATH = "file_url";
    private ImageView qrCode;
    Button btnConnect ;
    private EditText dataEdt;
    public Button scanbtn;
    public static Bitmap bitmap;
    public  final int SERVER_PORT = 8080;
    androidmads.library.qrgenearator.QRGEncoder qrgEncoder;
    public String clientip = "";
    public static  final String EXTRA_YOUR_KEY="shd";
    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Fine location permission is not granted!");
                    finish();
                }
                break;
        }
    }

    private boolean initP2p() {
        // Device capability definition check
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            Log.e(TAG, "Wi-Fi Direct is not supported by this device.");
            return false;
        }

        // Hardware capability check
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            Log.e(TAG, "Cannot get Wi-Fi system service.");
            return false;
        }

        if (!wifiManager.isP2pSupported()) {
            Log.e(TAG, "Wi-Fi Direct is not supported by the hardware or Wi-Fi is off.");
            return false;
        }

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        if (manager == null) {
            Log.e(TAG, "Cannot get Wi-Fi Direct system service.");
            return false;
        }

        channel = manager.initialize(this, getMainLooper(), null);
        if (channel == null) {
            Log.e(TAG, "Cannot initialize Wi-Fi Direct.");
            return false;
        }

        return true;
    }
    private Button wifiEnable,wifiDiscover;
    private TextView name;
    private Button edit;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
       qrCode= findViewById(R.id.idIVQrcode);




//        dataEdt = findViewById(R.id.idEdt);
//        generateQrBtn = findViewById(R.id.idBtnGenerateQR);

        // add necessary intent values to be matched.

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        if (!initP2p()) {
            finish();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    WiFiDirectActivity.PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION);
            // After this point you wait for callback in
            // onRequestPermissionsResult(int, String[], int[]) overridden method
        }
//        if (manager != null && channel != null) {
//
//            // Since this is the system wireless settings activity, it's
//            // not going to send us a result. We will be notified by
//            // WiFiDeviceBroadcastReceiver instead.
//
//            startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
//        } else {
//            Log.e(TAG, "channel or manager is null");
//        }
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifi.setWifiEnabled(true);
        scanbtn = (Button) findViewById(R.id.atn_direct_enable);
        btnConnect =(Button) findViewById(R.id.atn_direct_enabl);
        edit = (Button) findViewById(R.id.idEdt);
        btnConnect.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
//                Client b = new Client();
//                b.execute(edit.getText().toString());
                Intent intent = new Intent(WiFiDirectActivity.this,Server.class);
                startActivity(intent);
            }
        });
        edit.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
//
                Intent intent = new Intent(WiFiDirectActivity.this,Clientt.class);
                startActivity(intent);
            }
        });

        scanbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(WiFiDirectActivity.this,QRCodeScanner.class);
                intent.putExtra("IP",clientip);
                startActivity(intent);
                ConnectivityManager connManager = (ConnectivityManager)    getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
//                name.setText("" + networkInfo.getDetailedState());
//                Thread client = new Thread(new MyServer());
//                client.start();



            }

        });



        wifiDiscover= (Button) findViewById(R.id.atn_direct_discover);
        name = (TextView) findViewById(R.id.plain_text_input);
        wifiDiscover.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View view) {
                if (!isWifiP2pEnabled) {
                    Toast.makeText(WiFiDirectActivity.this, R.string.p2p_off_warning,
                            Toast.LENGTH_SHORT).show();


                }


//                Thread myThread = new Thread(new MyServer());
//                myThread.start();

                final DeviceListFragment fragment = (DeviceListFragment) getFragmentManager()
                        .findFragmentById(R.id.frag_list);
                fragment.onInitiateDiscovery();

//                manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
//
//                    @Override
//                    public void onSuccess() {
//                        Toast.makeText(WiFiDirectActivity.this, "Discovery Initiated",
//                                Toast.LENGTH_SHORT).show();
//                    }
//
//                    @Override
//                    public void onFailure(int reasonCode) {
//                        Toast.makeText(WiFiDirectActivity.this, "Discovery Failed : " + reasonCode,
//                                Toast.LENGTH_SHORT).show();
//                    }});



                manager.createGroup(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {

//                        // Device is ready to accept incoming connections from peers.
//                        Toast.makeText(WiFiDirectActivity.this, "Device is ready to accept incoming peers.",
//                                Toast.LENGTH_SHORT).show();

                        manager.requestGroupInfo(channel, new WifiP2pManager.GroupInfoListener() {
                            @Override
                            public void onGroupInfoAvailable(WifiP2pGroup wifiP2pGroup) {

                                String code = "Exception Detected .Please try again";
                               try{ String pass = wifiP2pGroup.getPassphrase();
                                String net = wifiP2pGroup.getNetworkName();


                                   WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                                    ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
//                                 code = "WIFI:S:"+net+";T:WEP;P:"+ pass +":H:false;;";
                                   code = net+ "_"+ pass+"_"+ ip;

                                name.setText( code);}
                               catch (Exception e){
                                   e.printStackTrace();

                                }


                                MultiFormatWriter m = new MultiFormatWriter();
                                try {
                                    BitMatrix bitmatrix =  m.encode(code, BarcodeFormat.QR_CODE,500,500);
                                    BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                                    Bitmap bitmap = barcodeEncoder.createBitmap(bitmatrix);
                                    qrCode.setImageBitmap(bitmap);
                                    ConnectivityManager connManager = (ConnectivityManager)    getSystemService(Context.CONNECTIVITY_SERVICE);
                                    NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
//                                    name.setText("" + networkInfo.getDetailedState());



                                }
                                catch(Exception e){
                                e.printStackTrace();
                                }


                            }
                        } );

                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(WiFiDirectActivity.this, "P2P group creation failed. Retry.",
                                Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });
    }


    /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    /**
     * Remove all peers and clear all fields. This is called on
     * BroadcastReceiver receiving a state change event.
     */
    public void resetData() {
        DeviceListFragment fragmentList = (DeviceListFragment) getFragmentManager()
                .findFragmentById(R.id.frag_list);
        DeviceDetailFragment fragmentDetails = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        if (fragmentList != null) {
            fragmentList.clearPeers();
        }
        if (fragmentDetails != null) {
            fragmentDetails.resetViews();
        }
    }
//    class MyServer implements Runnable{
//        ServerSocket ss;
//        Socket mysocket;
//        DataInputStream dls;
//        String message;
//
//        Handler handler = new Handler();
//
//        @Override
//        public void run(){
//            try{
//                ss = new ServerSocket(9700);
//                handler.post(new Runnable(){
//                    @Override
//                    public void run(){
//                        Toast.makeText(getApplicationContext(), "Waiting for client", Toast.LENGTH_SHORT).show();
//                    }
//                });
//                while(true){
//                    mysocket = ss.accept();
//                    dls = new DataInputStream(mysocket.getInputStream());
//                    message = dls.readUTF();
//
//                    if (message.equals("Open Camera")) {
//                        Toast.makeText(getApplicationContext(), "Opening Camera on other device", Toast.LENGTH_SHORT).show();
//                    }
//                    else if(message.equals("Open Gallery")){
//                        Toast.makeText(getApplicationContext(), "Opening Gallery on other device", Toast.LENGTH_SHORT).show();
//                    }
//
//                    handler.post(new Runnable(){
//                        @Override
//                        public void run(){
//                            Toast.makeText(getApplicationContext(), "message recieved from client: " + message, Toast.LENGTH_SHORT).show();
//                        }
//                    });
//
//                }
//
//            }catch(Exception e){
//                e.printStackTrace();
//            }
//        }
//    }
//    class BackgroundTask extends AsyncTask<String, Void, String> {
//        Socket s;
//        DataOutputStream dos;
//        String message;
//
//        @Override
//        protected String doInBackground(String... params){
////            ip = params[0];
//            clientip ="192.168.49.119";
//            message = params[0];
//
//            try{
//                s = new Socket(clientip,9700);
//                dos = new DataOutputStream(s.getOutputStream());
//                dos.writeUTF(message);
//
//                dos.close();
//                s.close();
//            }
//            catch (IOException e) {
//                e.printStackTrace();
//            }
//
//
//            return null;
//        }
//    }
//
//    class Client
//        extends AsyncTask<String, Void, String> {
//            Socket s;
//            DataOutputStream dos;
//            String message;
//
//
//            @Override
//            protected String doInBackground(String... param){
////            ip = params[0];
//                clientip ="192.168.49.119";
//                message = param[0];
//
//                try{
//                    s = new Socket(clientip,9700);
//                    dos = new DataOutputStream(s.getOutputStream());
//                    dos.writeUTF(message);
//
//                    dos.close();
//                    s.close();
//                }
//                catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//
//                return null;
//            }
//        }
//
//

  /*  @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_items, menu);
        return true;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
   /* @SuppressLint("MissingPermission")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.atn_direct_enable:
                if (manager != null && channel != null) {

                    // Since this is the system wireless settings activity, it's
                    // not going to send us a result. We will be notified by
                    // WiFiDeviceBroadcastReceiver instead.

                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                } else {
                    Log.e(TAG, "channel or manager is null");
                }
                return true;

            case R.id.atn_direct_discover:

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }*/

    @Override
    public void showDetails(WifiP2pDevice device) {
        DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.showDetails(device);

    }

    @Override
  public void connect(WifiP2pConfig config) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }


        manager.connect(channel, config, new ActionListener() {

            @Override
            public void onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(WiFiDirectActivity.this, "Connect failed. Retry.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void disconnect() {
        final DeviceDetailFragment fragment = (DeviceDetailFragment) getFragmentManager()
                .findFragmentById(R.id.frag_detail);
        fragment.resetViews();
        manager.removeGroup(channel, new ActionListener() {

            @Override
            public void onFailure(int reasonCode) {
                Log.d(TAG, "Disconnect failed. Reason :" + reasonCode);

            }

            @Override
            public void onSuccess() {
                fragment.getView().setVisibility(View.GONE);
            }

        });
    }

    @Override
    public void onChannelDisconnected() {
        // we will try once more
        if (manager != null && !retryChannel) {
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
            resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(this,
                    "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void cancelDisconnect() {


    }
    protected void onStop() {
        super.onStop();
//        try {
//            serverSocket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }




}
