package com.mercadopago.px_tracking.strategies;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mercadopago.px_tracking.model.AppInformation;
import com.mercadopago.px_tracking.model.DeviceInfo;
import com.mercadopago.px_tracking.model.Event;
import com.mercadopago.px_tracking.model.EventTrackIntent;
import com.mercadopago.px_tracking.model.ScreenViewEvent;
import com.mercadopago.px_tracking.utils.JsonConverter;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class EventsDatabaseImpl extends SQLiteOpenHelper implements EventsDatabase {

    private static final String DATABASE_NAME = "mercadopago-sdk.db";
    private static final String TABLE_NAME = "events";
    private static final String ID = "_id";
    private static final String TRACK_JSON = "track";
    private static final String TIMESTAMP = "timestamp";
    private static final String MAX_DAYS = "45";
    private static final int MAX_TRACKS = 10;
    private static final int DATABASE_VERSION = 1;

    private Integer batchSizeCache = 0;

    public EventsDatabaseImpl(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE_NAME + "(" +
                ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                TIMESTAMP + " TEXT, " +
                TRACK_JSON + " TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + TABLE_NAME);
        onCreate(db);
    }

    @Override
    public void persist(Event event) {
        ContentValues values = new ContentValues();
        putEventInfoIntoValues(values, event);
        persistData(values);
    }


    @Override
    public void persist(List<Event> batch) {
        for (Event event : batch) {
            persist(event);
        }
    }

    private void putEventInfoIntoValues(ContentValues values, Event event) {
        values.put(TRACK_JSON, JsonConverter.getInstance().toJson(event));
        values.put(TIMESTAMP, event.getTimestamp().toString());
    }

    private void persistData(ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        Long rowId = db.insert(TABLE_NAME, null, values);

        if (rowId != -1) {
            batchSizeCache++;
        }
        //TODO sacar
        Long count = DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM " + TABLE_NAME, null);
        db.close();
    }

    @Override
    public Integer getBatchSize() {
        if (batchSizeCache == 0) {
            SQLiteDatabase db = getWritableDatabase();
            Long count = DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM " + TABLE_NAME, null);
            db.close();
            batchSizeCache = count.intValue();
        }
        return batchSizeCache;
    }

    @Override
    public void clearExpiredTracks() {
        SQLiteDatabase db = getWritableDatabase();
        int count = db.delete(TABLE_NAME, "'EXTRACT(DAY FROM TIMESTAMP " + TIMESTAMP + ")'" + "- 'EXTRACT(DAY FROM TIMESTAMP " + new String(new Timestamp(System.currentTimeMillis()).toString()) + ")'" + " > " + MAX_DAYS, null);
        batchSizeCache = batchSizeCache - count;
        //TODO sacar
        Long count2 = DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM " + TABLE_NAME, null);
        db.close();

    }

    @Override
    public List<Event> retrieveBatch() {

        clearExpiredTracks();

        SQLiteDatabase db = getReadableDatabase();
        String[] columns = {ID, TRACK_JSON};

        Cursor cursor = db.query(false, TABLE_NAME, columns, null, null, null, null, "_id desc", null);

        String trackJsons;
        List<Event> events = new ArrayList<>();

        Long count = DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM " + TABLE_NAME, null);
        int retrievedTracksCount = 0;
        while (cursor.moveToNext() && retrievedTracksCount < MAX_TRACKS) {
            int id = cursor.getInt(0);
            trackJsons = cursor.getString(1);
            //TODO make an EventFactory for different event types.
            Event event = JsonConverter.getInstance().fromJson(trackJsons, ScreenViewEvent.class);
            events.add(event);
            deleteRow(db, id);
            count = DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM " + TABLE_NAME, null);
            retrievedTracksCount++;
        }
        cursor.close();
        db.close();

        return events;
    }

    private void deleteRow(SQLiteDatabase db, int id) {
        int rowsAffected = db.delete(TABLE_NAME, ID + "=" + id, null);
        batchSizeCache = batchSizeCache - rowsAffected;
        //TODO sacar
        Long count = DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM " + TABLE_NAME, null);
    }

    private EventTrackIntent compressTrackPayload(List<EventTrackIntent> tracks) {
        if (tracks != null && !tracks.isEmpty()) {
            AppInformation appInformation = tracks.get(0).getApplication();
            DeviceInfo deviceInfo = tracks.get(0).getDevice();
            String clientId = tracks.get(0).getClientId();

            List<Event> events = new ArrayList<>();
            for (EventTrackIntent intent : tracks) {
                events.addAll(intent.getEvents());
            }
            return new EventTrackIntent(clientId, appInformation, deviceInfo, events);
        } else {
            return null;
        }
    }

}
