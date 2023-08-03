package com.revolve44.serviceaccess1;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionHandler {
    private static PermissionCallback permissionCallback;

    public interface PermissionCallback {
        void onPermissionGranted();
        void onPermissionDenied();
    }

    public static void requestInternetPermission(Activity activity, PermissionCallback callback) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.INTERNET},
                    1);
            PermissionHandler.permissionCallback = callback;
        } else {
            callback.onPermissionGranted();
        }
    }

    public static void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionCallback.onPermissionGranted();
            } else {
                permissionCallback.onPermissionDenied();
            }
        }
    }
}