package com.mercadopago.px_tracking.strategies;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class ConnectivityCheckerImpl implements ConnectivityChecker {
    private Context mContext;

    public ConnectivityCheckerImpl(Context context) {
        this.mContext = context;
    }

    @Override
    public boolean hasConnection() {

        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected()) {
            return true;
        }

        return false;
    }

}
