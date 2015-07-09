package com.honsal.geosource.etc;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by 이 on 2015-07-08.
 */
public class GPSInfo extends Service implements LocationListener {
    private static final String TAG = "GPSInfo";

    private Context context;
    private boolean isGPSEnabled = false;
    private boolean isNetworkEnabled = false;
    private boolean canGetLocation = false;

    private Location location;
    private float latitude;
    private float longitude;
    private float direction;

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 10; // 1 SEC

    protected LocationManager locationManager;

    public GPSInfo(Context context) {
        this.context = context;
        getLocation();
    }

    private Location getLocation() {
        try {
            locationManager = (LocationManager)context.getSystemService(LOCATION_SERVICE);

            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                Log.e(TAG, "NO LOCATION PROVIDER ENABLED.");
                Toast.makeText(context, "NO LOCATION PROVIDER ENABLED", Toast.LENGTH_SHORT).show();
            } else {
                canGetLocation = true;
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    Toast.makeText(context, "네트워크 위치 제공자가 활성화됨.", Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Network provider enabled.");
                    if (locationManager != null) {
                        location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            latitude = (float)location.getLatitude();
                            longitude = (float)location.getLongitude();
                            direction = location.getBearing();
                        }
                    }

                    if (isGPSEnabled) {
                        if (location == null) {
                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                            Toast.makeText(context, "GPS 위치 제공자가 활성화됨.", Toast.LENGTH_SHORT).show();
                            Log.w(TAG, "GPS provider enabled.");
                            if (locationManager != null) {
                                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                if (location != null) {
                                    latitude = (float)location.getLatitude();
                                    longitude = (float)location.getLongitude();
                                    direction = location.getBearing();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.toString());
            ToastOnUIThread.makeTextLongExit(context, "위치 제공자 초기화 중 에러 발생: " + e.toString());
        }
        return location;
    }

    public float getLatitude() {
        if (location != null) {
            latitude = (float)location.getLatitude();
        }
        return latitude;
    }

    public float getLongitude() {
        if (location != null) {
            longitude = (float)location.getLongitude();
        }
        return longitude;
    }

    public float getDirection() {
        if (location != null) {
            direction = location.getBearing();
        }

        return direction;
    }

    public boolean canGetLocation() {
        return canGetLocation;
    }

    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setTitle("GPS Setting");

        alertDialog.setMessage("GPS가 활성화되어있지 않습니다. 활성화 메뉴로 이동해 활성화하시겠습니까?");

        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                context.startActivity(intent);
            }
        });

        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                ToastOnUIThread.makeTextLongExit(context, "GPS를 활성화해야 앱을 사용할 수 있습니다.");
            }
        });

        alertDialog.show();
    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
        this.latitude = (float) location.getLatitude();
        this.longitude = (float) location.getLongitude();
        this.direction = location.getBearing();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
