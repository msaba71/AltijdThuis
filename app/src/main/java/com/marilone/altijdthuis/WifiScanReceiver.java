package com.marilone.altijdthuis;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.List;

public class WifiScanReceiver extends BroadcastReceiver {
    WifiManager wifi;
    String wifis[];
    List<ScanResult> wifiScanList;
    boolean AccesspointFound;

    public void onReceive(Context c, Intent intent) {
        wifi = (WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiScanList = wifi.getScanResults();
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

    void connectToAP(String ssid, final String passkey, Context c) {
        final WifiConfiguration wifiConfiguration = new WifiConfiguration();
        final String networkSSID = ssid;
        final String[] password = {""};
        for (ScanResult result : wifiScanList) {

            if (result.SSID.equals(networkSSID)) {

                String securityMode = getScanResultSecurity(result);

                if (securityMode.equals("PSK")) {
                    final AlertDialog.Builder dialog = new AlertDialog.Builder(c);
                    dialog.setTitle("Password");
                    dialog.setMessage("Enter Passsword");
                    final EditText editText = new EditText(c);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                    editText.setLayoutParams(lp);
                    dialog.setView(editText);


                    dialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            password[0] = editText.getText().toString();
                            wifiConfiguration.SSID = "\"" + networkSSID + "\"";
                            wifiConfiguration.preSharedKey = "\"" + editText.getText().toString() + "\"";
                            wifiConfiguration.hiddenSSID = true;
                            wifiConfiguration.status = WifiConfiguration.Status.ENABLED;
                            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                            wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                            wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                            wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                            wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

                            int res = wifi.addNetwork(wifiConfiguration);

                            wifi.enableNetwork(res, true);

                            boolean changeHappen = wifi.saveConfiguration();

                            if (res != -1 && changeHappen) {

                                // .connectedSsidName = networkSSID;

                            } else {
                                // Log.d(TAG, "*** Change NOT happen");
                            }

                            wifi.setWifiEnabled(true);

                        }
                    });

                    dialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    dialog.show();

                }

                if (securityMode.equalsIgnoreCase("OPEN")) {

                    wifiConfiguration.SSID = "\"" + networkSSID + "\"";
                    wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    int res = wifi.addNetwork(wifiConfiguration);
                    boolean b = wifi.enableNetwork(res, true);
                    wifi.setWifiEnabled(true);

                }
            }
        }
    }

    String getScanResultSecurity(ScanResult scanResult) {

        final String cap = scanResult.capabilities;
        final String[] securityModes = {"WEP", "PSK", "EAP"};

        for (int i = securityModes.length - 1; i >= 0; i--) {
            if (cap.contains(securityModes[i])) {
                return securityModes[i];
            }
        }
        return "OPEN";
    }

}

