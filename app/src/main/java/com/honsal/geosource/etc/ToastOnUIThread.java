package com.honsal.geosource.etc;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

/**
 * Created by 이 on 2015-07-08.
 */
public class ToastOnUIThread {

    private static Activity mainActivity;

    public static final void setMainActivity(Activity activity) {
        mainActivity = activity;
    }

    private static final void makeText(final Context context, final String message, final boolean isLong) {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static final void makeTextShort(Context context, String message) {
        makeText(context, message, false);
    }

    public static void makeTextLong(Context context, String message) {
        makeText(context, message, true);
    }

    private static void makeTextExit(final Context context, final String message, final boolean isLong) {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                if (!isLong) {
                    try {
                        Thread.sleep(2000);
                        System.exit(0);
                    } catch (InterruptedException e) {
                    }
                } else {
                    try {
                        Thread.sleep(3500);
                        System.exit(0);
                    } catch (InterruptedException e) {
                    }
                }
            }
        });

        t.start();
    }

    public static void makeTextShortExit(Context context, String message) {
        makeTextExit(context, message, false);
    }

    public static void makeTextLongExit(Context context, String message) {
        makeTextExit(context, message, true);
    }
}
