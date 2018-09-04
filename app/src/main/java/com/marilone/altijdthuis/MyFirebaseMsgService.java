package com.marilone.altijdthuis;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

//import com.firebase.jobdispatcher.FirebaseJobDispatcher;
//import com.firebase.jobdispatcher.GooglePlayDriver;
//import com.firebase.jobdispatcher.Job;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

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
import java.util.Map;

public class MyFirebaseMsgService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: " + remoteMessage.getFrom());
        Map<String, String> ja = remoteMessage.getData();
        sendNotification("Hoi");

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleNow();
        }
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
        }
    }


    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(token);
    }

    /**
     * Schedule a job using FirebaseJobDispatcher.
     */
    /*
    private void scheduleJob() {
        // [START dispatch_job]
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));
        Job myJob = dispatcher.newJobBuilder()
                .setService(MyJobService.class)
                .setTag("my-job-tag")
                .build();
        dispatcher.schedule(myJob);
        // [END dispatch_job]
    }
*/

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private void handleNow() {
        Log.d(TAG, "Short lived task is done.");
    }

    private void sendRegistrationToServer(String token) {
        // Add custom implementation, as needed.
        JSONObject oRegistration = new JSONObject();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        String altijdthuisid = sharedPreferences.getString("altijdthuisid",null);
        String postcode = sharedPreferences.getString("postcode",null);
        String huisnummer = sharedPreferences.getString("huisnummer",null);
        String emailadres = sharedPreferences.getString("emailadres",null);

        Boolean Firsttime=false;

        String host = sharedPreferences.getString(QuickstartPreferences.ALTIJDTHUIS_HOST, null);
        int port = sharedPreferences.getInt(QuickstartPreferences.ALTIJDTHUIS_PORT, 8080);

        // Naar apigility!
        try {
            if (altijdthuisid == null ||
                    altijdthuisid.contains("Error")) {
                Firsttime = true;
                //String url ="http:/"+host+":"+String.valueOf(port)+"/altijdthuis/OpenAltijdThuis.php";
                String url = "http:/" + host + ":" + String.valueOf(port) + "/GetAltijdThuisID";

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
        String urlpath = Global.apigiltyURL + "registratie";
        if (!Firsttime  ) urlpath += "/" + altijdthuisid;

        Log.d("Registreer api:", "url: "+urlpath);

        // prepare the Request
        HttpURLConnection connection = null;
        try {
            URL url=new URL(urlpath);
            connection = (HttpURLConnection) url.openConnection();
            // Bestaande registraties overschrijven.

            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");

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
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                Log.d("registratie geupdated:", stringBuilder.toString());
            } else {
                Log.e("response not ok", connection.getResponseMessage() + connection.getErrorStream());
            }
        } catch (Exception exception){
            Log.e("apigily fault", exception.toString());
        } finally {
            if (connection != null){
                connection.disconnect();
            }
        }
    }

    private String GetAltijdThuisID(String altijdthuis_url ) {

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
            while ( (line = reader.readLine()) != null ) {
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
        } catch (JSONException e) {
            e.printStackTrace();
        }finally {
            try {
                assert connection != null;
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private void sendNotification(String messageBody) {
        Intent intent = new Intent(this, MainAltijdThuis.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.mipmap.black_white_metro_box_icon)
                        .setContentTitle("Altijdthuis")
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        if (notificationManager != null) {
            notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
        }
    }
}