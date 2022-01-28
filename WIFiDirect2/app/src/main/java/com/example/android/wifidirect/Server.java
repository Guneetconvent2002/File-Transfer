package com.example.android.wifidirect;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;

import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
public class Server extends AppCompatActivity implements View.OnClickListener {

private ServerSocket serverSocket;
private Socket tempClientSocket;
        Thread serverThread = null;
public static final int SERVER_PORT = 3003;
private LinearLayout msgList;
private Handler handler;
private int greenColor;
private EditText edMessage;
private  String Read;
    String Path;

@Override
protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Server");
        greenColor = ContextCompat.getColor(this, R.color.green);
        handler = new Handler();
        msgList = findViewById(R.id.msgList);
        edMessage = findViewById(R.id.edMessage);
        }

public TextView textView(String message, int color) {
        if (null == message || message.trim().isEmpty()) {
        message = "<Empty Message>";
        }
        TextView tv = new TextView(this);
        tv.setTextColor(color);
        tv.setText(message + " [" + getTime() +"]");
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
        if (view.getId() == R.id.start_server) {
        msgList.removeAllViews();
        showMessage("Server Started.", Color.BLACK);
        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();
        return;
        }
        if (view.getId() == R.id.send_data) {
        String msg = edMessage.getText().toString().trim();
        showMessage("Server : " + msg, Color.BLUE);
        sendMessage(msg);

        }
        }

private void sendMessage(final String message) {
        try {
        if (null != tempClientSocket) {
        new Thread(new Runnable() {
@Override
public void run() {
        PrintWriter out = null;
        try {
        out = new PrintWriter(new BufferedWriter(
        new OutputStreamWriter(tempClientSocket.getOutputStream())),
        true);
        } catch (IOException e) {
        e.printStackTrace();
        }
        out.println(message);
        }
        }).start();
        }
        } catch (Exception e) {
        e.printStackTrace();
        }
        }

class ServerThread implements Runnable {

    public void run() {
        Socket socket= null;
        try {
            serverSocket = new ServerSocket(SERVER_PORT);

        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Error Starting Server : " + e.getMessage(), Color.RED);
        }
        if (null != serverSocket) {
            InputStream in = null;
            OutputStream out = null;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    socket = serverSocket.accept();
                    CommunicationThread commThread = new CommunicationThread(socket);
                    new Thread(commThread).start();
                } catch (IOException e) {
                    e.printStackTrace();
                    showMessage("Error Communicating to Client :" + e.getMessage(), Color.RED);
                }
                try {
                    in = socket.getInputStream();
                } catch (IOException ex) {
                    System.out.println("Can't get socket input stream. ");
                }
//
//                try {
//                    out = new FileOutputStream("M:\\test2.xml");
//                } catch (FileNotFoundException ex) {
//                    System.out.println("File not found. ");
//                }
//
//                byte[] bytes = new byte[16*1024];
//
//                int count=0;
//                while (true) {
//                    try {
//                        if (!((count = in.read(bytes)) > 0)) break;
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    try {
//                        out.write(bytes, 0, count);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                try {
//                    out.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                try {
//                    in.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            }
        }
    }
}


class CommunicationThread implements Runnable {

    private Socket clientSocket;

    private BufferedReader input;

    public CommunicationThread(Socket clientSocket) {
        this.clientSocket = clientSocket;
        tempClientSocket = clientSocket;
        try {
            this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));

        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Error Connecting to Client!!", Color.RED);
        }
        showMessage("Connected to Client!!", greenColor);
    }
    @Override
    public void run() {

        while (!Thread.currentThread().isInterrupted()) {

            try {
                String read = input.readLine();

                if (null == read || "Disconnect".contentEquals(read)) {
                    Thread.interrupted();
                    read = "Client Disconnected";

                    showMessage("Client : " + read, greenColor);
                    break;
                }
                showMessage("Client : " + read, greenColor);
                Read =read;
                File downloads=   Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                 Path= downloads.getAbsolutePath();
//                File  file = new File(Path+"/"+Read);
            } catch (IOException e) {
                e.printStackTrace();
            }


            InputStream in = null;
            OutputStream out = null;

            try {
                in = clientSocket.getInputStream();
            } catch (IOException ex) {
                System.out.println("Can't get socket input stream. ");
            }

            try {
                out = new FileOutputStream(Path+"/"+Read);
            } catch (FileNotFoundException ex) {
                System.out.println("File not found. ");
            }

            byte[] bytes = new byte[1000000];

            int count=0;
            while (true) {
                try {
                    if (!((count = in.read(bytes)) > 0)) break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    out.write(bytes, 0, count);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        }
    }


}


    String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != serverThread) {
            sendMessage("Disconnect");
            serverThread.interrupt();
            serverThread = null;
        }
    }
}
