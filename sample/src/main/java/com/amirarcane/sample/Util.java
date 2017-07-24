package com.amirarcane.sample;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.util.Pair;

import java.util.HashMap;

/**
 * Created by Arcane on 7/24/2017.
 */

public class Util {

    private static final Integer PERMISSION_REQUEST_CODE = 9999;
    private static final String TAG = Util.class.getSimpleName();
    private static Integer permissionStringRes;
    private static HashMap<Integer, Pair<Pair<Activity, OnPermissionCallback>, String[]>> permissionMap = new HashMap<>();

    public static void checkPermission(Activity activity, String[] permissions,
                                       OnPermissionCallback permissionsResultCallback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean req = false;

            for (int i = 0; i < permissions.length; i++) {
                if (activity.checkSelfPermission(permissions[i]) == PackageManager.PERMISSION_DENIED) {
                    req = true;
                }
            }
            if (!req)
                permissionsResultCallback.onPermissionGranted();
            else {
                permissionMap.clear();

                permissionStringRes = R.string.pleaseEnablePermission;
                permissionMap.put(PERMISSION_REQUEST_CODE, new Pair<>(new Pair<>(activity,
                        permissionsResultCallback), permissions));

                activity.requestPermissions(permissions, PERMISSION_REQUEST_CODE);

            }
        } else {
            permissionsResultCallback.onPermissionGranted();
        }
    }

    @SuppressWarnings("MissingPermission")
    public interface OnPermissionCallback {
        void onPermissionGranted();

        void onPermissionDenied();
    }

}
