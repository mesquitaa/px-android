package com.mercadopago.presenters;

import android.text.TextUtils;
import android.view.View;

import com.mercadopago.R;
import com.mercadopago.callbacks.Callback;
import com.mercadopago.callbacks.FailureRecovery;
import com.mercadopago.controllers.CheckoutTimer;
import com.mercadopago.controllers.PaymentMethodGuessingController;
import com.mercadopago.core.MercadoPagoServices;
import com.mercadopago.exceptions.CardTokenException;
import com.mercadopago.exceptions.MercadoPagoError;
import com.mercadopago.model.ApiException;
import com.mercadopago.model.BankDeal;
import com.mercadopago.model.CardInformation;
import com.mercadopago.model.CardToken;
import com.mercadopago.model.Cardholder;
import com.mercadopago.model.Device;
import com.mercadopago.model.Discount;
import com.mercadopago.model.Identification;
import com.mercadopago.model.IdentificationType;
import com.mercadopago.model.Installment;
import com.mercadopago.model.Issuer;
import com.mercadopago.model.PayerCost;
import com.mercadopago.model.PaymentMethod;
import com.mercadopago.model.PaymentRecovery;
import com.mercadopago.model.PaymentType;
import com.mercadopago.model.SecurityCode;
import com.mercadopago.model.Setting;
import com.mercadopago.model.Token;
import com.mercadopago.mvp.MvpPresenter;
import com.mercadopago.mvp.OnResourcesRetrievedCallback;
import com.mercadopago.preferences.PaymentPreference;
import com.mercadopago.providers.GuessingCardProvider;
import com.mercadopago.uicontrollers.card.CardView;
import com.mercadopago.uicontrollers.card.FrontCardView;
import com.mercadopago.util.CurrenciesUtil;
import com.mercadopago.util.TextUtil;
import com.mercadopago.util.MercadoPagoUtil;
import com.mercadopago.util.MPCardMaskUtil;
import com.mercadopago.views.GuessingCardView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by vaserber on 10/13/16.
 */

public class GuessingCardPresenter extends MvpPresenter<GuessingCardView, GuessingCardProvider> {

    private static final int CARD_DEFAULT_SECURITY_CODE_LENGTH = 4;
    private static final int CARD_DEFAULT_IDENTIFICATION_NUMBER_LENGTH = 12;
//    private static final String NO_PAYER_COSTS_FOUND = "no payer costs found";
//    private static final String NO_INSTALLMENTS_FOUND_FOR_AN_ISSUER = "no installments found for an issuer";
//    private static final String MULTIPLE_INSTALLMENTS_FOUND_FOR_AN_ISSUER = "multiple installments found for an issuer";

    //Card controller
    private PaymentMethodGuessingController mPaymentMethodGuessingController;
    private List<IdentificationType> mIdentificationTypes;

    private FailureRecovery mFailureRecovery;

    //Mercado Pago instance
    private MercadoPagoServices mMercadoPago;

    //Activity parameters
    private String mPublicKey;
    private PaymentRecovery mPaymentRecovery;
    private PaymentMethod mPaymentMethod;
    private List<PaymentMethod> mPaymentMethodList;
    private Identification mIdentification;
    private boolean mIdentificationNumberRequired;
    private PaymentPreference mPaymentPreference;
    private String mMerchantBaseUrl;
    private String mMerchantDiscountUrl;
    private String mMerchantGetDiscountUri;
    private Map<String, String> mDiscountAdditionalInfo;
    private Boolean mShowDiscount;

    //Card Settings
    private CardInformation mCardInfo;
    private int mSecurityCodeLength;
    private String mSecurityCodeLocation;
    private boolean mIsSecurityCodeRequired;
    private boolean mEraseSpace;

    //Card Info
    private String mBin;
    private String mCardNumber;
    private String mCardholderName;
    private String mExpiryMonth;
    private String mExpiryYear;
    private String mSecurityCode;
    private IdentificationType mIdentificationType;
    private String mIdentificationNumber;
    private CardToken mCardToken;
    private Token mToken;
    private PaymentType mPaymentType;

    //Extra info
    private List<BankDeal> mBankDealsList;
    private boolean showPaymentTypes;
    private List<PaymentType> mPaymentTypesList;
    private Boolean mShowBankDeals;
    private Device mDevice;

    //Discount
    private Boolean mDiscountEnabled;
    private Boolean mDirectDiscountEnabled;
    private String mPayerEmail;
    private BigDecimal mTransactionAmount;
    private Discount mDiscount;
    private String mPrivateKey;
    private int mCurrentNumberLength;
    private Issuer mIssuer;


    public GuessingCardPresenter() {
        this.mEraseSpace = true;
    }

    public void setCurrentNumberLength(int currentNumberLength) {
        this.mCurrentNumberLength = currentNumberLength;
    }

    public void setFailureRecovery(FailureRecovery failureRecovery) {
        this.mFailureRecovery = failureRecovery;
    }

    public void setDevice(Device device) {
        this.mDevice = device;
    }

    public String getPublicKey() {
        return mPublicKey;
    }

    public void setPublicKey(String publicKey) {
        this.mPublicKey = publicKey;
    }

    public PaymentRecovery getPaymentRecovery() {
        return mPaymentRecovery;
    }

    public void setPaymentRecovery(PaymentRecovery paymentRecovery) {
        this.mPaymentRecovery = paymentRecovery;
        if (recoverWithCardholder()) {
            saveCardholderName(paymentRecovery.getToken().getCardHolder().getName());
            saveIdentificationNumber(paymentRecovery.getToken().getCardHolder().getIdentification().getNumber());
        }
    }

    private void fillRecoveryFields() {
        if (recoverWithCardholder()) {
            getView().setCardholderName(mPaymentRecovery.getToken().getCardHolder().getName());
            getView().setIdentificationNumber(mPaymentRecovery.getToken().getCardHolder().getIdentification().getNumber());
        }
    }

    private boolean recoverWithCardholder() {
        return mPaymentRecovery != null && mPaymentRecovery.getToken() != null &&
                mPaymentRecovery.getToken().getCardHolder() != null;
    }

    public PaymentMethod getPaymentMethod() {
        return mPaymentMethod;
    }

    public List<IdentificationType> getIdentificationTypes() {
        return this.mIdentificationTypes;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.mPaymentMethod = paymentMethod;
        if (paymentMethod == null) {
            clearCardSettings();
        }
    }

    public boolean hasToShowPaymentTypes() {
        return showPaymentTypes;
    }

    public boolean isSecurityCodeRequired() {
        return mIsSecurityCodeRequired;
    }

    public void setSecurityCodeRequired(boolean required) {
        this.mIsSecurityCodeRequired = required;
        if (required) {
            getView().showSecurityCodeInput();
        }
    }

    public void setSecurityCodeLength(int securityCodeLength) {
        this.mSecurityCodeLength = securityCodeLength;
    }

    private void clearCardSettings() {
        mSecurityCodeLength = CARD_DEFAULT_SECURITY_CODE_LENGTH;
        mSecurityCodeLocation = CardView.CARD_SIDE_BACK;
        mIsSecurityCodeRequired = true;
        mBin = "";
    }

    public String getSecurityCodeLocation() {
        return mSecurityCodeLocation;
    }

    public int getSecurityCodeLength() {
        return mSecurityCodeLength;
    }

    public void setToken(Token token) {
        this.mToken = token;
    }

    public Token getToken() {
        return mToken;
    }

    public CardToken getCardToken() {
        return mCardToken;
    }

    public void setCardToken(CardToken cardToken) {
        this.mCardToken = cardToken;
    }

    public List<PaymentMethod> getPaymentMethodList() {
        return mPaymentMethodList;
    }

    public void setPaymentMethodList(List<PaymentMethod> paymentMethodList) {
        this.mPaymentMethodList = paymentMethodList;
    }

    public void setPaymentTypesList(List<PaymentType> paymentTypesList) {
        this.mPaymentTypesList = paymentTypesList;
    }

    public void setIdentificationTypesList(List<IdentificationType> identificationTypesList) {
        this.mIdentificationTypes = identificationTypesList;
    }

    public void setBankDealsList(List<BankDeal> bankDealsList) {
        this.mBankDealsList = bankDealsList;
    }

    public Identification getIdentification() {
        return mIdentification;
    }

    public void setIdentification(Identification identification) {
        this.mIdentification = identification;
    }

    public boolean isIdentificationNumberRequired() {
        return mIdentificationNumberRequired;
    }

    public void setIdentificationNumberRequired(boolean identificationNumberRequired) {
        this.mIdentificationNumberRequired = identificationNumberRequired;
        if (identificationNumberRequired) {
            getView().showIdentificationInput();
        }
    }

    public PaymentPreference getPaymentPreference() {
        return mPaymentPreference;
    }

    public void setPaymentPreference(PaymentPreference paymentPreference) {
        this.mPaymentPreference = paymentPreference;
    }

    public void initializeCardToken() {
        mCardToken = new CardToken("", null, null, "", "", "", "");
    }

    public void initialize() {
        try {
            validateParameters();
            onValidStart();
        } catch (IllegalStateException exception) {
            getView().showError(new MercadoPagoError(exception.getMessage(), false));
        }
    }

    private void validateParameters() throws IllegalStateException {
        if (mPublicKey == null) {
            throw new IllegalStateException(getResourcesProvider().getMissingPublicKeyErrorMessage());
        }
    }

    public void onValidStart() {
        initializeCardToken();

        getView().loadViews();
        getView().decorate();

        if (CheckoutTimer.getInstance().isTimerEnabled()) {
            getView().initializeTimer();
        } else {
            checkBankDeals();
        }
        getView().setNormalState();
        startGuessingFlow();
        fillRecoveryFields();
    }

    private void startGuessingFlow() {
        if (showDiscount()) {
            loadDiscount();
        } else {
            loadPaymentMethods();
        }
    }

    public String getSecurityCodeFront() {
        String securityCode = null;
        if (mSecurityCodeLocation.equals(CardView.CARD_SIDE_FRONT)) {
            securityCode = getSecurityCode();
        }
        return securityCode;
    }

    public CardInformation getCardInformation() {
        return mCardInfo;
    }

    public boolean isCardLengthResolved() {
        return mPaymentMethod != null && mBin != null;
    }

    public Integer getCardNumberLength() {
        return PaymentMethodGuessingController.getCardNumberLength(mPaymentMethod, mBin);
    }

    public void initializeGuessingCardNumberController() {
        List<PaymentMethod> supportedPaymentMethods = mPaymentPreference
                .getSupportedPaymentMethods(mPaymentMethodList);
        mPaymentMethodGuessingController = new PaymentMethodGuessingController(
                supportedPaymentMethods, mPaymentPreference.getDefaultPaymentTypeId(),
                mPaymentPreference.getExcludedPaymentTypes());
    }

    private void startGuessingForm() {
        initializeGuessingCardNumberController();
        getView().initializeTitle();
        getView().setCardNumberListeners(mPaymentMethodGuessingController);
        getView().setCardholderNameListeners();
        getView().setExpiryDateListeners();
        getView().setSecurityCodeListeners();
        getView().setIdentificationTypeListeners();
        getView().setIdentificationNumberListeners();
        getView().setNextButtonListeners();
        getView().setBackButtonListeners();
    }

    public String getPaymentTypeId() {
        if (mPaymentMethodGuessingController == null) {
            return null;
        } else {
            return mPaymentMethodGuessingController.getPaymentTypeId();
        }
    }

    private void loadDiscount() {
        initializeDiscountRow();
        loadPaymentMethods();

    }

    private void getDirectDiscount() {
        if (isMerchantServerDiscountsAvailable()) {
            getMerchantDirectDiscount();
        } else {
            getMPDirectDiscount();
        }
    }

    private Boolean isAmountValid() {
        return mTransactionAmount != null && mTransactionAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    public void initializeDiscountActivity() {
        getView().startDiscountActivity(mTransactionAmount);
    }

    private void initializeDiscountRow() {
        getView().showDiscountRow(mTransactionAmount);
    }

    private void getMPDirectDiscount() {
        mMercadoPago.getDirectDiscount(mTransactionAmount.toString(), mPayerEmail, new Callback<Discount>() {
            @Override
            public void success(Discount discount) {
                mDiscount = discount;
                initializeDiscountRow();
                loadPaymentMethods();
            }

            @Override
            public void failure(ApiException apiException) {
                mDirectDiscountEnabled = false;
                initializeDiscountRow();
                loadPaymentMethods();
            }
        });
    }

    private void getMerchantDirectDiscount() {
        String merchantDiscountUrl = getMerchantServerDiscountUrl();

        getResourcesProvider().getDirectDiscountAsync(mTransactionAmount.toString(), mPayerEmail, merchantDiscountUrl, mMerchantGetDiscountUri, mDiscountAdditionalInfo, new OnResourcesRetrievedCallback<Discount>() {
            @Override
            public void onSuccess(Discount discount) {
                mDiscount = discount;
                resolveDirectDiscount();
            }

            @Override
            public void onFailure(MercadoPagoError error) {
                mDirectDiscountEnabled = false;
                resolveDirectDiscount();
            }
        });
    }

    private void resolveDirectDiscount() {
        initializeDiscountRow();
        loadPaymentMethods();
    }

    public void onDiscountReceived(Discount discount) {
        setDiscount(discount);
        initializeDiscountRow();
    }

    public Discount getDiscount() {
        return mDiscount;
    }

    public void setDiscount(Discount discount) {
        this.mDiscount = discount;
    }

    public void setPayerEmail(String payerEmail) {
        this.mPayerEmail = payerEmail;
    }

    public String getPayerEmail() {
        return mPayerEmail;
    }

    public void setDiscountEnabled(Boolean discountEnabled) {
        this.mDiscountEnabled = discountEnabled;
    }

    public void setDiscountAdditionalInfo(Map<String, String> discountAdditionalInfo) {
        this.mDiscountAdditionalInfo = discountAdditionalInfo;
    }

    public Map<String, String> getDiscountAdditionalInfo() {
        return this.mDiscountAdditionalInfo;
    }

    public void setMerchantDiscountBaseUrl(String merchantDiscountUrl) {
        this.mMerchantDiscountUrl = merchantDiscountUrl;
    }

    public String getMerchantDiscountBaseUrl() {
        return this.mMerchantDiscountUrl;
    }

    public void setMerchantBaseUrl(String merchantBaseUrl) {
        this.mMerchantBaseUrl = merchantBaseUrl;
    }

    public String getMerchantBaseUrl() {
        return this.mMerchantBaseUrl;
    }

    public void setMerchantGetDiscountUri(String merchantGetDiscountUri) {
        this.mMerchantGetDiscountUri = merchantGetDiscountUri;
    }

    public String getMerchantGetDiscountUri() {
        return mMerchantGetDiscountUri;
    }

    public Boolean getDiscountEnabled() {
        return this.mDiscountEnabled;
    }

    public void setDirectDiscountEnabled(Boolean directDiscountEnabled) {
        this.mDirectDiscountEnabled = directDiscountEnabled;
    }

    public void setShowDiscount(Boolean showDiscount) {
        this.mShowDiscount = showDiscount;
    }

    public Boolean getDirectDiscountEnabled() {
        return this.mDirectDiscountEnabled;
    }

    public BigDecimal getTransactionAmount() {
        BigDecimal amount;

        if (mDiscount != null && isDiscountValid()) {
            amount = mDiscount.getAmountWithDiscount(mTransactionAmount);
        } else {
            amount = mTransactionAmount;
        }

        return amount;
    }

    private Boolean isDiscountValid() {
        return isDiscountCurrencyIdValid() && isAmountValid(mDiscount.getCouponAmount()) && isCampaignIdValid();
    }

    private Boolean isDiscountCurrencyIdValid() {
        return mDiscount != null && mDiscount.getCurrencyId() != null && CurrenciesUtil.isValidCurrency(mDiscount.getCurrencyId());
    }

    private Boolean isCampaignIdValid() {
        return mDiscount != null && mDiscount.getId() != null;
    }

    private Boolean isAmountValid(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) >= 0;
    }


    private void loadPaymentMethods() {
        if (mPaymentMethodList == null || mPaymentMethodList.isEmpty()) {
            getPaymentMethodsAsync();
        } else {
            getView().showInputContainer();
            startGuessingForm();
        }
    }

    public void checkBankDeals() {
        if (mShowBankDeals) {
            getBankDealsAsync();
        } else {
            getView().hideBankDeals();
        }
    }

    private void getPaymentMethodsAsync() {
        mMercadoPago.getPaymentMethods(new Callback<List<PaymentMethod>>() {
            @Override
            public void success(List<PaymentMethod> paymentMethods) {
                getView().showInputContainer();
                mPaymentMethodList = paymentMethods;
                startGuessingForm();
            }

            @Override
            public void failure(ApiException apiException) {
                setFailureRecovery(new FailureRecovery() {
                    @Override
                    public void recover() {
                        getPaymentMethodsAsync();
                    }
                });
                getView().showApiExceptionError(apiException);
            }
        });
    }

    public void setSelectedPaymentType(PaymentType paymentType) {
        if (mPaymentMethodGuessingController == null) {
            return;
        }
        for (PaymentMethod paymentMethod : mPaymentMethodGuessingController.getGuessedPaymentMethods()) {
            if (paymentMethod.getPaymentTypeId().equals(paymentType.getId())) {
                setPaymentMethod(paymentMethod);
            }
        }
    }

    public String getSavedBin() {
        return mBin;
    }

    public void saveBin(String bin) {
        mBin = bin;
        mPaymentMethodGuessingController.saveBin(bin);
    }

    public void configureWithSettings() {
        if (mPaymentMethod == null) return;

        mIsSecurityCodeRequired = mPaymentMethod.isSecurityCodeRequired(mBin);
        if (!mIsSecurityCodeRequired) {
            getView().hideSecurityCodeInput();
        }
        Setting setting = PaymentMethodGuessingController.getSettingByPaymentMethodAndBin(mPaymentMethod, mBin);
        if (setting == null) {
            getView().showError(new MercadoPagoError(getResourcesProvider().getSettingNotFoundForBinErrorMessage(), false));

        } else {
            int cardNumberLength = getCardNumberLength();
            int spaces = FrontCardView.CARD_DEFAULT_AMOUNT_SPACES;

            if (cardNumberLength == FrontCardView.CARD_NUMBER_DINERS_LENGTH || cardNumberLength == FrontCardView.CARD_NUMBER_AMEX_LENGTH || cardNumberLength == FrontCardView.CARD_NUMBER_MAESTRO_SETTING_1_LENGTH) {
                spaces = FrontCardView.CARD_AMEX_DINERS_AMOUNT_SPACES;
            } else if (cardNumberLength == FrontCardView.CARD_NUMBER_MAESTRO_SETTING_2_LENGTH) {
                spaces = FrontCardView.CARD_NUMBER_MAESTRO_SETTING_2_AMOUNT_SPACES;
            }
            getView().setCardNumberInputMaxLength(cardNumberLength + spaces);
            SecurityCode securityCode = setting.getSecurityCode();
            if (securityCode == null) {
                mSecurityCodeLength = CARD_DEFAULT_SECURITY_CODE_LENGTH;
                mSecurityCodeLocation = CardView.CARD_SIDE_BACK;
            } else {
                mSecurityCodeLength = securityCode.getLength();
                mSecurityCodeLocation = securityCode.getCardLocation();
            }
            getView().setSecurityCodeInputMaxLength(mSecurityCodeLength);
            getView().setSecurityCodeViewLocation(mSecurityCodeLocation);
        }
    }

    public void loadIdentificationTypes() {
        if (mPaymentMethod == null) {
            return;
        }
        mIdentificationNumberRequired = getPaymentMethod().isIdentificationNumberRequired();
        if (mIdentificationNumberRequired) {
            getIdentificationTypesAsync();
        } else {
            getView().hideIdentificationInput();
        }
    }

    private void getIdentificationTypesAsync() {
        getResourcesProvider().getIdentificationTypesAsync(new OnResourcesRetrievedCallback<List<IdentificationType>>() {
            @Override
            public void onSuccess(List<IdentificationType> identificationTypes) {
                resolveIdentificationTypes(identificationTypes);
            }

            @Override
            public void onFailure(MercadoPagoError error) {
                if (isViewAttached()) {
                    getView().showError(error);
                    setFailureRecovery(new FailureRecovery() {
                        @Override
                        public void recover() {
                            getIdentificationTypesAsync();
                        }
                    });
                }
            }
        });
    }

    private void resolveIdentificationTypes(List<IdentificationType> identificationTypes) {
        if (identificationTypes.isEmpty()) {
            getView().showError(new MercadoPagoError(getResourcesProvider().getMissingIdentificationTypesErrorMessage(), false));
        } else {
            mIdentificationType = identificationTypes.get(0);
            getView().initializeIdentificationTypes(identificationTypes);
            mIdentificationTypes = identificationTypes;
        }
    }

    public List<BankDeal> getBankDealsList() {
        return mBankDealsList;
    }

    private void getBankDealsAsync() {
        getResourcesProvider().getBankDealsAsync(new OnResourcesRetrievedCallback<List<BankDeal>>() {
            @Override
            public void onSuccess(List<BankDeal> bankDeals) {
                resolveBankDeals(bankDeals);
            }

            @Override
            public void onFailure(MercadoPagoError error) {
                if (isViewAttached()) {
                    getView().showError(error);
                    setFailureRecovery(new FailureRecovery() {
                        @Override
                        public void recover() {
                            getBankDealsAsync();
                        }
                    });
                }
            }
        });
    }

    private void resolveBankDeals(List<BankDeal> bankDeals) {
        if (bankDeals == null || bankDeals.isEmpty()) {
            getView().hideBankDeals();
        } else if (bankDeals.size() >= 1) {
            mBankDealsList = bankDeals;
            getView().showBankDeals();
        }
//        if (bankDeals != null) {
//            if (bankDeals.isEmpty()) {
//                getView().hideBankDeals();
//            } else if (bankDeals.size() >= 1) {
//                mBankDealsList = bankDeals;
//                getView().showBankDeals();
//            }
//        }
    }

    public void enablePaymentTypeSelection(List<PaymentMethod> paymentMethodList) {
        List<PaymentType> paymentTypesList = new ArrayList<>();
        for (PaymentMethod pm : paymentMethodList) {
            PaymentType type = new PaymentType(pm.getPaymentTypeId());
            paymentTypesList.add(type);
        }
        mPaymentTypesList = paymentTypesList;
        mPaymentType = paymentTypesList.get(0);
        showPaymentTypes = true;
    }

    public void disablePaymentTypeSelection() {
        mPaymentType = null;
        showPaymentTypes = false;
        mPaymentTypesList = null;
    }

    public List<PaymentMethod> getGuessedPaymentMethods() {
        if (mPaymentMethodGuessingController == null) {
            return null;
        }
        return mPaymentMethodGuessingController.getGuessedPaymentMethods();
    }

    public List<PaymentType> getPaymentTypes() {
        return mPaymentTypesList;
    }

    public void saveCardNumber(String cardNumber) {
        this.mCardNumber = cardNumber;
    }

    public void saveCardholderName(String cardholderName) {
        this.mCardholderName = cardholderName;
    }

    public void saveExpiryMonth(String expiryMonth) {
        this.mExpiryMonth = expiryMonth;
    }

    public void saveExpiryYear(String expiryYear) {
        this.mExpiryYear = expiryYear;
    }

    public void saveSecurityCode(String securityCode) {
        this.mSecurityCode = securityCode;
    }

    public void saveIdentificationNumber(String identificationNumber) {
        this.mIdentificationNumber = identificationNumber;
    }

    public void saveIdentificationType(IdentificationType identificationType) {
        this.mIdentificationType = identificationType;
        if (identificationType != null) {
            mIdentification.setType(identificationType.getId());
            getView().setIdentificationNumberRestrictions(identificationType.getType());
        }
    }

    public IdentificationType getIdentificationType() {
        return this.mIdentificationType;
    }

    public void setIdentificationNumber(String number) {
        mIdentificationNumber = number;
        mIdentification.setNumber(number);
    }

    public String getCardNumber() {
        return mCardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.mCardNumber = cardNumber;
    }

    public String getCardholderName() {
        return mCardholderName;
    }

    public void setCardholderName(String name) {
        this.mCardholderName = name;
    }

    public String getExpiryMonth() {
        return mExpiryMonth;
    }

    public String getExpiryYear() {
        return mExpiryYear;
    }

    public void setExpiryMonth(String expiryMonth) {
        this.mExpiryMonth = expiryMonth;
    }

    public void setExpiryYear(String expiryYear) {
        this.mExpiryYear = expiryYear;
    }

    public String getSecurityCode() {
        return mSecurityCode;
    }

    public String getIdentificationNumber() {
        return mIdentificationNumber;
    }

    public int getIdentificationNumberMaxLength() {
        int maxLength = CARD_DEFAULT_IDENTIFICATION_NUMBER_LENGTH;
        if (mIdentificationType != null) {
            maxLength = mIdentificationType.getMaxLength();
        }
        return maxLength;
    }

    public boolean validateCardNumber() {
        mCardToken.setCardNumber(getCardNumber());
        try {
            if (mPaymentMethod == null) {
                if (getCardNumber() == null || getCardNumber().length() < MercadoPagoUtil.BIN_LENGTH) {
                    throw new CardTokenException(CardTokenException.INVALID_CARD_NUMBER_INCOMPLETE);

                } else if (getCardNumber().length() == MercadoPagoUtil.BIN_LENGTH) {
                    throw new CardTokenException(CardTokenException.INVALID_PAYMENT_METHOD);
                } else {
                    throw new CardTokenException(CardTokenException.INVALID_PAYMENT_METHOD);
                }
            }
            mCardToken.validateCardNumber(mPaymentMethod);
            getView().clearErrorView();
            return true;
        } catch (CardTokenException e) {
            getView().setErrorView(e);
            getView().setErrorCardNumber();
            return false;
        }
    }

    public boolean validateCardName() {
        Cardholder cardHolder = new Cardholder();
        cardHolder.setName(getCardholderName());
        cardHolder.setIdentification(mIdentification);
        mCardToken.setCardholder(cardHolder);
        if (mCardToken.validateCardholderName()) {
            getView().clearErrorView();
            return true;
        } else {
            getView().setErrorView(getResourcesProvider().getInvalidEmptyNameErrorMessage());
            getView().setErrorCardholderName();
            return false;
        }
    }

    public boolean validateExpiryDate() {
        String monthString = getExpiryMonth();
        String yearString = getExpiryYear();
        Integer month = (monthString == null || monthString.isEmpty()) ? null : Integer.valueOf(monthString);
        Integer year = (yearString == null || yearString.isEmpty()) ? null : Integer.valueOf(yearString);
        mCardToken.setExpirationMonth(month);
        mCardToken.setExpirationYear(year);
        if (mCardToken.validateExpiryDate()) {
            getView().clearErrorView();
            return true;
        } else {
            getView().setErrorView(getResourcesProvider().getInvalidExpiryDateErrorMessage());
            getView().setErrorExpiryDate();
            return false;
        }
    }

    public boolean validateSecurityCode() {
        mCardToken.setSecurityCode(getSecurityCode());
        try {
            mCardToken.validateSecurityCode(mPaymentMethod);
            getView().clearErrorView();
            return true;
        } catch (CardTokenException e) {
            setCardSecurityCodeErrorView(e);
            return false;
        }
    }

    private void setCardSecurityCodeErrorView(CardTokenException exception) {
        if (!isSecurityCodeRequired()) {
            return;
        }
        getView().setErrorView(exception);
        getView().setErrorSecurityCode();
    }

    public boolean validateIdentificationNumber() {
        mIdentification.setNumber(getIdentificationNumber());
        mCardToken.getCardholder().setIdentification(mIdentification);
        boolean ans = mCardToken.validateIdentificationNumber(mIdentificationType);
        if (ans) {
            getView().clearErrorView();
            getView().clearErrorIdentificationNumber();
        } else {
            setCardIdentificationErrorView(getResourcesProvider().getInvalidIdentificationNumberErrorMessage());
        }
        return ans;
    }

    private void setCardIdentificationErrorView(String message) {
        getView().setErrorView(message);
        getView().setErrorIdentificationNumber();
    }

    public boolean checkIsEmptyOrValidCardholderName() {
        return TextUtils.isEmpty(mCardholderName) || validateCardName();
    }

    public boolean checkIsEmptyOrValidExpiryDate() {
        return TextUtils.isEmpty(mExpiryMonth) || validateExpiryDate();
    }

    public boolean checkIsEmptyOrValidSecurityCode() {
        return TextUtils.isEmpty(mSecurityCode) || validateSecurityCode();
    }

    public boolean checkIsEmptyOrValidIdentificationNumber() {
        return TextUtils.isEmpty(mIdentificationNumber) || validateIdentificationNumber();
    }

    public void recoverFromFailure() {
        if (mFailureRecovery != null) {
            mFailureRecovery.recover();
        }
    }

    private Boolean showDiscount() {
        return mDiscountEnabled && mShowDiscount;
    }

    public void setShowBankDeals(Boolean showBankDeals) {
        this.mShowBankDeals = showBankDeals;
    }

    public void setTransactionAmount(BigDecimal transactionAmount) {
        this.mTransactionAmount = transactionAmount;
    }

    public boolean isDefaultSpaceErasable() {

        if (MPCardMaskUtil.isDefaultSpaceErasable(mCurrentNumberLength)) {
            mEraseSpace = true;
        }

        if (isCardLengthResolved() && mEraseSpace && (getCardNumberLength() == FrontCardView.CARD_NUMBER_MAESTRO_SETTING_1_LENGTH || getCardNumberLength() == FrontCardView.CARD_NUMBER_MAESTRO_SETTING_2_LENGTH)) {
            mEraseSpace = false;
            return true;
        }
        return false;
    }

    private boolean isMerchantServerDiscountsAvailable() {
        return !TextUtil.isEmpty(getMerchantServerDiscountUrl()) && !TextUtil.isEmpty(mMerchantGetDiscountUri);
    }

    private String getMerchantServerDiscountUrl() {
        String merchantBaseUrl;

        if (TextUtil.isEmpty(mMerchantDiscountUrl)) {
            merchantBaseUrl = this.mMerchantBaseUrl;
        } else {
            merchantBaseUrl = this.mMerchantDiscountUrl;
        }

        return merchantBaseUrl;
    }

    public void setPrivateKey(String privateKey) {
        this.mPrivateKey = privateKey;
    }

    public String getPrivateKey() {
        return mPrivateKey;
    }

    public void clearSpaceErasableSettings() {
        this.mEraseSpace = true;
    }

    public boolean isPaymentMethodResolved() {
        return mPaymentMethod != null;
    }

    public void finishCardFlow() {
        createTokenAsync();
    }

    private void createTokenAsync() {
        getResourcesProvider().createTokenAsync(mCardToken, mDevice, new OnResourcesRetrievedCallback<Token>() {
            @Override
            public void onSuccess(Token token) {
                mToken = token;
                getIssuersAsync();
            }

            @Override
            public void onFailure(MercadoPagoError error) {
                if (isViewAttached()) {
                    getView().showError(error);
                    setFailureRecovery(new FailureRecovery() {
                        @Override
                        public void recover() {
                            createTokenAsync();
                        }
                    });
                }
            }
        });
    }

    private void getIssuersAsync() {
        getResourcesProvider().getIssuersAsync(mPaymentMethod.getId(), mBin, new OnResourcesRetrievedCallback<List<Issuer>>() {
            @Override
            public void onSuccess(List<Issuer> issuers) {
                resolveIssuersList(issuers);
            }

            @Override
            public void onFailure(MercadoPagoError error) {
                if (isViewAttached()) {
                    getView().showError(error);
                    setFailureRecovery(new FailureRecovery() {
                        @Override
                        public void recover() {
                            getIssuersAsync();
                        }
                    });
                }
            }
        });
    }

    private void resolveIssuersList(List<Issuer> issuers) {
        if (issuers.size() == 1) {
            mIssuer = issuers.get(0);
            getInstallmentsAsync();
        } else {
            getView().finishCardFlow(mPaymentMethod, mToken, mDiscount, mDirectDiscountEnabled, issuers);
        }
    }

    private void getInstallmentsAsync() {
        getResourcesProvider().getInstallmentsAsync(mBin, mTransactionAmount, mIssuer.getId(), mPaymentMethod.getId(), new OnResourcesRetrievedCallback<List<Installment>>() {
            @Override
            public void onSuccess(List<Installment> installments) {
                resolveInstallments(installments);
            }

            @Override
            public void onFailure(MercadoPagoError error) {
                if (isViewAttached()) {
                    getView().showError(error);
                    setFailureRecovery(new FailureRecovery() {
                        @Override
                        public void recover() {
                            getInstallmentsAsync();
                        }
                    });
                }
            }
        });
    }

    private void resolveInstallments(List<Installment> installments) {
        String errorMessage = null;
        if (installments == null || installments.size() == 0) {
            errorMessage = getResourcesProvider().getMissingInstallmentsForIssuerErrorMessage();
        } else if (installments.size() == 1) {
            resolvePayerCosts(installments.get(0).getPayerCosts());
        } else {
            errorMessage = getResourcesProvider().getMultipleInstallmentsForIssuerErrorMessage();
        }
        if (errorMessage != null && isViewAttached()) {
            getView().showError(new MercadoPagoError(errorMessage, false));
        }
    }

    private void resolvePayerCosts(List<PayerCost> payerCosts) {
        PayerCost defaultPayerCost = mPaymentPreference.getDefaultInstallments(payerCosts);
        if (defaultPayerCost != null) {
            getView().finishCardFlow(mPaymentMethod, mToken, mDiscount, mDirectDiscountEnabled, mIssuer, defaultPayerCost);
        } else if (payerCosts.isEmpty()) {
            getView().showError(new MercadoPagoError(getResourcesProvider().getMissingPayerCostsErrorMessage(), false));
        } else if (payerCosts.size() == 1) {
            getView().finishCardFlow(mPaymentMethod, mToken, mDiscount, mDirectDiscountEnabled, mIssuer, payerCosts.get(0));
        } else {
            getView().finishCardFlow(mPaymentMethod, mToken, mDiscount, mDirectDiscountEnabled, mIssuer, payerCosts);
        }
    }

    public void resolvePaymentMethodListSet(List<PaymentMethod> paymentMethodList, String bin) {
        saveBin(bin);
        if (paymentMethodList.size() == 0) {
            getView().setCardNumberInputMaxLength(MercadoPagoUtil.BIN_LENGTH);
            getView().setErrorView(getResourcesProvider().getInvalidPaymentMethodErrorMessage());
        } else if (paymentMethodList.size() == 1) {
            resolvePaymentMethodSet(paymentMethodList.get(0));
        } else {
            enablePaymentTypeSelection(paymentMethodList);
            resolvePaymentMethodSet(paymentMethodList.get(0));
        }
    }

    public void resolvePaymentMethodSet(PaymentMethod paymentMethod) {

        if (getPaymentMethod() == null) {
            setPaymentMethod(paymentMethod);
            configureWithSettings();
            loadIdentificationTypes();
            getView().loadCardViewData(paymentMethod, getCardNumberLength(), getSecurityCodeLength(), getSecurityCodeLocation());
        }
        //We need to erase default space in position 4 in some special cases.
        if (isDefaultSpaceErasable()) {
            getView().eraseDefaultSpace();
        }
    }

    public void resolvePaymentMethodCleared() {
        getView().clearErrorView();
        getView().clearCardNumberInputLength();

        if (isPaymentMethodResolved()) {
            clearSpaceErasableSettings();
            getView().clearCardNumberEditTextMask();
            setPaymentMethod(null);
            getView().clearSecurityCodeEditText();
            initializeCardToken();
            setIdentificationNumberRequired(true);
            setSecurityCodeRequired(true);
            disablePaymentTypeSelection();
            getView().checkClearCardView();
        }

    }
}
