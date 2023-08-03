package com.revolve44.serviceaccess1;

import android.content.Context;
import android.util.Log;

import com.revolve44.serviceaccess1.floatingframe.AutoService;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SocketHandler {
    public static Socket socket = null;
//    public static String SOCKET_SERVER_URL = "http://192.168.143.12:8001";
//    public static String SOCKET_SERVER_URL = "http://127.0.0.1:8001";
//    public static String SOCKET_SERVER_URL = "http://156.227.0.59:8001";
    public static String SOCKET_SERVER_URL =  "http://95.217.67.198:8001";

    public static Context context;

    public static void initSocket (final Context context) {
        try{
            SocketHandler.context = context;
            socket = IO.socket(SOCKET_SERVER_URL);

            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.e("Socket Status", "Connected!");
                    // send OTP code to the server.
                    socket.emit("otp", "123456");
//                  socket.emit("message", "hi");
//                  triggerEvent("test1", "Hello. this is first test event after connection");

                    socket.on("stdValue", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            Log.e("Bank Limit Balance", String.valueOf(args[0]));
                            String response = String.valueOf(args[0]);     // args[0] => {"type":"BankCard","value":50000}
//
                            try {
                                JSONObject jsonObject = new JSONObject(response);
                                String type = jsonObject.getString("type");
                                int value = jsonObject.getInt("value");

                                Log.e("BANK LIMIT BALANCE TYPE", type);
                                Log.e("BANK LIMIT BALANCE VAL", String.valueOf(value));

                                GlobalConstant.LIMIT_BALANCE = value;
                                // Do something with the type and value
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    socket.on("initRollOver", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            Log.e("Init Rollover", String.valueOf(args[0]));
                            String response = String.valueOf(args[0]);    // args[0] => {"bankType":"ABBANK","account":"123123123","amount":55555,"appointedToPay":"xxxx"}
                            // bank type  : shb
                            // bank account number : 1911200288
                            // amount : 10, 000

                            // Set  Target Bank Information
                            // SHB bank type
                            // 1911200288
                            // 10, 000 VND
                            // Note

                            // We will use this information in mTransferringMoney Runnable.
                            try {
                                JSONObject jsonObject = new JSONObject(response);
                                String bankType = jsonObject.getString("bankType");
                                String account = jsonObject.getString("account");
                                int amount = jsonObject.getInt("amount");
                                String appointedToPay = jsonObject.getString("appointedToPay");

                                Log.e("BANK bankType", bankType);
                                Log.e("BANK account", account);
                                Log.e("BANK amount", String.valueOf(amount));
                                Log.e("BANK appointedToPay", appointedToPay);

                                GlobalConstant.TARGET_BANK_TYPE = bankType;
                                GlobalConstant.TARGET_BANK_ACCOUNT = account;
                                GlobalConstant.TARGET_BANK_AMOUNT = amount;
                                GlobalConstant.TARGET_BANK_APPOINTED_TO_PAY = appointedToPay;

                                // Do something with the type and value
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    socket.on("orders", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            Log.e("MoneyWithdrawToCustomer", String.valueOf(args[0]));
                            GlobalConstant.MONEY_WITHDRAW = true;
                            // Source bank information
                            // self

                            String response = String.valueOf(args[0]);
                            // Target bank information
                            // bank type
                            // bank account number
                            // amount

                            AutoService.runWithDraw();

//                            mHandler.postDelayed(mMoneyTransferRunnable, 5000);
                        }
                    });
                }
            }).on("message", new Emitter.Listener() {
                //message is the keyword for communication exchanges
                @Override
                public void call(Object... args) {
                    socket.emit("message", "hi");
                }
            }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {}
            });
            socket.connect();
        }
        catch(Exception e){
            Log.e("socket exception", e.toString());
        }
    }

    public static void disconnectSocket() {
        socket.disconnect();
    }
    public static void triggerEvent(String event,  String data) {
        socket.emit(event, data);
    }

    public static void triggerEvent(String event, byte[] data) {socket.emit(event, data);}
}
