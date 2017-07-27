package com.mercadopago.px_tracking.strategies;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

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
    public void addTrack(EventTrackIntent eventTrackIntent) {
        ContentValues values = new ContentValues();
        //FIXME ACÁ SIEMPRE LLEGA CON 1 EVENTO, ENTONCES NO VA A RECIBIR MÁS UN EventTrackIntent
        values.put(TRACK_JSON, JsonConverter.getInstance().toJson(eventTrackIntent.getEvents().get(0)));
        values.put(TIMESTAMP, new Timestamp(System.currentTimeMillis()).toString());
        SQLiteDatabase db = getWritableDatabase();
        db.insert(TABLE_NAME, null, values);
        db.close();
        batchSizeCache++;
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
        db.execSQL("delete from " + TABLE_NAME + " where " + "'EXTRACT(DAY FROM TIMESTAMP " + TIMESTAMP + ")'" + "- 'EXTRACT(DAY FROM TIMESTAMP " + new String(new Timestamp(System.currentTimeMillis()).toString()) + ")'" + " > " + MAX_DAYS);
        db.close();
    }

    @Override
    public EventTrackIntent retrieveBatch() {

        clearExpiredTracks();

        SQLiteDatabase db = getReadableDatabase();
//        db.beginTransaction();
        String[] columns = {ID, TRACK_JSON};

        Cursor cursor = db.query(false, TABLE_NAME, columns, null, null, null, null, "_id desc", null);

        String trackJsons;
        List<Event> events = new ArrayList<>();

        while (cursor.moveToNext() && batchSizeCache < MAX_TRACKS) {
            int id = cursor.getInt(0);
            trackJsons = cursor.getString(1);
            Event event = JsonConverter.getInstance().fromJson(trackJsons, ScreenViewEvent.class);
            events.add(event);
            deleteRow(db,id);
        }

        cursor.close();
        db.close();

        //FIXME con el nuevo EventTrackIntent y ver si sacamos acá la info verdadera o donde.
        EventTrackIntent intent = new EventTrackIntent("clientId", new AppInformation.Builder().setCheckoutVersion("cho version").setPlatform("platform").setPublicKey("pk").build(), new DeviceInfo.Builder().setModel("").build(), events);

        return intent;
    }

    private void deleteRow(SQLiteDatabase db, int id) {
        db.delete(TABLE_NAME, ID + "=" + id, null);
        batchSizeCache--;
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

    @Override
    public void setTransactionSuccessful() {
        SQLiteDatabase db = getReadableDatabase();
        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }

    @Override
    public void beginTransaction() {
        SQLiteDatabase db = getReadableDatabase();
        db.beginTransaction();
        db.close();
    }

    //Notice, you do not need to explicitly rollback. If you call db.endTransaction() without db.setTransactionSuccessful() it will roll back automatically.
    @Override
    public void setTransactionFailure() {
        SQLiteDatabase db = getReadableDatabase();
        db.endTransaction();
        db.close();
        batchSizeCache++;
    }
}
