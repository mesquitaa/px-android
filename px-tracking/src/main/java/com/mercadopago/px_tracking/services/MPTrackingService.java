package com.mercadopago.px_tracking.services;

import android.content.Context;

import com.mercadopago.px_tracking.model.Event;
import com.mercadopago.px_tracking.model.EventTrackIntent;
import com.mercadopago.px_tracking.model.PaymentIntent;
import com.mercadopago.px_tracking.model.TrackingIntent;

import retrofit2.Call;
import retrofit2.Callback;

/**
 * Created by vaserber on 7/3/17.
 */

public interface MPTrackingService {

    void trackToken(TrackingIntent trackingIntent, Context context);
    void trackPaymentId(PaymentIntent paymentIntent, Context context);
    void trackEvents(EventTrackIntent eventTrackIntent, Context context);
    void trackEvents(EventTrackIntent eventTrackIntent, Context context, Callback<Void> callback);
}
