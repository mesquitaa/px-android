package com.mercadopago.px_tracking.strategies;

import android.content.Context;
import android.util.Log;

import com.mercadopago.px_tracking.model.Event;
import com.mercadopago.px_tracking.model.EventTrackIntent;
import com.mercadopago.px_tracking.services.MPTrackingService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForcedStrategy extends TrackingStrategy {

    private final MPTrackingService trackingService;
    private final ConnectivityChecker connectivityChecker;

    public ForcedStrategy(EventsDatabase database, ConnectivityChecker connectivityChecker, MPTrackingService trackingService) {
        setDatabase(database);
        this.trackingService = trackingService;
        this.connectivityChecker = connectivityChecker;
    }

    @Override
    public void trackEvent(Event event, Context context) {
        performTrackAttempt(context);
    }

    private void performTrackAttempt(Context context) {
        if (shouldSendBatch()) {
            sendTracksBatch(context);
        }
    }

    private boolean shouldSendBatch() {
        return isConnectivityOk() && isDataAvailable();
    }

    private boolean isConnectivityOk() {
        return connectivityChecker.hasConnection();
    }


    private void sendTracksBatch(final Context context) {
        final List<Event> batch = getDatabase().retrieveBatch();
        EventTrackIntent intent = new EventTrackIntent(getClientId(), getAppInformation(), getDeviceInfo(), batch);
        trackingService.trackEvents(intent, context, new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.v("FORCED","200");
                    performTrackAttempt(context);
                } else {
                    getDatabase().returnEvents(batch);

                    if (response.code() == 513) {
                        performTrackAttempt(context);
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                getDatabase().returnEvents(batch);
            }
        });
    }
}
