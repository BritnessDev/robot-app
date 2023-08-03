package com.revolve44.serviceaccess1;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class DetectManager {

    public static HashMap<String, String> detectBalance(String detectResult) {
        HashMap<String, String> map = new HashMap<>();

        return map;
    }

//    public static HashMap<String, String>[] detectTransactionHistories(String detectResult) {
//            //
//    }

    public static JSONObject detectFinalMoneyMove(String detectResult) {
        JSONObject jsonObject = new JSONObject();

        // Set bank name
        try {
            jsonObject.put("bank_type",  "TECHCOMBANK");

            // Split text by '/n'
            String[] lines = detectResult.split("\n");
            int index = 0;

            // Check each line and extract necessary infos
            for (String line: lines) {
                line = line.trim();
                if (line.startsWith("TO")) { // Destination
                    jsonObject.put("dest", line.substring(2).trim());
                } else if (line.startsWith("VND")) { // VND
                    jsonObject.put("vnd", line.substring(3).trim());
                } else if (line.startsWith("Receipient") && index + 2 < lines.length) { // Receipient details
                    jsonObject.put("bank_account_number", lines[index + 2]);
                } else if (line.startsWith("Transaction") && index + 1 < lines.length) { // Transaction ID
                    jsonObject.put("transaction_id", lines[index]);
                }
                index ++;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}