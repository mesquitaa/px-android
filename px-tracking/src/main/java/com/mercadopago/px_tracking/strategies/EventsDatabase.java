package com.mercadopago.px_tracking.strategies;

import android.database.sqlite.SQLiteDatabase;

import com.mercadopago.px_tracking.model.Event;
import com.mercadopago.px_tracking.model.EventTrackIntent;

import java.util.List;

public interface EventsDatabase {

    void addTrack(Event event);

    Integer getBatchSize();

    List<Event> retrieveBatch();

    void clearExpiredTracks();

    void addTracks(List<Event> batch);
}
