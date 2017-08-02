package com.mercadopago.px_tracking.strategies;

import android.database.sqlite.SQLiteDatabase;

import com.mercadopago.px_tracking.model.Event;
import com.mercadopago.px_tracking.model.EventTrackIntent;

import java.util.List;

public interface EventsDatabase {

    void persist(Event event);

    void persist(List<Event> batch);

    Integer getBatchSize();

    List<Event> retrieveBatch();

    void clearExpiredTracks();

}
