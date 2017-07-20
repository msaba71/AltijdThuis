package com.marilone.altijdthuis;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.nsd.NsdServiceInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
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
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.marilone.altijdthuis.packages.DeliveredPackages.PackageItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;


public class MainAltijdThuis extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, parcelFragment.OnListFragmentInteractionListener
   {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainAltijdThuis";

    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private boolean isReceiverRegistered;

    // Zoek de raspberry
    private NsdHelper mNsdHelper;
    private NsdServiceInfo mServiceInfo;

    private SwipeRefreshLayout swipeContainer;


       @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_altijd_thuis);

        // NSD
           mNsdHelper = new NsdHelper(this, (FloatingActionButton) findViewById(R.id.fab));
        mNsdHelper.initializeNsd();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
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

        if (id == R.id.nav_camera) {
            // Registering BroadcastReceiver
            final Context context = getApplicationContext();
            SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);
            String host = sharedPreferences.getString(QuickstartPreferences.ALTIJDTHUIS_HOST,null);
            int port = sharedPreferences.getInt(QuickstartPreferences.ALTIJDTHUIS_PORT,8080);

            if (checkPlayServices()) {
                // Start IntentService to register this application with GCM.
                Intent intent = new Intent(this, RegistrationIntentService.class);
                if (host!=null) {
                    intent.putExtra("Address", host);
                    intent.putExtra("Port", port);
                }
                startService(intent);
            }

            // Handle the camera action
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        assert drawer != null;
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

   @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        isReceiverRegistered = false;
        super.onPause();
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
               if ( result == "SUCCES") {
                Toast.makeText(getApplicationContext(), "Geopened.", Toast.LENGTH_SHORT).show();
               }
               if ( result == "Error_URL" || result == "Error_SERVER") {
                   Toast.makeText(getApplicationContext(), "Altijdthuisbox niet bereikbaar.", Toast.LENGTH_SHORT).show();
               }
           }

       }
   }
