package com.mercadopago.px_tracking.strategies;

import android.database.sqlite.SQLiteDatabase;

import com.mercadopago.px_tracking.model.EventTrackIntent;

public interface EventsDatabase {

    void addTrack(EventTrackIntent eventTrackIntent);

    Integer getBatchSize();

    EventTrackIntent retrieveBatch();

    void clearExpiredTracks();

    void beginTransaction();

    void setTransactionSuccessful();

    void setTransactionFailure();
}
