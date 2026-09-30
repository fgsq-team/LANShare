package com.fgsqw.lanshare.activity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.util.Log;
import com.fgsqw.lanshare.R;

public class VirtualLocationActivity extends FragmentActivity {

/*    private LocationManager locationManager;
    private String provider;

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            // 当位置发生变化时的处理
            Log.d("VirtualLocation", "onLocationChanged: " + location.toString());
            updata(location);

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // 当provider状态发生变化时的处理
        }

        @Override
        public void onProviderEnabled(String provider) {
            // 当provider被启用时的处理
        }

        @Override
        public void onProviderDisabled(String provider) {
            // 当provider被禁用时的处理
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 获取系统的定位服务
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // 选择定位的provider
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);

        try {
            locationManager.addTestProvider(provider, false, false,
                    false, false, true, true, true, 1, 5);
            locationManager.setTestProviderEnabled(provider, true);
        } catch (SecurityException e) {
            e.printStackTrace();
        }


        // 添加一个检查，确保provider不为null
        if (provider != null) {
            // 添加位置监听器
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            locationManager.requestLocationUpdates(provider, 1000, 1, locationListener);

            // 设置虚拟位置
            setLocation( 37.7749, -122.4194); // 例如，这里设置了旧金山的经纬度
        } else {
            Log.e("VirtualLocation", "Provider is null");
        }
    }

    // 修改设置虚拟位置的方法，将provider作为参数传入
    private void setLocation(double latitude, double longitude) {
        Location mockLocation = new Location(LocationManager.GPS_PROVIDER);
        mockLocation.setLatitude(latitude);
        mockLocation.setLongitude(longitude);
        mockLocation.setAccuracy(1.0f);
        mockLocation.setTime(System.currentTimeMillis());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mockLocation.setElapsedRealtimeNanos(System.nanoTime());
        }

        try {
            // 模拟位置
            locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, mockLocation);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 移除位置监听器
        locationManager.removeUpdates(locationListener);
    }

    private String updata(Location location) {
        if (location != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("实时的位置信息:\n");
            sb.append("经度:");
            sb.append(location.getLongitude());
            sb.append("\n纬度:");
            sb.append(location.getLatitude());
            sb.append("\n高度:");
            sb.append(location.getAltitude());
            sb.append("\n速度：");
            sb.append(location.getSpeed());
            sb.append("\n方向：");
            sb.append(location.getBearing());
            sb.append("\n当地时间：");
            sb.append(location.getTime());
            return sb.toString();
        }
        return null;
    }*/
}



