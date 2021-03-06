package com.marilone.altijdthuis;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import static android.net.nsd.NsdManager.DiscoveryListener;
import static android.net.nsd.NsdManager.PROTOCOL_DNS_SD;
import static android.net.nsd.NsdManager.RegistrationListener;
import static android.net.nsd.NsdManager.ResolveListener;
import static android.preference.PreferenceManager.getDefaultSharedPreferences;
import static com.marilone.altijdthuis.R.color.colorOffline;
import static com.marilone.altijdthuis.R.color.colorOnline;

class NsdHelper {
    final private Context mContext;
    final private NsdManager mNsdManager;
    private ResolveListener mResolveListener;
    private DiscoveryListener mDiscoveryListener;
    private RegistrationListener mRegistrationListener;
    private static final String SERVICE_TYPE = "_http._tcp.";
    private static final String TAG = "NSD";
    final private String mServiceName = "altijdthuis";
    private NsdServiceInfo mService;
    private boolean bFound = false;

    private final FloatingActionButton mOopenbutton;

    NsdHelper(Context context, final FloatingActionButton openbutton) {
        mContext = context;
        mOopenbutton = openbutton;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    void initializeNsd() {
        initializeResolveListener();
        discoverServices();

    }

    public boolean isFound() {
        return bFound;
    }

    private void initializeDiscoveryListener() {
        mDiscoveryListener = new DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }
            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(TAG, "Service discovery success" + service);
                if (!service.getServiceType().equals(SERVICE_TYPE)) {
                    Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(mServiceName)) {
                    bFound = true;
                    Log.d(TAG, "Same machine: " + mServiceName);
                    mNsdManager.resolveService(service, mResolveListener);
                }
            }
            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "service lost" + service);
                if (mService == service) {
                    mService = null;
                }
                setButtonOffline();
            }
            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            }
            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            }
        };
    }

    private void initializeResolveListener() {
        mResolveListener = new ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed" + errorCode);
            }
            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.d(TAG, "Resolve Succeeded. " + serviceInfo);
                if (serviceInfo.getServiceName().equals(mServiceName)) {
                    Log.d(TAG, "Same IP.");
                    mService = serviceInfo;
                    SharedPreferences sharedPreferences = getDefaultSharedPreferences(mContext);
                    sharedPreferences.edit().putString(QuickstartPreferences.ALTIJDTHUIS_HOST, mService.getHost().toString()).apply();
                    sharedPreferences.edit().putInt(QuickstartPreferences.ALTIJDTHUIS_PORT, mService.getPort() ).apply();
                    setButtonOnline();
                    bFound = true;
                }
            }
        };
    }

    private void discoverServices() {
        stopDiscovery();  // Cancel any existing discovery request
        initializeDiscoveryListener();
        mNsdManager.discoverServices(
                SERVICE_TYPE, PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    private void stopDiscovery() {
        if (mDiscoveryListener != null) {
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
            mDiscoveryListener = null;
        }
    }

    public void tearDown() {
        if (mRegistrationListener != null) {
            mNsdManager.unregisterService(mRegistrationListener);
            mRegistrationListener = null;
        }
    }

    private void setButtonOnline() {
        Handler refresh = new Handler(Looper.getMainLooper());
        refresh.post(new Runnable() {
            @Override
            public void run() {
                mOopenbutton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext, colorOnline)));
            }
        });
    }

    private void setButtonOffline() {
        Handler refresh = new Handler(Looper.getMainLooper());
        refresh.post(new Runnable() {
            @Override
            public void run() {
                mOopenbutton.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mContext, colorOffline)));
            }
        });
    }


}
