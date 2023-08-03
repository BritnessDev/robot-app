package com.revolve44.serviceaccess1;

import com.googlecode.tesseract.android.TessBaseAPI;

public class GlobalConstant {
    public static int LIMIT_BALANCE = 0;
    public static boolean MONEY_WITHDRAW = false;

    public static int CUR_BALANCE = 0;
    public static int TRANSFER_AMOUNT = 10000;

    public static TessBaseAPI tesseractApi = null;

    public static String SCREENSHOT_BALANCE = "Screenshot balance";
    public static String SCREENSHOT_FINAL_TRANSACTION = "Screenshot final transaction";

    public static String INIT_ROLL_BANK_TYPE  = "shb";
    public static String INIT_ROLL_BANK_ACCOUNT_NUMBER = "1911200288";
    public static int INIT_ROLL_MONEY_AMOUNT = 1000;

    public static int RUNNABLE_START = 0;
    public static int GOING_FOR_MONITORING = 1;
    public static int RUNNING_SWIPING_FOR_MONITORING = 2;
    public static int GOING_FOR_TRANSFERRING = 3;
    public static int FINISHED_TRANSFERRING = 4;
    public static int LOGGING_OUT_FROM_MONITORING = 5;
    public static int LOGGING_OUT_FROM_TRANSFERRING = 6;
    public static int RUN_WITHDRAW = 7;


    // this is the default collection target bank information.
    public static String TARGET_BANK_TYPE = "shb";
    public static String TARGET_BANK_ACCOUNT="1911200288";
    public static int TARGET_BANK_AMOUNT = 10000;
    public static String TARGET_BANK_APPOINTED_TO_PAY = "hello";

}
