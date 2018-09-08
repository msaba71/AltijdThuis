package com.marilone.altijdthuis;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.List;

public class WifiScanReceiver extends BroadcastReceiver {
    private WifiManager wifi;
    private String wifis[];
    private List<ScanResult> wifiScanList;
    private boolean AccesspointFound;

    public void onReceive(Context c, Intent intent) {
        wifi = (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            wifiScanList = wifi.getScanResults();
        }
        wifis = new String[wifiScanList.size()];
        int aantal = wifiScanList.size();

        Log.d("Wifi gevonden", Integer.toString(aantal));
        AccesspointFound = false;
        for (int i = 0; i < wifiScanList.size(); i++) {
            wifis[i] = wifiScanList.get(i).toString();
            String SSID = wifiScanList.get(i).SSID;

            if (SSID.equals(c.getString(R.string.accesspoint))) {
                AccesspointFound = true;
            }

            Log.d("SSID", wifiScanList.get(i).SSID);
        }
    }

    public boolean isFound() {
        return AccesspointFound;
    }

}

