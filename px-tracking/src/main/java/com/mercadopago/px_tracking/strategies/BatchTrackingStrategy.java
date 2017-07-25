package com.mercadopago.px_tracking.strategies;

import android.content.Context;

import com.mercadopago.px_tracking.model.EventTrackIntent;
import com.mercadopago.px_tracking.services.MPTrackingService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BatchTrackingStrategy implements TrackingStrategy {

    private final static int MIN_BATCH_SIZE = 5;

    private final EventsDatabase database;
    private final MPTrackingService trackingService;
    private final ConnectivityChecker connectivityChecker;

    public BatchTrackingStrategy(EventsDatabase database, ConnectivityChecker connectivityChecker, MPTrackingService trackingService) {
        this.database = database;
        this.trackingService = trackingService;
        this.connectivityChecker = connectivityChecker;
    }

    @Override
    public void trackEvents(EventTrackIntent eventTrackIntent, Context context) {
        database.addTrack(eventTrackIntent);
        performTrackAttempt(context);
    }

    private void performTrackAttempt(Context context) {
        if (shouldSendBatch()) {
            sendTracksBatch(context);
        }
    }

    private boolean shouldSendBatch() {
        return isConnectivityOk() && isDataReady();
    }

    private boolean isConnectivityOk() {
        return connectivityChecker.hasConnection();
    }

    private boolean isDataReady() {
        return database.batchSize() >= MIN_BATCH_SIZE;
    }

    private void sendTracksBatch(final Context context) {
        EventTrackIntent batch = database.retrieveBatch();
        trackingService.trackEvent(batch, context, new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    performTrackAttempt(context);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                database.rollback();
            }
        });
    }
}
