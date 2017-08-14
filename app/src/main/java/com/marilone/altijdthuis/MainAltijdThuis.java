package com.marilone.altijdthuis;

import android.*;
import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.InputType;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.marilone.altijdthuis.packages.DeliveredPackages.PackageItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;


public class MainAltijdThuis extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, parcelFragment.OnListFragmentInteractionListener
   {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainAltijdThuis";

    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private boolean isReceiverRegistered;

       private NsdServiceInfo mServiceInfo;

    private SwipeRefreshLayout swipeContainer;

       WifiManager wifi;
       String wifis[];
       WifiScanReceiver wifiReciever;

       @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_altijd_thuis);

        // NSD
           NsdHelper mNsdHelper = new NsdHelper(this, (FloatingActionButton) findViewById(R.id.fab));
        mNsdHelper.initializeNsd();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
           ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
           if (drawer != null) drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        assert navigationView != null;
        navigationView.setNavigationItemSelectedListener(this);

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SharedPreferences sharedPreferences =
                        getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences
                        .getBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false);
            }
        };

           wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
           wifiReciever = new WifiScanReceiver();
           registerReceiver(wifiReciever, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
           if (ContextCompat.checkSelfPermission(this,
                   android.Manifest.permission.ACCESS_COARSE_LOCATION)
                   != PackageManager.PERMISSION_GRANTED) {
               askLocationPermission();
           } else {
               wifi.startScan();
           }

       }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        assert drawer != null;
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_altijd_thuis, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent( this, MyPreferencesActivity.class);
            startActivity( intent );
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_registreer) {
            Intent intent = new Intent(this, MyPreferencesActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.nav_setwifi) {
            if (!wifiReciever.isFound()) {
                Toast.makeText(getApplicationContext(), "Altijdthuisbox is waarschijnlijk al ingesteld.", Toast.LENGTH_SHORT).show();
            } else {
                SendWifiConfiguration();

            }
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        assert drawer != null;
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

       private void SendWifiConfiguration() {

           final WifiConfiguration wifiConfiguration = new WifiConfiguration();
           final String[] password = {""};
           final String SSID = wifi.getConnectionInfo().getSSID().replace("\"", "");
           final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
           dialog.setTitle("Wifi wachtwoord");
           dialog.setMessage("Voer wachtwoord in voor: " + SSID + " netwerk");

           final EditText editText = new EditText(this);
           editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
           LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
           editText.setLayoutParams(lp);
           dialog.setView(editText);

           dialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
               @Override
               public void onClick(DialogInterface dialog, int which) {
                   final String netid = Integer.toString(wifi.getConnectionInfo().getNetworkId());
                   password[0] = editText.getText().toString();
                   wifiConfiguration.SSID = "\"" + getString(R.string.accesspoint) + "\"";
                   wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                   final int res = wifi.addNetwork(wifiConfiguration);
                   boolean b = wifi.enableNetwork(res, true);
                   wifi.setWifiEnabled(true);

                   boolean changeHappen = wifi.saveConfiguration();

                   if (res != -1 && changeHappen) {
                       // .connectedSsidName = networkSSID;
                   } else {
                       // Log.d(TAG, "*** Change NOT happen");
                   }
                   wifi.setWifiEnabled(true);
                   // Verstuur

                   Thread t = new Thread() {
                       @Override
                       public void run() {
                           try {
                               //check if connected!
                               while (!isConnected(MainAltijdThuis.this)) {
                                   //Wait to connect
                                   Thread.sleep(1000);
                               }

                               new SetWifiAP().execute("192.168.4.1", SSID, editText.getText().toString(), netid);
                           } catch (Exception e) {
                           }
                       }
                   };
                   t.start();

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

       @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        isReceiverRegistered = false;
           unregisterReceiver(wifiReciever);
        super.onPause();
    }

       protected void onResume() {
           registerReceiver(wifiReciever, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
           super.onResume();
       }

    private void registerReceiver(){
        if(!isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                    new IntentFilter(QuickstartPreferences.REGISTRATION_COMPLETE));
            isReceiverRegistered = true;
        }
    }
    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    public void onButtonAltijdThuisClick( View v) {

        SharedPreferences sharedPreferences =
                getDefaultSharedPreferences(this);
        String host = sharedPreferences.getString(QuickstartPreferences.ALTIJDTHUIS_HOST,null);
        int port = sharedPreferences.getInt(QuickstartPreferences.ALTIJDTHUIS_PORT,8080);

        String url ="http:/"+host+":"+String.valueOf(port)+"/altijdthuis/OpenAltijdThuis.php";
        new OpenAltijdThuis().execute(url);
    }

       @Override
       public void onListFragmentInteraction(PackageItem item) {

       }

       public static boolean isConnected(Context context) {
           ConnectivityManager connectivityManager = (ConnectivityManager)
                   context.getSystemService(Context.CONNECTIVITY_SERVICE);
           NetworkInfo networkInfo = null;
           if (connectivityManager != null) {
               networkInfo = connectivityManager.getActiveNetworkInfo();
           }

           return networkInfo != null && networkInfo.getState() == NetworkInfo.State.CONNECTED;
       }

       private class OpenAltijdThuis extends AsyncTask<String, Void, String> {

           @Override
           protected String doInBackground(String... strings) {
               HttpURLConnection connection = null;

               String result;
               BufferedReader reader;
               try {
                   URL url = new URL(strings[0]);
                   connection = (HttpURLConnection) url.openConnection();
                   connection.connect();
                   InputStream stream = connection.getInputStream();

                   reader = new BufferedReader( new InputStreamReader(stream));
                   StringBuilder buffer = new StringBuilder();
                   String line;
                   while ( (line = reader.readLine()) != null )
                   {
                       buffer.append(line);
                   }
                 //  Toast.makeText(getParent().getBaseContext(), "Geopened.", Toast.LENGTH_SHORT).show();
               } catch (MalformedURLException e) {
                   e.printStackTrace();
                   result = "Error_URL";
                   return result;
               } catch (IOException e) {
                   e.printStackTrace();
                   result = "Error_SERVER";
                   return result;
               } finally {
                   if (connection != null) {
                       connection.disconnect();
                   }
                   result = "SUCCES";
               }

               return result;
           }

           protected void onPostExecute(String result) {
               //Print Toast or open dialog
               if (result.equals("SUCCES")) {
                Toast.makeText(getApplicationContext(), "Geopened.", Toast.LENGTH_SHORT).show();
               }
               if (result.equals("Error_URL") || result.equals("Error_SERVER")) {
                   Toast.makeText(getApplicationContext(), "Altijdthuisbox niet bereikbaar.", Toast.LENGTH_SHORT).show();
               }
           }

       }

       public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

       public boolean askLocationPermission() {
           // Should we show an explanation?
           if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                   android.Manifest.permission.ACCESS_COARSE_LOCATION)) {

               // Show an explanation to the user *asynchronously* -- don't block
               // this thread waiting for the user's response! After the user
               // sees the explanation, try again to request the permission.
               new AlertDialog.Builder(this)
                       .setTitle(R.string.title_location_permission)
                       .setMessage(R.string.text_location_permission)
                       .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface dialogInterface, int i) {
                               //Prompt the user once explanation has been shown
                               ActivityCompat.requestPermissions(MainAltijdThuis.this,
                                       new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                                       MY_PERMISSIONS_REQUEST_LOCATION);
                           }
                       })
                       .create()
                       .show();


           } else {
               // No explanation needed, we can request the permission.
               ActivityCompat.requestPermissions(this,
                       new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},
                       MY_PERMISSIONS_REQUEST_LOCATION);
           }
           return false;
       }

       @Override
       public void onRequestPermissionsResult(int requestCode,
                                              @NonNull String permissions[], @NonNull int[] grantResults) {
           switch (requestCode) {
               case MY_PERMISSIONS_REQUEST_LOCATION: {
                   // If request is cancelled, the result arrays are empty.
                   if (grantResults.length > 0
                           && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                       // permission was granted, yay! Do the
                       // location-related task you need to do.
                       if (ContextCompat.checkSelfPermission(this,
                               Manifest.permission.ACCESS_COARSE_LOCATION)
                               == PackageManager.PERMISSION_GRANTED) {

                           //Scan het netwerk!
                           if (!wifi.isWifiEnabled()) {
                               Toast.makeText(getApplicationContext(), "wifi is disabled. making it enabled", Toast.LENGTH_LONG).show();
                               wifi.setWifiEnabled(true);
                           }
                           wifi.startScan();
                       }

                   }
               }
           }
       }

       private class SetWifiAP extends AsyncTask<String, String, String> {

           @Override
           protected String doInBackground(String... strings) {
               HttpURLConnection connection = null;

               String result = null;
               BufferedReader reader;
               try {
                   URL url = new URL("http://" + strings[0] + "/wifisave?s=" + strings[1] + "&p=" + strings[2]);

                   Log.d("URL AP", url.toString());
                   connection = (HttpURLConnection) url.openConnection();
                   connection.connect();
                   InputStream stream = connection.getInputStream();

                   reader = new BufferedReader(new InputStreamReader(stream));
                   StringBuilder buffer = new StringBuilder();
                   String line;
                   while ((line = reader.readLine()) != null) {
                       buffer.append(line);
                   }
               } catch (MalformedURLException e) {
                   e.printStackTrace();
                   return e.getMessage();
               } catch (IOException e) {
                   e.printStackTrace();
                   return e.getMessage();
               } finally {
                   try {
                       assert connection != null;
                       connection.disconnect();
                       wifi.disconnect();
                       wifi.removeNetwork(wifi.getConnectionInfo().getNetworkId());
                       wifi.saveConfiguration();
                       Integer netid = Integer.valueOf(strings[3]);
                       wifi.enableNetwork(netid, false);
                       wifi.reconnect();
                       wifi.startScan();
                   } catch (Exception e) {
                       e.printStackTrace();
                   }
               }
               result = "Connected";
               return result;
           }

           protected void onPostExecute(String result) {
               //Print Toast or open dialog
               if (result.equals("SUCCES")) {
                   Toast.makeText(getApplicationContext(), "Geopened.", Toast.LENGTH_SHORT).show();
               }
               if (result.equals("Error_URL") || result.equals("Error_SERVER")) {
                   Toast.makeText(getApplicationContext(), "Altijdthuisbox niet bereikbaar.", Toast.LENGTH_SHORT).show();
               }
           }

       }
   }
