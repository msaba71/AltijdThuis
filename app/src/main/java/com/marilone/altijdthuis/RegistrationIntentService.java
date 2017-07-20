package com.marilone.altijdthuis;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import org.json.JSONException;
import org.json.JSONObject;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class RegistrationIntentService extends IntentService {

    private static final String TAG = "RegIntentService";
    private static final String[] TOPICS = {"global"};
    private String Address;
    private String Port;

    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            Bundle bundle = intent.getExtras();
            if(bundle.getString("Address")!= null)
            {
                 Address = bundle.getString("Address");
            }
            if( bundle.getInt("Port") != 0 )
            {
                Port = String.valueOf(bundle.getInt("Port"));
            }

            // [START register_for_gcm]
            // Initially this call goes out to the network to retrieve the token, subsequent calls
            // are local.
            // R.string.gcm_defaultSenderId (the Sender ID) is typically derived from google-services.json.
            // See https://developers.google.com/cloud-messaging/android/start for details on this file.
            // [START get_token]
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            // [END get_token]
            Log.i(TAG, "GCM Registration Token: " + token);

            sendRegistrationToServer(token);

            // Subscribe to topic channels
            subscribeTopics(token);

            // You should store a boolean that indicates whether the generated token has been
            // sent to your server. If the boolean is false, send the token to your server,
            // otherwise your server should have already received the token.
            sharedPreferences.edit().putBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, true).apply();
            // [END register_for_gcm]

        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
            sharedPreferences.edit().putBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false).apply();
        }
        // Notify UI that registration has completed, so the progress indicator can be hidden.
        Intent registrationComplete = new Intent(QuickstartPreferences.REGISTRATION_COMPLETE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    /**
     * Persist registration to third-party servers.
     *
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        // Add custom implementation, as needed.
        JSONObject oRegistration = new JSONObject();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        String altijdthuisid = sharedPreferences.getString("altijdthuisid",null);
        String postcode = sharedPreferences.getString("postcode",null);
        String huisnummer = sharedPreferences.getString("huisnummer",null);
        String emailadres = sharedPreferences.getString("emailadres",null);

        Boolean Firsttime=false;

        // Naar apigility!
        try {
            if ( altijdthuisid == null ) {
                Firsttime = true;
                String url = "http:/" + Address + ":" + String.valueOf(Port) + "/altijdthuis/GetAltijdThuisID.php";
                altijdthuisid = GetAltijdThuisID(url);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("altijdthuisid", altijdthuisid);
                editor.apply();
            }
            oRegistration.put("TelefoonID", token);
            oRegistration.put("AltijdthuisID", altijdthuisid);
            oRegistration.put("Postcode", postcode);
            oRegistration.put("Huisnummer", huisnummer);
            oRegistration.put("Email", emailadres);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String urlpath = Global.apigiltyURL + "/registratie";
        if (!Firsttime  ) urlpath += "/" + altijdthuisid;

        Log.d("Registreer api:", "url: "+urlpath);

        // prepare the Request
        HttpURLConnection connection = null;
        try {
            URL url=new URL(urlpath);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");

            // Bestaande registraties overschrijven.
            if ( !Firsttime )
            {
                connection.setRequestMethod("PUT");
            }
            else
            {
                connection.setRequestMethod("POST");
            }
            OutputStreamWriter streamWriter = new OutputStreamWriter(connection.getOutputStream());
            streamWriter.write(oRegistration.toString());
            streamWriter.flush();
            StringBuilder stringBuilder = new StringBuilder();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED){
                InputStreamReader streamReader = new InputStreamReader(connection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(streamReader);
                String response;
                while ((response = bufferedReader.readLine()) != null) {
                    stringBuilder.append(response).append("\n");
                }
                bufferedReader.close();
                if ( Firsttime ) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("altijdthuisid", altijdthuisid);
                    editor.apply();
                }
                Log.d("registratie aangemaakt:", stringBuilder.toString());
            }
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
            {
                Log.d("registratie geupdated:", stringBuilder.toString());
            }
            else
            {
                Log.e("response not ok", connection.getResponseMessage() + connection.getResponseCode());
            }
        } catch (Exception exception){
            Log.e("apigily fault", exception.toString());
        } finally {
            if (connection != null){
                connection.disconnect();
            }
        }
    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    // [START subscribe_topics]
    private void subscribeTopics(String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (String topic : TOPICS) {
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }
    // [END subscribe_topics]

    private String GetAltijdThuisID( String altijdthuis_url ) {

        HttpURLConnection connection = null;

        String result = null;
        BufferedReader reader;
        try {
            URL url = new URL(altijdthuis_url);
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

            JSONObject altijdthuis = new JSONObject( buffer.toString() );
            result = altijdthuis.getString("AltijdthuisID");

            //  Toast.makeText(getParent().getBaseContext(), "Geopened.", Toast.LENGTH_SHORT).show();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            result = "Error_URL";
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            result = "Error_SERVER";
            return result;
        }
        catch (JSONException e) {
            e.printStackTrace();
        }finally {
            try {
                assert connection != null;
                connection.disconnect();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return result;
    }
}
