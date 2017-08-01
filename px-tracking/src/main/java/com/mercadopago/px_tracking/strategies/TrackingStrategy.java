package com.mercadopago.px_tracking.strategies;

import android.content.Context;
import android.os.Build;

import com.mercadopago.px_tracking.model.AppInformation;
import com.mercadopago.px_tracking.model.DeviceInfo;
import com.mercadopago.px_tracking.model.Event;
import com.mercadopago.px_tracking.model.EventTrackIntent;

public abstract class TrackingStrategy {

    private String clientId;
    private AppInformation appInformation;
    private DeviceInfo deviceInfo;

    public abstract void trackEvent(Event event, Context context);

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setAppInformation(AppInformation appInformation) {
        this.appInformation = appInformation;
    }

    public void setDeviceInfo(DeviceInfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getClientId() {
        return clientId;
    }

    public AppInformation getAppInformation() {
        return appInformation;
    }

    public DeviceInfo getDeviceInfo() {
        return deviceInfo;
    }
}
