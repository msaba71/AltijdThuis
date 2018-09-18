package com.marilone.altijdthuis;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

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
import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class MyPreferencesActivity extends AppCompatPreferenceActivity {
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();

    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (!super.onMenuItemSelected(featureId, item)) {
                NavUtils.navigateUpFromSameTask(this);
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
            setHasOptionsMenu(true);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("postcode"));
            bindPreferenceSummaryToValue(findPreference("huisnummer"));
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), MyPreferencesActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onDetach() {
            super.onDetach();
            FirebaseInstanceId.getInstance().getInstanceId()
                    .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                        @Override
                        public void onComplete(@NonNull Task<InstanceIdResult> task) {
                            // Get new Instance ID token
                            String token = task.getResult().getToken();
                            Log.d("preference", token);
                            sendRegistrationToServer(token);
                        }
                    });

        }

        private void sendRegistrationToServer(final String token) {
            // Naar apigility!
            try {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Add custom implementation, as needed.
                            JSONObject oRegistration = new JSONObject();

                            SharedPreferences preferences = getPreferenceManager().getSharedPreferences();

                            String altijdthuisid = preferences.getString("altijdthuisid", null);
                            String postcode = preferences.getString("postcode", null);
                            String huisnummer = preferences.getString("huisnummer", null);
                            String emailadres = preferences.getString("emailadres", null);

                            Boolean Firsttime = false;

                            String host = preferences.getString(QuickstartPreferences.ALTIJDTHUIS_HOST, null);
                            int port = preferences.getInt(QuickstartPreferences.ALTIJDTHUIS_PORT, 80);

                            if (altijdthuisid == null || altijdthuisid.contains("Error")) {

                                Firsttime = true;
                                //String url ="http:/"+host+":"+String.valueOf(port)+"/altijdthuis/OpenAltijdThuis.php";
                                String url = "http:/" + host + ":" + String.valueOf(port) + "/GetAltijdThuisID";

                                altijdthuisid = GetAltijdThuisID(url);
                                SharedPreferences.Editor editor = findPreference("altijdthuisid").getEditor();
                                editor.putString("altijdthuisid", altijdthuisid);
                                editor.apply();
                            }
                            oRegistration.put("TelefoonID", token);
                            oRegistration.put("AltijdthuisID", altijdthuisid);
                            oRegistration.put("Postcode", postcode);
                            oRegistration.put("Huisnummer", huisnummer);
                            oRegistration.put("Email", emailadres);

                            String urlpath = Global.apigiltyURL + "registratie";
                            if (!Firsttime) urlpath += "/" + altijdthuisid;

                            Log.d("Registreer api:", "url: " + urlpath);

                            // prepare the Request
                            HttpURLConnection connection = null;
                            try {
                                URL url = new URL(urlpath);
                                connection = (HttpURLConnection) url.openConnection();
                                // Bestaande registraties overschrijven.

                                if (Firsttime) {
                                    connection.setRequestMethod("POST");
                                } else {
                                    connection.setRequestMethod("PUT");
                                }
                                connection.setDoOutput(true);
                                connection.setDoInput(true);
                                connection.setRequestProperty("Content-Type", "application/json");
                                connection.setRequestProperty("Accept", "application/json");

                                OutputStreamWriter streamWriter = new OutputStreamWriter(connection.getOutputStream());
                                streamWriter.write(oRegistration.toString());
                                streamWriter.flush();
                                StringBuilder stringBuilder = new StringBuilder();
                                if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                                    InputStreamReader streamReader = new InputStreamReader(connection.getInputStream());
                                    BufferedReader bufferedReader = new BufferedReader(streamReader);
                                    String response;
                                    while ((response = bufferedReader.readLine()) != null) {
                                        stringBuilder.append(response).append("\n");
                                    }
                                    bufferedReader.close();
                                    Log.d("registratie aangemaakt:", stringBuilder.toString());
                                }
                                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                    Log.d("registratie geupdated:", stringBuilder.toString());
                                } else {
                                    Log.e("response not ok", connection.getResponseMessage() + connection.getErrorStream());
                                }
                            } catch (Exception exception) {
                                Log.e("apigily fault", exception.toString());
                            } finally {
                                if (connection != null) {
                                    connection.disconnect();
                                }
                            }
                        } catch (Exception e) {
                            Log.e("Altijdthuid", e.getMessage());
                        }
                    }
                });
                thread.start();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        private String GetAltijdThuisID(String altijdthuis_url) {

            HttpURLConnection connection = null;
            String result = null;
            BufferedReader reader;
            try {
                URL url = new URL(altijdthuis_url);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuilder buffer = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }

                JSONObject altijdthuis = new JSONObject(buffer.toString());
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
            } finally {
                try {
                    assert connection != null;
                    connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return result;
        }


    }
 }
