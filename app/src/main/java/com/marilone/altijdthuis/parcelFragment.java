package com.marilone.altijdthuis;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.marilone.altijdthuis.packages.DeliveredPackages;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    public MyparcelRecyclerViewAdapter mAdapter;

    private JSONArray mPackages;
    private DeliveredPackages mDeliveredPackages;

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
    public interface OnListFragmentInteractionListener {

        void onListFragmentInteraction(DeliveredPackages.PackageItem item);
    }

    public void RetrieveDeliveredPackages() {

        final Context context = getContext();
        SharedPreferences sharedPreferences =
                getDefaultSharedPreferences(context);
        String host = sharedPreferences.getString(QuickstartPreferences.ALTIJDTHUIS_HOST,null);
        int port = sharedPreferences.getInt(QuickstartPreferences.ALTIJDTHUIS_PORT,8080);

        RequestQueue queue = Volley.newRequestQueue(context);
        if ( host == null )
        {
             Toast.makeText(context.getApplicationContext(), "Altijdthuis niet gevonden.", Toast.LENGTH_LONG).show();
             return;
        }

        String url ="http:/"+host+":"+String.valueOf(port)+"/altijdthuis//GetDeliveredPackages.php";
        //String url ="http://192.168.1.106:8080/altijdthuis//GetDeliveredPackages.php";
        Log.d("GetPackages:", "url: "+url);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        try {

                            JSONObject mResponse = new JSONObject(response);
                            mPackages = (JSONArray) mResponse.get("response");
                            mDeliveredPackages = new DeliveredPackages(mPackages);
                            mAdapter.notifyDataSetChanged();
                            swipeContainer.setRefreshing(false);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Log.d("GetPackages:", "Response is: "+ response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(context.getApplicationContext(), "Altijdthuis is niet aanwezig.", Toast.LENGTH_LONG).show();
                Log.d("GetPackages:", "Altijdthuis is niet aanwezig." + error.getMessage());
                swipeContainer.setRefreshing(false);
                // mTextView.setText("That didn't work!");
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
}
