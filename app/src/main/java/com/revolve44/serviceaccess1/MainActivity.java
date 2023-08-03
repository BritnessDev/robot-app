package com.revolve44.serviceaccess1;

import static android.os.Build.VERSION.SDK_INT;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.revolve44.serviceaccess1.floatingframe.FloatingView;

import io.socket.client.Socket;

public class MainActivity extends AppCompatActivity {
    private static final int SYSTEM_ALERT_WINDOW_PERMISSION = 2084;
    public static final String  DEVICE_ID = "dev_tcb0";
    public static Socket socket;

    private int type = FloatingView.CAPTURE;

    private String TAG = "MainActivity";
    private int result = 0;
    private Intent intent = null;
    private int REQUEST_MEDIA_PROJECTION = 1;
    private MediaProjectionManager mMediaProjectionManager;

    private Activity activity = this;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PermissionHandler.requestInternetPermission(activity, new PermissionHandler.PermissionCallback() {
            @Override
            public void onPermissionGranted() {
                Toast.makeText(getApplicationContext(), "Permission Granted!", Toast.LENGTH_SHORT).show();
                SocketHandler.initSocket(getApplicationContext());
            }

            @Override
            public void onPermissionDenied() {
                // Do something when permission is denied
                Log.e("permssion is denied", "denied");
            }
        });
//
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            askPermission();
        }
        findViewById(R.id.btn_start_robot_floatview).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initService();
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    startService(new Intent(getApplicationContext(), FloatingView.class));
                    Intent i = new Intent(Intent.ACTION_MAIN);
                    i.addCategory(Intent.CATEGORY_HOME);
                    startActivity(i);
                } else if (Settings.canDrawOverlays(getApplicationContext())) {
                    startService(new Intent(getApplicationContext(), FloatingView.class));
                    Intent i = new Intent(Intent.ACTION_MAIN);
                    i.addCategory(Intent.CATEGORY_HOME);
                    startActivity(i);
                } else {
                    askPermission();
                    Toast.makeText(getApplicationContext(), "You need System Alert Window Permission to do this", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.btn_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        if (SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                startActivityForResult(intent, 2296);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, 2296);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    2299);
        }

        startMediaProjectionService();
    }

    @Override
    public  void onDestroy() {
        super.onDestroy();
        stopService();
    }

    private void askPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, SYSTEM_ALERT_WINDOW_PERMISSION);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void startMediaProjectionService() {
        mMediaProjectionManager = (MediaProjectionManager) getApplication().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
        Log.d("MainActivity", "startMediaProjectionService");
    }

    public void initService() {
        Log.d("MainActivity", "initService");
        sendMessage(FloatingView.INIT_SERVICE, type);
    }

    public void stopService() {
        sendMessage(FloatingView.STOP_SERVICE);
    }

    public void sendMessage(int code, Object content) {

        if (isConn) {
            Message msgFromClient = Message.obtain(null, code);

            if (content != null) {
                if (content instanceof Integer) {
                    msgFromClient.arg1 = (int) content;
                } else if (content instanceof String) {
                    Bundle bundle = new Bundle();
                    bundle.putString("content", (String) content);
                    msgFromClient.setData(bundle);
                }
            }

            msgFromClient.replyTo = mMessenger;
            try {
                mService.send(msgFromClient);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "isConn is FALSE", Toast.LENGTH_SHORT).show();
        }
    }

    public void sendMessage(int code) {
        sendMessage(code, null);
    }

    private static Messenger mService;
    private static boolean isConn;
    private static Messenger mMessenger = new Messenger(new Handler() {
        @Override
        public void handleMessage(Message msgFromServer) {
            switch (msgFromServer.what) {
                case FloatingView.CAPTURE_SUCCESS:
                    Bundle bundle = msgFromServer.getData();
                    String path = bundle.getString("path");
                    Log.d("TAG", "path:    " + path);
                    break;
            }
            super.handleMessage(msgFromServer);
        }
    });

    public static void screenshot() {
        if (MainActivity.isConn) {
            Message msgFromClient = Message.obtain(null, FloatingView.CAPTURE);
            msgFromClient.replyTo = MainActivity.mMessenger;
            try {
                MainActivity.mService.send(msgFromClient);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Log.d("GLOBAL", "isConn is FALSE");
        }
    }

    public void detectText(View view) {
        sendMessage(FloatingView.CAPTURE);
    }

    private ServiceConnection mConn = new ServiceConnection() {
        //IBinder 对象，需要Bundle包装，传给Unity页面，和service进行通信
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = new Messenger(service);
            isConn = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            isConn = false;
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode != Activity.RESULT_OK) {
                Log.e("MEDIA_PROJECTION", "PERMISSION REQUEST ERROR!!!");
                return;
            } else if (data != null && resultCode != 0) {
                Log.e("MEDIA_PROJECTION", "PERMISSION REQUEST SUCCESS!!!");

                result = resultCode;
                intent = data;
                Intent intent = new Intent(getApplicationContext(), FloatingView.class);
                intent.putExtra("code", resultCode);
                intent.putExtra("data", data);
                bindService(intent, mConn, Context.BIND_AUTO_CREATE);
            }
        }

        if (requestCode == 2296) {
            if (SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    // perform action when allow permission success
                } else {
                    Toast.makeText(this, "Allow permission for storage access!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}