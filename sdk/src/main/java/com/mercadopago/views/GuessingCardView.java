package com.mercadopago.views;

import com.mercadopago.controllers.PaymentMethodGuessingController;
import com.mercadopago.exceptions.CardTokenException;
import com.mercadopago.exceptions.MercadoPagoError;
import com.mercadopago.model.ApiException;
import com.mercadopago.model.Discount;
import com.mercadopago.model.IdentificationType;
import com.mercadopago.model.Issuer;
import com.mercadopago.model.PayerCost;
import com.mercadopago.model.PaymentMethod;
import com.mercadopago.model.Token;
import com.mercadopago.mvp.MvpView;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by vaserber on 4/26/17.
 */

public interface GuessingCardView extends MvpView {

    void showError(MercadoPagoError error);

    void showApiExceptionError(ApiException exception);

    void setErrorView(String mErrorState);

    void setErrorView(CardTokenException exception);

    void setCardholderName(String cardholderName);

    void setIdentificationNumber(String identificationNumber);

    void showSecurityCodeInput();

    void showIdentificationInput();

    void initializeTitle();

    void finishCardFlow(PaymentMethod paymentMethod, Token token, Discount discount, Boolean directDiscountEnabled, List<Issuer> issuers);

    void finishCardFlow(PaymentMethod paymentMethod, Token token, Discount discount, Boolean directDiscountEnabled, Issuer issuer, List<PayerCost> payerCosts);

    void finishCardFlow(PaymentMethod paymentMethod, Token token, Discount discount, Boolean directDiscountEnabled, Issuer issuer, PayerCost payerCost);

    void clearErrorIdentificationNumber();

    void clearErrorView();

    void setErrorSecurityCode();

    void setErrorExpiryDate();

    void setErrorCardholderName();

    void setErrorIdentificationNumber();

    void setErrorCardNumber();

    void setIdentificationNumberRestrictions(String type);

    void showBankDeals();

    void hideBankDeals();

    void initializeIdentificationTypes(List<IdentificationType> identificationTypes);

    void hideIdentificationInput();

    void setCardNumberInputMaxLength(int length);

    void setSecurityCodeInputMaxLength(int length);

    void setSecurityCodeViewLocation(String location);

    void hideSecurityCodeInput();

    void showInputContainer();

    void showDiscountRow(BigDecimal transactionAmount);

    void startDiscountActivity(BigDecimal transactionAmount);

    void setCardNumberListeners(PaymentMethodGuessingController controller);

    void setCardholderNameListeners();

    void setExpiryDateListeners();

    void setSecurityCodeListeners();

    void setNextButtonListeners();

    void setBackButtonListeners();

    void setIdentificationTypeListeners();

    void setIdentificationNumberListeners();

    void loadViews();

    void decorate();

    void initializeTimer();

    void setNormalState();

    void loadCardViewData(PaymentMethod paymentMethod, Integer cardNumberLength, int securityCodeLength, String securityCodeLocation);

    void eraseDefaultSpace();

    void clearCardNumberInputLength();

    void clearCardNumberEditTextMask();

    void clearSecurityCodeEditText();

    void checkClearCardView();

    void askForPaymentType();

    void showFinishCardFlow();

    void askForIssuers();
}
