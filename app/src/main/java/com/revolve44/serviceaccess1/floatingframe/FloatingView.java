package com.revolve44.serviceaccess1.floatingframe;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.revolve44.serviceaccess1.GlobalConstant;
import com.revolve44.serviceaccess1.MainActivity;
import com.revolve44.serviceaccess1.R;
import com.revolve44.serviceaccess1.SocketHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class FloatingView extends Service implements View.OnClickListener {

    private static final String TAG = "FLOATING VIEW -----> ";

    public static final int CAPTURE = 0x110;
    public static final int CAPTURE_SUCCESS = 0x1113;
    public static final int INIT_SERVICE = 0x111;
    public static final int STOP_SERVICE = 0x112;

    public int mResultCode = 0;
    public Intent mResultData = null;
    public MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private int windowWidth = 0;
    private int windowHeight = 0;

    private DisplayMetrics metrics;
    private int mScreenDensity;
    private ImageReader mImageReader;

    private Messenger mMessenger;

    private WindowManager mWindowManager;
    private View myFloatingView;

    private String transDate = "", transContent = "";

    private int type;

    static class MessageHandler extends Handler {
        FloatingView service;

        public MessageHandler(FloatingView s) {
            service = s;
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CAPTURE:
                    service.startCapture(msg);
                    break;
                case INIT_SERVICE:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        service.initVirtualDisplay(msg.arg1);
                    }
                    break;
                case STOP_SERVICE:
                    service.stopCaptureService();
                    break;
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void startCapture(Message msg) {
//        if (!AutoService.isSwipePlayingForMonitoring) return;

        //client message
        Message msgToClient = Message.obtain(msg);
        Bundle bundle = new Bundle();
        msgToClient.what = CAPTURE_SUCCESS;
        bundle.putString("path", "nameImage--");
        msgToClient.setData(bundle);

        //捕获bitmap
        Image image = mImageReader.acquireLatestImage();
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int width = image.getWidth();
        int height = image.getHeight();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] pngData = byteArrayOutputStream.toByteArray();

        AutoService.setScreenData(pngData);

        Bitmap newBitmap = BitmapFactory.decodeByteArray(pngData, 0, pngData.length);
        detectText(newBitmap);

        image.close();
    }

    void detectText(Bitmap newBitmap) {
        if (!AutoService.isSwipePlayingForMonitoring) return;
        GlobalConstant.tesseractApi.setImage(newBitmap);
        String extractedText = GlobalConstant.tesseractApi.getUTF8Text();

        Log.d("Extracted Text: ", extractedText);
        String[] lines = extractedText.split("\n");
        String[] res;
        Boolean flag = false;
        int skip = 1, dateCnt = 0, amountCnt = 0;
        String[] _transDate = new String[10], _transContent = new String[10];
        for (String line: lines) {
            if (line.trim().startsWith("VND")) {
                String sBalance = line.substring(4).replaceAll("[^0-9]", "");

                try {
                    GlobalConstant.CUR_BALANCE = Integer.parseInt(sBalance);
                    Log.e("INTEGER BALANCE", String.valueOf(GlobalConstant.CUR_BALANCE));
                } catch (Exception e) {
                    throw e;
                }

                Toast toast= Toast.makeText(getApplicationContext()," BALLANCE:::"+String.valueOf(GlobalConstant.CUR_BALANCE), Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 0);
                toast.show();

                String device_id = MainActivity.DEVICE_ID;

                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("device_id", device_id);
                    jsonObject.put("balance", String.valueOf(GlobalConstant.CUR_BALANCE));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                SocketHandler.triggerEvent("balanceOfBank",  jsonObject.toString());

                Log.e("balanceOfBank=====>",  jsonObject.toString());
            } else if (line.startsWith("Transaction history")) {
                flag = true;
                continue;
            }
            if (flag && skip % 3 != 0) {
                if (skip % 3 == 1) {
                    _transDate[dateCnt] = line;
                    dateCnt ++;
                }
                if (skip % 3 == 2) {
                    int index = line.indexOf(") ");
                    String processed = line.substring(index + 2);
                    _transContent[amountCnt] = processed;
                    amountCnt ++;
                }
                skip ++;
            }
        }

        Log.d ("ABC", String.valueOf(transDate));

        int i, updated = 0;
        String cmpString = "";
        if (transDate != null && transContent != null) {
            cmpString = cmpString.concat(transDate);
            cmpString = cmpString.concat(transContent);
        }
        for (i = 0; i < amountCnt; i ++) {
            String iString = _transDate[i];
            iString = iString.concat(_transContent[i]);

            Log.d("I-Index Transaction :: ", iString);
            Log.d("Before Transaction ::", cmpString);

            Boolean bRes = iString.equals(cmpString);

            Log.d("Compared Result :: ", String.valueOf(bRes) );
            if (bRes) break;
            updated ++;
            Log.d("yes", String.valueOf(updated));
        }

        Toast toast= Toast.makeText(getApplicationContext(),String.valueOf(updated) + " transaction is updated successfully", Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL, 0, 0);
        toast.show();

        Log.d("Detected: ", String.valueOf(amountCnt) + " transaction(s) detected.");
        for (i = 0; i < amountCnt; ++ i) {
            Log.d(String.valueOf(i) + ". ", _transDate[i]);
            Log.d(String.valueOf(i) + ". ", _transContent[i]);
        }

        transDate = _transDate[0];
        transContent = _transContent[0];
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void initVirtualDisplay(int type) {
        this.type = type;
        if (mMediaProjection == null) {
            setUpMediaProjection();
        }
        if (type == CAPTURE) {
            if (mImageReader == null || mVirtualDisplay == null) //截取屏幕需要先初始化  virtualDisplay
                virtualDisplay();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void virtualDisplay() {
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
                windowWidth, windowHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                getSurface(), null, null);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private Surface getSurface() {
            initImageReader();
            return mImageReader.getSurface();
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void initImageReader() {
        mImageReader = ImageReader.newInstance(windowWidth, windowHeight, (int)1, 2); //ImageFormat.RGB_565

        Log.d("FLoatingView", "Initialized Image Reader");
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setUpMediaProjection() {
        createNotificationChannel();//构建通知栏，适配api 29,小于29可以不用，
        mMediaProjection = mMediaProjectionManager.getMediaProjection(mResultCode, mResultData);
        Log.d(TAG, "mMediaProjection 初始化成功");

        initVirtualDisplay(CAPTURE);
    }

    private void createNotificationChannel() {
        Notification.Builder builder = new Notification.Builder(getApplicationContext()); //获取一个Notification构造器
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher)) // 设置下拉列表中的图标(大图标)
                //.setContentTitle("SMI InstantView") // 设置下拉列表里的标题
                .setSmallIcon(R.mipmap.ic_launcher) // 设置状态栏内的小图标
                .setContentText("is running......") // 设置上下文内容
                .setWhen(System.currentTimeMillis()); // 设置该通知发生的时间

        /*以下是对Android 8.0的适配*/
        //普通notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("notification_id");
        }
        //前台服务notification适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel("notification_id", "notification_name", NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = builder.build(); // 获取构建好的Notification
        startForeground(110, notification);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void stopCaptureService() {
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }

        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        if (mMediaProjection != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mMediaProjection.stop();
            }
            mMediaProjection = null;
        }
        stopForeground(true);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mResultCode = intent.getIntExtra("code", -1);
        mResultData = intent.getParcelableExtra("data");
        return mMessenger.getBinder();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate() {
        super.onCreate();

        initView();
        initTesseractAPI();

//        final Handler handler = new Handler();
//        Runnable runnable = new Runnable() {
//            @Override
//            public void run() {
//                MainActivity.screenshot();
//                handler.postDelayed(this, 10000);
//            }
//        };
//        handler.postDelayed(runnable, 5000);

        initMediaProjection();
    }

    private void initView() {

        //getting the widget layout from xml using layout inflater
        myFloatingView = LayoutInflater.from(this).inflate(R.layout.floating_view, null);

        int layout_parms;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            layout_parms = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layout_parms = WindowManager.LayoutParams.TYPE_PHONE;
        }

        //setting the layout parameters
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layout_parms,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;

        //getting windows services and adding the floating view to it
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mWindowManager.addView(myFloatingView, params);

        //adding an touchlistener to make drag movement of the floating widget
        myFloatingView.findViewById(R.id.thisIsAnID).setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d("TOUCH","THIS IS TOUCHED");
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        //this code is helping the widget to move around the screen with fingers
                        params.x = initialX + 60 + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        mWindowManager.updateViewLayout(myFloatingView, params);
                        return true;
                }
                return false;
            }
        });

//      Button socketConnectButton = (Button) myFloatingView.findViewById(R.id.btn_socket_conn);
        /*Button detectButton = (Button) myFloatingView.findViewById(R.id.btn_detect);
            detectButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    MainActivity.screenshot();
                }
        });*/

        Button startButton = (Button) myFloatingView.findViewById(R.id.start);
        startButton.setOnClickListener(this);
        Button stopButton = (Button) myFloatingView.findViewById(R.id.stop);
        stopButton.setOnClickListener(this);

        Button tcbButton = (Button) myFloatingView.findViewById(R.id.btn_open_TCB);
        tcbButton.setOnClickListener(this);

        Button closeTCB = (Button) myFloatingView.findViewById(R.id.btn_close_to);
        closeTCB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(intent);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initMediaProjection() {
        mMessenger = new Messenger(new MessageHandler(this));
        mMediaProjectionManager = (MediaProjectionManager) getApplication().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        WindowManager mWindowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        Point realSize = new Point();
        mWindowManager.getDefaultDisplay().getRealSize(realSize);
        windowWidth = realSize.x;
        windowHeight = realSize.y;
        metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
    }

    private void loadDatabaseTesseract() {
        // Specify the file name in the assets folder
        final File externalStorage = Environment.getExternalStorageDirectory();
        // Create a directory for Tessdata in the app's private storage

        File engTrainedFile = new File(externalStorage.getAbsolutePath() + "/tesseract/tessdata/eng.traineddata");
        Toast.makeText(getApplicationContext(), "Train Data Exists: " + String.valueOf(engTrainedFile.exists()), Toast.LENGTH_SHORT).show();

        Log.d("Tesseract: ", "Set Readable to true? " + String.valueOf(engTrainedFile.setReadable(true)));
        Log.d("Tesseract: ", "Train Data Exists?" + String.valueOf(engTrainedFile.exists()));
        Log.d("Tesseract: ", "Train Data Readable?" + String.valueOf(engTrainedFile.canRead()));

        if (engTrainedFile.exists() && engTrainedFile.canRead()) {
            // File exists and is readable
            // Proceed with reading the file
            try {
                FileInputStream fileInputStream = new FileInputStream(engTrainedFile);
                byte[] buffer = new byte[fileInputStream.available()];
                fileInputStream.read(buffer);
                fileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            // File doesn't exist or is not readable
            // Handle the error or take appropriate action
        }

    }
    private void initTesseractAPI() {
        // load database for tesseract
        loadDatabaseTesseract();

        GlobalConstant.tesseractApi = new TessBaseAPI();
        GlobalConstant.tesseractApi.setDebug(true);
        Log.d("VERSION: ", String.valueOf(GlobalConstant.tesseractApi.getVersion()));
        try {
            String dataPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/tesseract";
            boolean success = GlobalConstant.tesseractApi.init(dataPath, "eng");
            Toast.makeText(getApplicationContext(),"TessBaseAPI:" + String.valueOf(success), Toast.LENGTH_SHORT).show();
            Log.d("Tesseract: ", String.valueOf(success));
            if (!success) return;
            Toast.makeText(getApplicationContext(), "Load database file successfully", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.d("Tesseract: ", "No traineddata files found.");
            Toast.makeText(getApplicationContext(), "Error while reading database", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        if (myFloatingView != null) mWindowManager.removeView(myFloatingView);

        stopSelf();
        if (GlobalConstant.tesseractApi != null) {
            GlobalConstant.tesseractApi.recycle();
        }
    }

    @Override
    public void onClick(View v) {
        //Log.d("onClick","THIS IS CLICKED");
        Intent intent = new Intent(getApplicationContext(), AutoService.class);
        switch (v.getId()) {
            case R.id.start:
                //Log.d("START","THIS IS STARTED");
                int[] location = new int[2];
                myFloatingView.getLocationOnScreen(location);
                intent.putExtra("action", "play");
                intent.putExtra("x", location[0] - 1);
                intent.putExtra("y", location[1] - 1);
                break;
            case R.id.stop:
                intent.putExtra("action", "stop");
                mWindowManager.removeView(myFloatingView);
//                Intent appMain = new Intent(getApplicationContext(), MainActivity.class);
//                startActivity(appMain);

                stopForeground(true);
                stopSelf();

                //getApplication().startActivity(appMain);
                //requires the FLAG_ACTIVITY_NEW_TASK flag
                break;
            case R.id.btn_open_TCB:
                intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("package:vn.com.techcombank.bb.app"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.setClassName("vn.com.techcombank.bb.app", "com.techcombank.retail.MainActivity");
                startActivity(intent);
        }
        getApplication().startService(intent);
    }
}