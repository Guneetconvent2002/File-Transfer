package com.example.android.wifidirect;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.zxing.Result;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class QRCodeScanner extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    private static final int REQUEST_CAMERA = 1;
    private BroadcastReceiver receiver = null;
    private WifiManager wifiManager ;
    private ConnectivityManager connectivityManager;
    private Socket socket;
    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private static final int SERVER_PORT = 9700;
    private static QRCodeScanner a;
    public  String SERVER_IP = "";
    public String clientip;

    private ZXingScannerView ScannerView;
    private static int cam = Camera.CameraInfo.CAMERA_FACING_BACK;
    private EditText edit;
    @Override
    protected void onCreate(Bundle savedInstancesState){
        super.onCreate(savedInstancesState);

        Intent intent= new Intent();
        clientip =intent.getStringExtra("IP");
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        ScannerView =new ZXingScannerView(this);
//        String thread1;
//        try {
//                SERVER_IP = getLocalIpAddress();
//            } catch (UnknownHostException e) {
//                e.printStackTrace();
//            }
//
//            Thread1 = new Thread(new Thread1());
//            Thread1.start();
        setContentView(ScannerView);
        int currentapiversion = Build.VERSION.SDK_INT;
        if(currentapiversion >= Build.VERSION_CODES.M){
            if(checkPermission()){
//                Toast.makeText(getApplicationContext(),"Permission Granted",Toast.LENGTH_LONG).show();
            }
        }
    }

//    private String getLocalIpAddress() throws UnknownHostException {
//        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
//        assert wifiManager != null;
//        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
//        int ipInt = wifiInfo.getIpAddress();
//        return InetAddress.getByAddress(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ipInt).array()).getHostAddress();
//    }
//    private PrintWriter output;
//    private BufferedReader input;
//
//    class Thread1 implements Runnable {
//                public void run() {
//            Socket socket;
//            try {
//                socket = new Socket(SERVER_IP, SERVER_PORT);
//                Toast.makeText(QRCodeScanner.this, "Client active", Toast.LENGTH_LONG).show();
//                output = new PrintWriter(socket.getOutputStream());
//                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
//                    }
//                });
//                new Thread(new Thread2()).start();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//    class Thread2 implements Runnable {
//        @Override
//        public void run() {
//            while (true) {
//                try {
//                    final String message = input.readLine();
//                    if (message != null) {
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                Toast.makeText(getApplicationContext(), "Client"+message, Toast.LENGTH_SHORT).show();
//                                edit.setText("connected");
//                            }
//                        });
//                    } else {
//                        Thread1 = new Thread(new Thread1());
//                        Thread1.start();
//                        return;
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//    class Thread3 implements Runnable {
//        private String message;
//        Thread3(String message) {
//            this.message = message;
//        }
//        @Override
//        public void run() {
//            output.write(message);
//            output.flush();
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                   Toast.makeText(getApplicationContext(),"Server"+message,Toast.LENGTH_SHORT).show();
//
//                }
//            });
//        }
//    }
    private boolean checkPermission(){
        return (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
    }
    private  void requestPermission(String[] strings, int requestCamera){
        ActivityCompat.requestPermissions(QRCodeScanner.this, new String[] {Manifest.permission.CAMERA},REQUEST_CAMERA);
    }
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void  onRequestPermissionResult(int requestCode, String permission[], int[] grantresult){
        switch (requestCode){
            case REQUEST_CAMERA:
                if(grantresult.length > 0){
                    boolean cameraAccept = grantresult[0] == PackageManager.PERMISSION_GRANTED;
                    if(cameraAccept){
                        Toast.makeText(getApplicationContext(), "Permission Granted by the user", Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(),"Permission Not granted by user for camera", Toast.LENGTH_LONG).show();
                        if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
                               showMessageOKCancel("You need to grant camera permission to the app",
                                       new DialogInterface.OnClickListener() {
                                           @Override
                                           public void onClick(DialogInterface dialogInterface, int i) {
                                               if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                                                   requestPermission(new String[]{Manifest.permission.CAMERA},REQUEST_CAMERA);


                                               }
                                           }
                                       });
                               return;
                        }
                    }
                    break;
                }
        }

    }
    @Override
    public void onResume(){
        super.onResume();
        int currentapiVersion = Build.VERSION.SDK_INT;
        if(currentapiVersion >= Build.VERSION_CODES.M){
            if(checkPermission()){
                if(ScannerView == null){
                    ScannerView = new ZXingScannerView(this);
                    setContentView(ScannerView);
                }
                ScannerView.setResultHandler(this);
                ScannerView.startCamera();
            }
        }
    }


    @Override
    public void onDestroy(){
        super.onDestroy();
        ScannerView.stopCamera();
        ScannerView = null;
    }
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener ){
        new AlertDialog.Builder(QRCodeScanner.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel",null)
                .create()
                .show();
    }
     private static String credentials[];

public String Ip;
    public void handleResult(Result result){
//                onWifiChangeBroadcastReceived(getApplicationContext(), new Intent());


                final String rawresult = result.getText();
        credentials = rawresult.split("_");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("ScanResult")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        ScannerView.resumeCameraPreview(QRCodeScanner.this);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onDestroy();
                    }
                });
                builder.setMessage(credentials[0]+"  "+credentials[1]+" "+credentials[2]);
                this.Ip = credentials[2];
                clientip = credentials[2];
        wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connectToWifi(credentials[0], credentials[1]);
//        Client client = new Client();

        finish();
        super.onBackPressed();



    }
    private void connectToWifi(final String networkSSID, final String networkPassword) {
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = String.format("\"%s\"", networkSSID);
        conf.preSharedKey = String.format("\"%s\"", networkPassword);

        int netId = wifiManager.addNetwork(conf);

        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
    }

}