package com.example.android.wifidirect;


import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.os.Handler;

import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.os.Bundle;
import android.util.Base64;

public class Clientt extends Activity implements View.OnClickListener {

    public static final int SERVERPORT = 3003;
    EditText txt_pathShow;
    Button btn_fileclicker,btn_sender;
    Intent myfileintent;
    public static  String SERVER_IP = "192.168.49.119";
    private ClientThread clientThread;
    private Thread thread;
    private LinearLayout msgList;
    private Handler handler;
    private int clientTextColor;
    private EditText edMessage;
    private static final int SOCKET_TIMEOUT = 5000;
    String send,Path;
    public static final String EXTRAS_FILE_PATH = "file_url";
    String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clientt);
        txt_pathShow = (EditText)findViewById(R.id.txt_path);
        SERVER_IP = txt_pathShow.getText().toString();
        btn_fileclicker = (Button)findViewById(R.id.btn_fileclicker);
        setTitle("Client");
        clientTextColor = ContextCompat.getColor(this, R.color.green);
        handler = new Handler();
        msgList = findViewById(R.id.msgList);
        edMessage = findViewById(R.id.edMessage);
        btn_sender = (Button)findViewById(R.id.btn_sender) ;
        btn_sender.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_TEXT,"Demo Title");
                if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.N) {
                    Uri path= FileProvider.getUriForFile(Clientt.this,"com.example.android.wifidirect",new File(Path));
                    intent.putExtra(Intent.EXTRA_STREAM,Path);
                }
                else{
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(Path)));
                }
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setType("*/*");
                startActivity(intent);

            }
        });
        btn_fileclicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myfileintent = new Intent(Intent.ACTION_GET_CONTENT);
                myfileintent.setType("*/*");

                startActivityForResult(myfileintent,10);


//


            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode){
            case 10:
                if(resultCode==RESULT_OK){

//                    PathUtils.getPath(getApplicationContext(), path);
//                    String path1 = path.toString();
                    Uri path = data.getData();


                    String filePath= null;
                    try {
                        filePath = PathUtils.getPath(getApplicationContext(),path);
                        File file = new File(filePath);
                        String p[] ;

                        p= filePath.split("/");
                        int index = p.length;
                        index-=1;
                        name = p[index];
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }

                    this.Path = filePath;
                    String  Send = encodeFileToBase64Binary(new File(Path));
                        this.send = Send;

//                    txt_pathShow.setText(Send);

                }
        }
    }



    public TextView textView(String message, int color) {
//        if (null == message || message.trim().isEmpty()) {
//            message = "<Empty Message>";
//        }
        TextView tv = new TextView(this);
        tv.setTextColor(color);
        tv.setText(message + " [" + getTime() + "]");
        tv.setTextSize(20);
        tv.setPadding(0, 5, 0, 0);
        return tv;
    }

    public void showMessage(final String message, final int color) {
        handler.post(new Runnable() {
            @Override
            public void run() {

                msgList.addView(textView(message, color));
            }
        });
    }

    @Override
    public void onClick(View view) {

        if (view.getId() == R.id.connect_server) {
            msgList.removeAllViews();
            showMessage("Connecting to Server...", clientTextColor);
            clientThread = new ClientThread();
            thread = new Thread(clientThread);
            thread.start();

//            showMessage("Connected to Server...", clientTextColor);
            return;
        }

        if (view.getId() == R.id.send_data) {
            File clientMessage = new File(Path) ;
//            showMessage(clientMessage, Color.BLUE);
            if (null != clientThread) {
                clientThread.sendMessage(name);
                clientThread.sendMessage(clientMessage);
            }
            if (view.getId()==R.id.btn_fileclicker){
                myfileintent = new Intent(Intent.ACTION_GET_CONTENT);
                myfileintent.setType("*/*");
                startActivityForResult(myfileintent,10);


            }
        }
    }
    private String encodeFileToBase64Binary(File yourFile) {
        int size = (int) yourFile.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(yourFile));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String encoded = Base64.encodeToString(bytes, Base64.NO_WRAP);
        return encoded;
    }

    class ClientThread implements Runnable {

        private Socket socket;
        private BufferedReader input;

        @Override
        public void run() {

            try {
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket(serverAddr, SERVERPORT);
                socket.bind(null);
                socket.connect((new InetSocketAddress(SERVER_IP, SERVERPORT)), SOCKET_TIMEOUT);
                showMessage("Connected to Server...", clientTextColor);
                while (!Thread.currentThread().isInterrupted()) {

                    this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String message = input.readLine();
                    if (null == message || "Disconnect".contentEquals(message)) {
                        Thread.interrupted();
                        message = "Server Disconnected.";
                        showMessage(message, Color.RED);
                        break;
                    }
                    showMessage("Server: " + message, clientTextColor);
                }

            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        }

        void sendMessage( File message) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (null != socket) {
//                            PrintWriter out = new PrintWriter(new BufferedWriter(
//                                    new OutputStreamWriter(socket.getOutputStream())),
//                                    true);

//
                            byte[] bytes = new byte[1000000];
                            InputStream in = new FileInputStream(message);
                            OutputStream out = socket.getOutputStream();

                            int count;
                            while ((count = in.read(bytes)) > 0) {
                                out.write(bytes, 0, count);
                            }

                            out.close();
                            in.close();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        void    sendMessage(String message){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (null != socket) {
                            PrintWriter out = new PrintWriter(new BufferedWriter(
                                    new OutputStreamWriter(socket.getOutputStream())),
                                    true);

                                out.println(message);

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }

    }

    String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != clientThread) {
//            clientThread.sendMessage("Disconnect");
            clientThread = null;
        }
    }
}