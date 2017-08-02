package com.mercadopago.px_tracking.strategies;

import android.content.Context;

import com.mercadopago.px_tracking.model.Event;
import com.mercadopago.px_tracking.model.EventTrackIntent;
import com.mercadopago.px_tracking.services.MPTrackingService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BatchTrackingStrategy extends TrackingStrategy {

    private final static int MIN_BATCH_SIZE = 10;

    private final EventsDatabase database;
    private final MPTrackingService trackingService;
    private final ConnectivityChecker connectivityChecker;

    public BatchTrackingStrategy(EventsDatabase database, ConnectivityChecker connectivityChecker, MPTrackingService trackingService) {
        this.database = database;
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
        return isConnectivityOk() && isDataReady();
    }

    private boolean isConnectivityOk() {
        return connectivityChecker.hasWifiConnection();
    }

    private boolean isDataReady() {
        return database.getBatchSize() >= MIN_BATCH_SIZE;
    }

    private void sendTracksBatch(final Context context) {
        final List<Event> batch = database.retrieveBatch();
        EventTrackIntent intent = new EventTrackIntent(getClientId(), getAppInformation(), getDeviceInfo(), batch);
        trackingService.trackEvents(intent, context, new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    database.persist(batch);

                    if (response.code() == 513) {
                        performTrackAttempt(context);
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                database.persist(batch);
            }
        });
    }
}
