package com.marilone.altijdthuis;

import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.marilone.altijdthuis.packages.DeliveredPackages;
import com.marilone.altijdthuis.packages.DeliveredPackages.PackageItem;
import com.marilone.altijdthuis.parcelFragment.OnListFragmentInteractionListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link PackageItem} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 *
 */
class MyparcelRecyclerViewAdapter extends RecyclerView.Adapter<MyparcelRecyclerViewAdapter.ViewHolder> {

    private final List<PackageItem> mValues;
    private final OnListFragmentInteractionListener mListener;


    MyparcelRecyclerViewAdapter(List<PackageItem> items, OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_parcel, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mContentView.setText(mValues.get(position).content);

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    mListener.onListFragmentInteraction(holder.mItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final View mView;
        final TextView mContentView;
        DeliveredPackages.PackageItem mItem;

        ViewHolder(View view) {
            super(view);
            mView = view;
            mContentView = view.findViewById(R.id.content);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }

    void clear() {
        mValues.clear();
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        PackageItem mPackage = mValues.get(position);
        String url = Global.apigiltyURL + "ontvangenpakketten/" + mPackage.getId();
        Log.d("Delete package:", "url: " + url);

        new DeletePackage().execute(url);
        mValues.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, mValues.size());
    }

    private class DeletePackage extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            if (android.os.Debug.isDebuggerConnected())
                android.os.Debug.waitForDebugger();
            HttpURLConnection connection = null;

            String result;
            BufferedReader reader;
            try {
                URL url = new URL(strings[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("DELETE");

                connection.connect();
                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));
                StringBuilder buffer = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
                result = buffer.toString();
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
            }
            return result;
        }

        protected void onPostExecute(String result) {
            //Print Toast or open dialog
            // if (result.equals("Error_URL") || result.equals("Error_SERVER")) {
            //     Toast.makeText(getContext(), "Altijdthuisbox niet bereikbaar.", Toast.LENGTH_SHORT).show();
            // }
        }
    }

}
