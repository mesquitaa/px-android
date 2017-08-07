package com.mercadopago.px_tracking.strategies;

import android.content.Context;

import com.mercadopago.px_tracking.model.Event;
import com.mercadopago.px_tracking.model.EventTrackIntent;

public interface TrackingStrategy {

    void trackEvent(EventTrackIntent eventTrackIntent, Context context);
}
