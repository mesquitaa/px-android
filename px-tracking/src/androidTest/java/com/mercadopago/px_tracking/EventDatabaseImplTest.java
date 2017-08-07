package com.mercadopago.px_tracking;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.mercadopago.px_tracking.mocks.EventMock;
import com.mercadopago.px_tracking.model.Event;
import com.mercadopago.px_tracking.model.ScreenViewEvent;
import com.mercadopago.px_tracking.strategies.EventsDatabase;
import com.mercadopago.px_tracking.strategies.EventsDatabaseImpl;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

/**
 * Created by marlanti on 8/2/17.
 */

@RunWith(AndroidJUnit4.class)
public class EventDatabaseImplTest {

    @Test
    public void testTrackNEventStorage(){
        MockedMPTrackingContext trackingContext = new MockedMPTrackingContext();

        int N = 5;
        int storedTracksAmount = trackingContext.getBatchSize();

        ScreenViewEvent event = EventMock.getScreenViewEvent();

        for(int i=0;i<N;i++){
            trackingContext.trackEvent(event);
        }

        int newTracksAmount = trackingContext.getBatchSize();

        assertEquals(newTracksAmount,storedTracksAmount+N);
    }

    @Test
    public void testClean(){

        /*MockedMPTrackingContext trackingContext = new MockedMPTrackingContext();

        int N = 5;

        ScreenViewEvent event = EventMock.getScreenViewEvent();

        for(int i=0;i<N;i++){
            trackingContext.trackEvent(event);
        }

        int storedTracksAmount = trackingContext.getBatchSize();

        //FIXME hay algo mal en la consulta de EventsDatabaseImpl.clearExpiredTracks()
        trackingContext.clearExpiredTracks();

        int clearedTracksAmount = trackingContext.getBatchSize();

        assertEquals(clearedTracksAmount-N,storedTracksAmount);*/

    }

    private class MockedMPTrackingContext{
        private EventsDatabase database;

        MockedMPTrackingContext(){
            Context appContext = InstrumentationRegistry.getTargetContext();
            database = new EventsDatabaseImpl(appContext);
        }


        public void trackEvent(Event event) {
            database.persist(event);
        }

        public void clearExpiredTracks() {
            database.clearExpiredTracks();
        }

        public Integer getBatchSize(){
            return database.getBatchSize();
        }

    }

}
