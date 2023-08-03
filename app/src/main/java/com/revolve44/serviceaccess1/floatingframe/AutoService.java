package com.revolve44.serviceaccess1.floatingframe;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.revolve44.serviceaccess1.AutoDataModel;
import com.revolve44.serviceaccess1.DetectManager;
import com.revolve44.serviceaccess1.GlobalConstant;
import com.revolve44.serviceaccess1.KeyboardPoints;
import com.revolve44.serviceaccess1.MainActivity;
import com.revolve44.serviceaccess1.SocketHandler;

import org.json.JSONObject;

public class AutoService extends AccessibilityService {

    public static Context context;

    private static Handler mHandler;
    private int mX;
    private int mY;

    public static Boolean isSwipePlayingForMonitoring = false;
    private static byte[] screenshotData;
    private static String screenshotType = "";

    public static int RUNNABLE_STATUS = GlobalConstant.RUNNABLE_START;

    public AutoDataModel[] mMonitoringPoints = new AutoDataModel[]{
            new AutoDataModel(new Point(192, 624), 15000),

            new AutoDataModel(new Point(420, 1664), 500),
            new AutoDataModel(new Point(420, 1664), 500),
            new AutoDataModel(new Point(420, 1664), 500),
            new AutoDataModel(new Point(714, 1664), 500),
            new AutoDataModel(new Point(714, 1664), 500),
            new AutoDataModel(new Point(714, 1664), 15000),

            new AutoDataModel(new Point(811, 446), 15000)
    };

    public AutoDataModel[] mSwipePoints = new AutoDataModel[]{
            new AutoDataModel(new Point(509, 418)),
            new AutoDataModel(new Point(509, 1189))
    };

    public AutoDataModel[] mMoveMoneyPoints = new AutoDataModel[] {
            new AutoDataModel(new Point(352, 1035), 10000),    // move money button click

            // Password
            new AutoDataModel(new Point(420, 1664), 1000),
            new AutoDataModel(new Point(420, 1664), 1000),
            new AutoDataModel(new Point(420, 1664), 1000),
            new AutoDataModel(new Point(714, 1664), 1000),
            new AutoDataModel(new Point(714, 1664), 1000),
            new AutoDataModel(new Point(714, 1664), 10000),

            new AutoDataModel(new Point(344, 1689), 10000),   // to someone else
            new AutoDataModel(new Point(347, 1024), 10000),
            new AutoDataModel(new Point(405, 1364), 10000),

            // tap input to search bank
            new AutoDataModel(new Point(261, 501)),

            //TODO: bank name input dynamatically
            new AutoDataModel(KeyboardPoints.keyPointsByKey("s"), 500),
            new AutoDataModel(KeyboardPoints.keyPointsByKey("h"), 500),
            new AutoDataModel(KeyboardPoints.keyPointsByKey("b")),
//            new AutoDataModel(new Point(220, 1882), 500),   //s
//            new AutoDataModel(new Point(631, 1857), 500),   //h
//            new AutoDataModel(new Point(631, 2009), 500),   //b

            new AutoDataModel(new Point(476, 844)),     //  SHB bank select
            new AutoDataModel(new Point(68, 2179)),     // 123 number change

            //TODO: bank account number dynamatically
            // HOANG THI LAN SHB
            new AutoDataModel(new Point(58, 1753), 500),   // 1
            new AutoDataModel(new Point(912, 1753), 500),  // 9
            new AutoDataModel(new Point(58, 1753), 500),    // 1
            new AutoDataModel(new Point(58, 1753), 500),   // 1
            new AutoDataModel(new Point(164, 1753), 500),  // 2
            new AutoDataModel(new Point(1018, 1753), 500),   // 0
            new AutoDataModel(new Point(1018, 1753), 500),   // 0
            new AutoDataModel(new Point(164, 1753), 500),  // 2
            new AutoDataModel(new Point(805, 1753), 500),   // 8
            new AutoDataModel(new Point(805, 1753), 500),   // 8

            new AutoDataModel(new Point(998, 2194), 10000),    // enter keyboard
            new AutoDataModel(new Point(567, 2141), 10000),    // Next button

            //TODO: Amount input dynamatically.
            // Amount
            new AutoDataModel(new Point(129, 1649), 1000),    // 1
//            new AutoDataModel(new Point(129, 2131), 1000),    // 000
//            new AutoDataModel(new Point(413, 2131)),    // 0

            new AutoDataModel(new Point(998, 2194)),  // New
            new AutoDataModel(new Point(998, 2194), 10000),   // -> keyboard Enter

            // Confirm with Passcode
            new AutoDataModel(new Point(567, 2141), 20000),  // confirm passcode button

            new AutoDataModel(new Point(420, 1664), 5000),
            new AutoDataModel(new Point(420, 1664), 500),
            new AutoDataModel(new Point(420, 1664), 500),
            new AutoDataModel(new Point(714, 1664), 500),
            new AutoDataModel(new Point(714, 1664), 500),
            new AutoDataModel(new Point(714, 1664), 60000),
            // loading ... ... ...
            // send Screenshot to the web server

            // click Done
            new AutoDataModel(new Point(567, 2118))
    };

    public AutoDataModel[] mLogoutsMonitoringPoints = new AutoDataModel[]{
            // back pressed
            new AutoDataModel(new Point(76, 208), 10000),  // back pressed
            new AutoDataModel(new Point(76, 208), 10000),   // open left draw layout menu
            new AutoDataModel(new Point(164, 2219), 20000), // tap sign out button
    };

    public AutoDataModel[] mLogoutsTransferringPoints = new AutoDataModel[]{
            new AutoDataModel(new Point(76, 208), 10000),   // open left draw layout menu
            new AutoDataModel(new Point(164, 2219), 20000), // tap sign out button
    };

    // Monitoring Runnable
    public static MonitoringIntervalRunnable mMonitoringRunnable;
    public int monitoringIndex = 0;

    public  int tempIndex = 0;

    private class MonitoringIntervalRunnable implements Runnable {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            if(monitoringIndex < mMonitoringPoints.length) {
                AutoService.RUNNABLE_STATUS = GlobalConstant.GOING_FOR_MONITORING;

                playTap(mMonitoringPoints[monitoringIndex].point.x, mMonitoringPoints[monitoringIndex].point.y);
                isSwipePlayingForMonitoring = false;

                mHandler.postDelayed(mMonitoringRunnable, mMonitoringPoints[monitoringIndex].delay);
                monitoringIndex++;
            } else {
                // swipe
                AutoService.RUNNABLE_STATUS = GlobalConstant.RUNNING_SWIPING_FOR_MONITORING;


                playTap(mSwipePoints[0].point.x, mSwipePoints[0].point.y, mSwipePoints[1].point.x, mSwipePoints[1].point.y);
                isSwipePlayingForMonitoring = true;

                screenshotType = GlobalConstant.SCREENSHOT_BALANCE;
                MainActivity.screenshot();

                tempIndex++;
                if(tempIndex == 3 )  {
                    monitoringIndex = 0;
                    mHandler.postDelayed(mLogoutsMonitoringRunnable, 10000);
                    tempIndex = 0;
                    return;
                }


                // check balance with GlobalConstant.LIMIT_BALANCE
                if(GlobalConstant.LIMIT_BALANCE != 0 && GlobalConstant.CUR_BALANCE > GlobalConstant.LIMIT_BALANCE) {    // it means limit balance value is set from the server. so we should check if cur balance is over limit balance value.
                        // stop this monitoring interval runnable and run move transfer money
//                        mHandler.postDelayed(mMoneyTransferRunnable, 5000);
                        monitoringIndex = 0;
                        mHandler.postDelayed(mLogoutsMonitoringRunnable, 10000);
                        return;
                }

                mHandler.postDelayed(mMonitoringRunnable, 20000);
                // perform swipe to check current balance 10s
            }
        }
    }

    // MoneyTransfer Runnable
    public static MoneyTransferIntervalRunnable mMoneyTransferRunnable;
    public int moneyTransferIndex = 0 ;

    private class MoneyTransferIntervalRunnable implements Runnable {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            if(moneyTransferIndex < mMoveMoneyPoints.length) {
                AutoService.RUNNABLE_STATUS = GlobalConstant.GOING_FOR_TRANSFERRING;

                if(moneyTransferIndex == mMoveMoneyPoints.length - 1) {
                    // take screenshot of last screen and send image to server

                    screenshotType = GlobalConstant.SCREENSHOT_FINAL_TRANSACTION;
                    MainActivity.screenshot();

                    Toast.makeText(getApplicationContext(), "Done -- ScreenCapture", Toast.LENGTH_SHORT).show();
                    try {
                        android.util.Log.d("SCREENSHOT SIZE", String.valueOf(screenshotData.length));
                    } catch(Exception ex) {

                    }

                    AutoService.RUNNABLE_STATUS = GlobalConstant.FINISHED_TRANSFERRING;
                }
                playTap(mMoveMoneyPoints[moneyTransferIndex].point.x, mMoveMoneyPoints[moneyTransferIndex].point.y);
                isSwipePlayingForMonitoring = false;

                mHandler.postDelayed(mMoneyTransferRunnable, mMoveMoneyPoints[moneyTransferIndex].delay);
                moneyTransferIndex++;
            } else {
                moneyTransferIndex = 0;
                mHandler.postDelayed(mLogoutsTransferringRunnable, 10000);
            }
        }
    }

    public static LogoutsMonitoringRunnable mLogoutsMonitoringRunnable;
    public int logoutMonitoringIndex = 0;

    private class LogoutsMonitoringRunnable implements Runnable {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            if(logoutMonitoringIndex < mLogoutsMonitoringPoints.length) {
                AutoService.RUNNABLE_STATUS = GlobalConstant.LOGGING_OUT_FROM_MONITORING;

                playTap(mLogoutsMonitoringPoints[logoutMonitoringIndex].point.x, mLogoutsMonitoringPoints[logoutMonitoringIndex].point.y);
                mHandler.postDelayed(mLogoutsMonitoringRunnable, mLogoutsMonitoringPoints[logoutMonitoringIndex].delay);
                logoutMonitoringIndex ++;
            } else {
                mHandler.postDelayed(mMoneyTransferRunnable, 5000);
                logoutMonitoringIndex = 0;
                return;
            }
        }
    }

    public static LogoutsTransferringRunnable mLogoutsTransferringRunnable;
    public int logoutTransferringIndex = 0;

    private class LogoutsTransferringRunnable implements Runnable {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            if(logoutTransferringIndex < mLogoutsTransferringPoints.length) {
                AutoService.RUNNABLE_STATUS = GlobalConstant.LOGGING_OUT_FROM_TRANSFERRING;

                playTap(mLogoutsTransferringPoints[logoutTransferringIndex].point.x, mLogoutsTransferringPoints[logoutTransferringIndex].point.y);
                mHandler.postDelayed(mLogoutsTransferringRunnable, mLogoutsTransferringPoints[logoutTransferringIndex].delay);
                logoutTransferringIndex ++;
            } else {
                logoutTransferringIndex = 0;
                mHandler.postDelayed(mMonitoringRunnable, 5000);
            }
        }
    }


    public static LogoutsTransferringAfterTransferringRunnable mLogoutsTransferringAfterTransferringRunnable;
    public int logoutTransferringAfterTransferringIndex = 0;

    private class LogoutsTransferringAfterTransferringRunnable implements Runnable {
        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void run() {
            if(logoutTransferringAfterTransferringIndex < mLogoutsTransferringPoints.length) {
                AutoService.RUNNABLE_STATUS = GlobalConstant.LOGGING_OUT_FROM_TRANSFERRING;

                playTap(mLogoutsTransferringPoints[logoutTransferringAfterTransferringIndex].point.x, mLogoutsTransferringPoints[logoutTransferringAfterTransferringIndex].point.y);
                mHandler.postDelayed(mLogoutsTransferringAfterTransferringRunnable, mLogoutsTransferringPoints[logoutTransferringAfterTransferringIndex].delay);
                logoutTransferringAfterTransferringIndex ++;
            } else {
                logoutTransferringAfterTransferringIndex = 0;
                mHandler.postDelayed(mMoneyTransferRunnable, 5000);
            }
        }
    }

    // MainActivity.screenshot() =>
    static  void setScreenData(byte[] screenshotImgData) {
        Log.d("SCREENSHOT SIZE", "setScreenData");
        Bitmap newBitmap = BitmapFactory.decodeByteArray(screenshotImgData, 0, screenshotImgData.length);

        String extractedText = "";

        if (GlobalConstant.tesseractApi  != null) {
            TessBaseAPI tessearctApi = GlobalConstant.tesseractApi;
            tessearctApi.setImage(newBitmap);
            extractedText = tessearctApi.getUTF8Text();
        }

        if(screenshotType == GlobalConstant.SCREENSHOT_BALANCE) {
            // detect balance
            // Move the code from FloatingView to AutoService here.


        } else if(screenshotType == GlobalConstant.SCREENSHOT_FINAL_TRANSACTION) {
            // detect text from transaction and send text
            detectTextTransaction(screenshotImgData, extractedText);
        }

        screenshotData = screenshotImgData;
    }

   public static void runWithDraw() {
        /*
    public static int GOING_FOR_MONITORING = 1;
    public static int GOING_FOR_TRANSFERRING = 3;
    public static int LOGGING_OUT_FROM_MONITORING = 5;
    public static int LOGGING_OUT_FROM_TRANSFERRING = 6;
    public static int RUNNABLE_START = 0;
    public static int RUNNING_SWIPING_FOR_MONITORING = 2;
    public static int FINISHED_TRANSFERRING = 4;
         */
       if (AutoService.RUNNABLE_STATUS == GlobalConstant.GOING_FOR_MONITORING ||
           AutoService.RUNNABLE_STATUS == GlobalConstant.GOING_FOR_TRANSFERRING ||
           AutoService.RUNNABLE_STATUS == GlobalConstant.LOGGING_OUT_FROM_MONITORING ||
           AutoService.RUNNABLE_STATUS == GlobalConstant.LOGGING_OUT_FROM_TRANSFERRING)
       {
           //
           Toast.makeText(AutoService.context, "Busy! Can't perform transaction!", Toast.LENGTH_SHORT).show();
       } else if(AutoService.RUNNABLE_STATUS == GlobalConstant.RUNNING_SWIPING_FOR_MONITORING){
           // logoutsMonitoringRunnable
           // TransferringMoneyRunnable
           AutoService.RUNNABLE_STATUS = GlobalConstant.RUN_WITHDRAW;

           Toast.makeText(AutoService.context, "RUNNING_SWIPING_FOR_MONITORING", Toast.LENGTH_SHORT).show();
            mHandler.postDelayed(mLogoutsMonitoringRunnable, 20000);
       } else if(AutoService.RUNNABLE_STATUS == GlobalConstant.FINISHED_TRANSFERRING) {
           // logoutsTransferringRunnable
           // TransferringMoneyRunnable
           AutoService.RUNNABLE_STATUS = GlobalConstant.RUN_WITHDRAW;

           Toast.makeText(AutoService.context, "FINISHED_TRANSFERRING", Toast.LENGTH_SHORT).show();
            mHandler.postDelayed(mLogoutsTransferringAfterTransferringRunnable, 20000);

       } else if(AutoService.RUNNABLE_STATUS == GlobalConstant.RUNNABLE_START) {
           // TransferringMoneyRunnable
           AutoService.RUNNABLE_STATUS = GlobalConstant.RUN_WITHDRAW;

           Toast.makeText(AutoService.context, "RUNNABLE_START", Toast.LENGTH_SHORT).show();
           mHandler.postDelayed(mMoneyTransferRunnable, 20000);
       }
    }

    public static void detectTextTransaction(byte[] screenshotImgData, String extractedText) {
        JSONObject jsonObject = new JSONObject();
        jsonObject = DetectManager.detectFinalMoneyMove(extractedText);
        Log.e("DetectTransaction", jsonObject.toString());

        SocketHandler.triggerEvent("transaction", jsonObject.toString());
        SocketHandler.triggerEvent("screenshot", screenshotImgData);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        HandlerThread handlerThread = new HandlerThread("auto-handler");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
    }

    @Override
    protected void onServiceConnected() {

    }

    private class TestRunnable implements Runnable {
        @Override
        public void run() {
                Log.e("Here is Test Runnable.", "Here is Test Runnable!");
                mHandler.postDelayed(testRunnable, 2000);
        }
    }

    TestRunnable testRunnable;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("Service","SERVICE STARTED");
        if(intent!=null){
            String action = intent.getStringExtra("action");
            if (action.equals("play")) {
                mX = intent.getIntExtra("x", 0);
                mY = intent.getIntExtra("y", 0);

                // Monitoring Runnable initialzie and run
                if (mMonitoringRunnable == null) {
                    mMonitoringRunnable = new MonitoringIntervalRunnable();
                }
                mHandler.postDelayed(mMonitoringRunnable, 5000);

//                testRunnable = new TestRunnable();
//                mHandler.postDelayed(testRunnable, 2000);

                // MoneyTransfer Runnable initialize
                if(mMoneyTransferRunnable == null) {
                    mMoneyTransferRunnable = new MoneyTransferIntervalRunnable();
                }

//                mHandler.postDelayed(mMoneyTransferRunnable, 5000);


                if(mLogoutsMonitoringRunnable == null) {
                    mLogoutsMonitoringRunnable = new LogoutsMonitoringRunnable();
                }

                if(mLogoutsTransferringRunnable == null) {
                    mLogoutsTransferringRunnable = new LogoutsTransferringRunnable();
                }

                if(mLogoutsTransferringAfterTransferringRunnable == null) {
                    mLogoutsTransferringAfterTransferringRunnable = new LogoutsTransferringAfterTransferringRunnable();
                }

                Toast.makeText(getBaseContext(), "Auto Click Started", Toast.LENGTH_SHORT).show();
            }
            else if(action.equals("stop")){
                mHandler.removeCallbacksAndMessages(null);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    //@RequiresApi(api = Build.VERSION_CODES.N)
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void playTap(int x, int y) {
        Path swipePath = new Path();
        swipePath.moveTo(x, y);
        swipePath.lineTo(x, y);

        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 10));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            boolean isDispatched = dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    Log.e("Gesture Completed","Gesture Completed");
                    super.onCompleted(gestureDescription);
//                  mHandler.postDelayed(mMonitoringRunnable, 5000);
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    Log.e("Gesture Cancelled","Gesture Cancelled");
                    super.onCancelled(gestureDescription);
                }
            }, null);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void playTap(int fromX, int fromY, int toX, int toY) {
        Path swipePath = new Path();
        swipePath.moveTo(fromX, fromY);
        swipePath.lineTo(toX, toY);

        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 10));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            boolean isDispatched = dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    Log.e("Gesture Completed","Gesture Completed");
                    super.onCompleted(gestureDescription);
//                    mHandler.postDelayed(mMonitoringRunnable, 5000);
                }

                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    Log.e("Gesture Cancelled","Gesture Cancelled");
                    super.onCancelled(gestureDescription);
                }
            }, null);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

}