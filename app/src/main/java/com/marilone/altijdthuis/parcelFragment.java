package com.marilone.altijdthuis;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.marilone.altijdthuis.packages.DeliveredPackages;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class parcelFragment extends Fragment {

    private static final String ARG_COLUMN_COUNT = "column-count";

    private int mColumnCount = 1;
    private OnListFragmentInteractionListener mListener;

    private SwipeRefreshLayout swipeContainer;

    private MyparcelRecyclerViewAdapter mAdapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public parcelFragment() {
    }


    public static parcelFragment newInstance(int columnCount) {
        parcelFragment fragment = new parcelFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_parcel_list, container, false);
        // Set the adapter
        Context context = view.getContext();
        RecyclerView recyclerView =  (RecyclerView) view.findViewById(R.id.list);
        if (mColumnCount <= 1) {
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
        }
        mAdapter = new MyparcelRecyclerViewAdapter(DeliveredPackages.ITEMS, mListener);
        recyclerView.setAdapter(mAdapter);

        swipeContainer = (SwipeRefreshLayout) view.findViewById(R.id.swipeContainer);
        // Setup refresh listener which triggers new data loading
        swipeContainer.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                // Your code to refresh the list here.
                // Make sure you call swipeContainer.setRefreshing(false)
                // once the network request has completed successfully.
                mAdapter.clear();
                RetrieveDeliveredPackages();
            }
        });
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.clear();
        RetrieveDeliveredPackages();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    interface OnListFragmentInteractionListener {

        void onListFragmentInteraction(DeliveredPackages.PackageItem item);
    }

    private void RetrieveDeliveredPackages() {


        // String host = sharedPreferences.getString(QuickstartPreferences.ALTIJDTHUIS_HOST,null);
        // int port = sharedPreferences.getInt(QuickstartPreferences.ALTIJDTHUIS_PORT,8080);

        //String url ="http:/"+host+":"+String.valueOf(port)+"/altijdthuis//GetDeliveredPackages.php";
        String url = Global.apigiltyURL + "/getontvangenpakketten";
        Log.d("GetPackages:", "url: "+url);

        new GetDeliveredPackages().execute(url);
    }

    private class GetDeliveredPackages extends AsyncTask<String, String, String>
    {

        @Override
        protected String doInBackground(String... strings) {
            HttpURLConnection connection=null;
            BufferedReader reader=null;

            try {
                URL url = new URL(strings[0]);

                connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestMethod("POST");

                OutputStreamWriter streamWriter = new OutputStreamWriter(connection.getOutputStream());
                JSONObject oRegistration = new JSONObject();
                final Context context = getContext();
                SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);

                String altijdthuisid = sharedPreferences.getString("altijdthuisid", null);
                oRegistration.put("AltijdthuisID", altijdthuisid);

                streamWriter.write(oRegistration.toString());
                streamWriter.flush();
                StringBuilder buffer = new StringBuilder();
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream stream = connection.getInputStream();

                    reader = new BufferedReader(new InputStreamReader(stream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line);
                    }
                }
                return buffer.toString();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                if ( reader != null)
                {
                    try {
                        reader.close();
                    }
                    catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String deliveredpackages) {
            super.onPostExecute(deliveredpackages);
            try {

                if (!Objects.equals(deliveredpackages, "") && deliveredpackages != null) {
                    JSONObject mResponse = new JSONObject(deliveredpackages);
                    JSONArray mPackages = (JSONArray) mResponse.get("response");
                    DeliveredPackages mDeliveredPackages = new DeliveredPackages(mPackages);
                    mAdapter.notifyDataSetChanged();
                    swipeContainer.setRefreshing(false);
                }
                else
                {
                   if ( getContext() != null ) {
                       Toast.makeText(getContext(), "Altijdthuis is niet aanwezig.", Toast.LENGTH_LONG).show();
                   }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
