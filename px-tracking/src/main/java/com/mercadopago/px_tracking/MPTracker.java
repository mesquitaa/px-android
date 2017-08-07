package com.mercadopago.px_tracking;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mercadopago.px_tracking.listeners.TracksListener;
import com.mercadopago.px_tracking.model.ActionEvent;
import com.mercadopago.px_tracking.model.AppInformation;
import com.mercadopago.px_tracking.model.DeviceInfo;
import com.mercadopago.px_tracking.model.Event;
import com.mercadopago.px_tracking.model.PaymentIntent;
import com.mercadopago.px_tracking.model.ScreenViewEvent;
import com.mercadopago.px_tracking.model.TrackingIntent;
import com.mercadopago.px_tracking.strategies.BatchTrackingStrategy;
import com.mercadopago.px_tracking.strategies.ConnectivityCheckerImpl;
import com.mercadopago.px_tracking.strategies.EventsDatabaseImpl;
import com.mercadopago.px_tracking.strategies.ForcedStrategy;
import com.mercadopago.px_tracking.strategies.TrackingStrategy;
import com.mercadopago.px_tracking.services.MPTrackingService;
import com.mercadopago.px_tracking.services.MPTrackingServiceImpl;
import com.mercadopago.px_tracking.utils.JsonConverter;
import com.mercadopago.px_tracking.utils.TrackingUtil;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static android.text.TextUtils.isEmpty;

/**
 * Created by vaserber on 6/5/17.
 */

public class MPTracker {

    private static MPTracker mMPTrackerInstance;
    private EventsDatabaseImpl database;

    private TracksListener mTracksListener;

    private MPTrackingService mMPTrackingService;

    private String mPublicKey;
    private String mSdkVersion;
    private String mSiteId;
    private Context mContext;

    private static final String SDK_PLATFORM = "Android";
    private static final String SDK_TYPE = "native";

    private static final String DEFAULT_SITE = "";
    private static final String DEFAULT_FLAVOUR = "3";

    private Boolean trackerInitialized = false;

    private TrackingStrategy trackingStrategy;
    private Event mEvent;

    protected MPTracker() {
    }

    synchronized public static MPTracker getInstance() {
        if (mMPTrackerInstance == null) {
            mMPTrackerInstance = new MPTracker();
        }
        return mMPTrackerInstance;
    }

    private void initializeMPTrackingService() {
        if (mMPTrackingService == null) {
            mMPTrackingService = new MPTrackingServiceImpl();
        }
    }

    public void setMPTrackingService(MPTrackingService trackingService) {
        mMPTrackingService = trackingService;
    }

    public void setTracksListener(TracksListener tracksListener) {
        this.mTracksListener = tracksListener;
    }

    private void trackScreenLaunchedListener(String screenName) {
        if (this.mTracksListener != null) {
            this.mTracksListener.onScreenLaunched(screenName);
        }
    }

    private void trackEventPerformedListener(Map<String, String> eventMap) {
        if (this.mTracksListener != null) {
            this.mTracksListener.onEventPerformed(eventMap);
        }
    }

    /**
     * @param paymentId The payment id of a payment method off. Cannot be {@code null}.
     * @param typeId    The payment type id. It has to be a card type.
     */
    public PaymentIntent trackPayment(Long paymentId, String typeId) {

        PaymentIntent paymentIntent = null;

        if (trackerInitialized) {

            if (!isCardPaymentType(typeId)) {
                paymentIntent = new PaymentIntent(mPublicKey, paymentId.toString(), DEFAULT_FLAVOUR, SDK_PLATFORM, SDK_TYPE, mSdkVersion, mSiteId);
                initializeMPTrackingService();
                mMPTrackingService.trackPaymentId(paymentIntent, mContext);
            }

        }
        return paymentIntent;
    }

    /**
     * @param token The card token id of a payment. Cannot be {@code null}.
     */
    public TrackingIntent trackToken(String token) {
        TrackingIntent trackingIntent = null;
        if (trackerInitialized && !isEmpty(token)) {
            trackingIntent = new TrackingIntent(mPublicKey, token, DEFAULT_FLAVOUR, SDK_PLATFORM, SDK_TYPE, mSdkVersion, mSiteId);
            initializeMPTrackingService();
            mMPTrackingService.trackToken(trackingIntent, mContext);
        }
        return trackingIntent;
    }

    /**
     * This method tracks a list of events in one request
     *
     * @param clientId       Id that identifies the client that is using the SDK
     * @param appInformation Info about this application and SDK integration
     * @param deviceInfo     Info about the device that is using the app
     * @param event          Event to track
     * @param context        Application context
     */
    public void trackEvent(String clientId, AppInformation appInformation, DeviceInfo deviceInfo, Event event, Context context) {

        initializeMPTrackingService();

        mEvent = event;
        database.persist(event);

        getTrackingStrategy(context, event);

        if (trackingStrategy != null) {
            trackingStrategy.trackEvent(event, context);
            trackingStrategy.setClientId(clientId);
            trackingStrategy.setAppInformation(appInformation);
            trackingStrategy.setDeviceInfo(deviceInfo);
        }

        if (event.getType().equals(Event.TYPE_ACTION)) {
            ActionEvent actionEvent = (ActionEvent) event;
            Map<String, String> eventMap = createEventMap(actionEvent);
            trackEventPerformedListener(eventMap);
        } else if (event.getType().equals(Event.TYPE_SCREEN_VIEW)) {
            ScreenViewEvent screenViewEvent = (ScreenViewEvent) event;
            trackScreenLaunchedListener(screenViewEvent.getScreenName());
        }
    }

    private Map<String, String> createEventMap(ActionEvent actionEvent) {
        Map<String, String> eventMap = new HashMap<>();

        String eventJson = JsonConverter.getInstance().toJson(actionEvent);
        Type type = new TypeToken<Map<String, String>>() {
        }.getType();
        Map<String, String> actionEventDataMap = new Gson().fromJson(eventJson, type);

        eventMap.putAll(actionEventDataMap);

        return eventMap;
    }


    /**
     * @param publicKey  The public key of the merchant. Cannot be {@code null}.
     * @param siteId     The site that comes in the preference. Cannot be {@code null}.
     * @param sdkVersion The Mercado Pago sdk version. Cannot be {@code null}.
     * @param context    Reference to Android Context. Cannot be {@code null}.
     */
    public void initTracker(String publicKey, String siteId, String sdkVersion, Context context) {
        if (!isTrackerInitialized()) {
            if (areInitParametersValid(publicKey, siteId, sdkVersion, context)) {
                trackerInitialized = true;

                this.mPublicKey = publicKey;
                this.mSiteId = siteId;
                this.mSdkVersion = sdkVersion;
                this.mContext = context;
                this.database = new EventsDatabaseImpl(mContext);
            }
        }
    }

    /**
     * @param publicKey  The public key of the merchant. Cannot be {@code null}.
     * @param siteId     The site that comes in the preference. Cannot be {@code null}.
     * @param sdkVersion The Mercado Pago sdk version. Cannot be {@code null}.
     * @param context    Reference to Android Context. Cannot be {@code null}.
     * @return True if all parameters are valid. False if any parameter is invalid
     */
    private boolean areInitParametersValid(String publicKey, String siteId, String sdkVersion, Context context) {
        return !isEmpty(publicKey) && !isEmpty(sdkVersion) && !isEmpty(siteId) && context != null;
    }

    /**
     * Check if MPTracker is initialized
     *
     * @return True if is initialized. False if is not initialized.
     */
    private boolean isTrackerInitialized() {
        return this.mPublicKey != null && this.mSdkVersion != null && this.mSiteId != null && this.mContext != null;
    }

    /**
     * Get the set site
     *
     * @return The site that is set in the first track. if it is null returns an empty string
     */
    private String getSiteId() {
        return mSiteId == null ? DEFAULT_SITE : mSiteId;
    }

    /**
     * Indicates if a payment was done by card or not
     *
     * @param paymentTypeId The payment type id of the payment tracked
     * @return True if it is a card payment. False if not a card payment.
     */
    private Boolean isCardPaymentType(String paymentTypeId) {
        return paymentTypeId.equals("credit_card") || paymentTypeId.equals("debit_card") || paymentTypeId.equals("prepaid_card");
    }

    private TrackingStrategy getTrackingStrategy(Context context, Event event) {

        if (hasBatchStrategyScreenEvent(event)) {
            trackingStrategy = new BatchTrackingStrategy(database, new ConnectivityCheckerImpl(context), mMPTrackingService);
        } else if (hasForcedStrategyScreenEvent(event)) {
            trackingStrategy = new ForcedStrategy(database, new ConnectivityCheckerImpl(context), mMPTrackingService);
        }

        return trackingStrategy;
    }

    private boolean hasForcedStrategyScreenEvent(Event event) {
        boolean hasForcedStrategyScreenEvent = false;
        if (event instanceof ScreenViewEvent) {
            ScreenViewEvent screenViewEvent = (ScreenViewEvent) event;
            if (isForcedStrategyScreenEvent(screenViewEvent)) {
                hasForcedStrategyScreenEvent = true;
            }
        }
        return hasForcedStrategyScreenEvent;
    }

    private boolean hasBatchStrategyScreenEvent(Event event) {
        boolean hasBatchStrategyScreenEvent = false;
        if (event instanceof ScreenViewEvent) {
            ScreenViewEvent screenViewEvent = (ScreenViewEvent) event;

            if (isBatchTrackingStrategyScreenEvent(screenViewEvent)) {
                hasBatchStrategyScreenEvent = true;
            }
        }
        return hasBatchStrategyScreenEvent;
    }

    private boolean isForcedStrategyScreenEvent(ScreenViewEvent screenViewEvent) {
        return isErrorScreen(screenViewEvent.getScreenName()) || isResultScreen(screenViewEvent.getScreenName());
    }

    private boolean isErrorScreen(String name) {
        return name.equals(TrackingUtil.SCREEN_NAME_ERROR);
    }

    private boolean isResultScreen(String name) {
        return name.equals(TrackingUtil.SCREEN_NAME_PAYMENT_RESULT_APPROVED) || name.equals(TrackingUtil.SCREEN_NAME_PAYMENT_RESULT_PENDING) || name.equals(TrackingUtil.SCREEN_NAME_PAYMENT_RESULT_REJECTED) ||
                name.equals(TrackingUtil.SCREEN_NAME_PAYMENT_RESULT_INSTRUCTIONS);
    }

    private boolean isBatchTrackingStrategyScreenEvent(ScreenViewEvent screenViewEvent) {
        return screenViewEvent.getScreenName().equals(TrackingUtil.SCREEN_NAME_PAYMENT_VAULT) || screenViewEvent.getScreenName().equals(TrackingUtil.SCREEN_NAME_REVIEW_AND_CONFIRM);
    }

    public TrackingStrategy getTrackingStrategy() {
        return trackingStrategy;
    }

    public Event getEvent() {
        return mEvent;
    }
}