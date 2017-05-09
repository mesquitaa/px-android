package com.mercadopago.guessingcard;

import com.mercadopago.constants.PaymentTypes;
import com.mercadopago.controllers.PaymentMethodGuessingController;
import com.mercadopago.exceptions.CardTokenException;
import com.mercadopago.exceptions.MercadoPagoError;
import com.mercadopago.mocks.BankDeals;
import com.mercadopago.mocks.Cards;
import com.mercadopago.mocks.DummyCard;
import com.mercadopago.mocks.IdentificationTypes;
import com.mercadopago.mocks.Issuers;
import com.mercadopago.mocks.PayerCosts;
import com.mercadopago.mocks.PaymentMethods;
import com.mercadopago.mocks.Tokens;
import com.mercadopago.model.ApiException;
import com.mercadopago.model.BankDeal;
import com.mercadopago.model.CardToken;
import com.mercadopago.model.Device;
import com.mercadopago.model.Discount;
import com.mercadopago.model.IdentificationType;
import com.mercadopago.model.Installment;
import com.mercadopago.model.Issuer;
import com.mercadopago.model.PayerCost;
import com.mercadopago.model.Payment;
import com.mercadopago.model.PaymentMethod;
import com.mercadopago.model.PaymentRecovery;
import com.mercadopago.model.Token;
import com.mercadopago.mvp.OnResourcesRetrievedCallback;
import com.mercadopago.preferences.PaymentPreference;
import com.mercadopago.presenters.GuessingCardPresenter;
import com.mercadopago.providers.GuessingCardProvider;
import com.mercadopago.uicontrollers.card.CardView;
import com.mercadopago.utils.CardTestUtils;
import com.mercadopago.views.GuessingCardView;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Created by vaserber on 5/9/17.
 */

public class GuessingCardPresenterTest {

    @Test
    public void ifPublicKeyNotSetThenShowMissingPublicKeyError() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.initialize();

        assertEquals(MockedProvider.MISSING_PUBLIC_KEY, mockedView.errorShown.getMessage());
    }

    @Test
    public void ifPublicKeySetThenCheckValidStart() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        List<PaymentMethod> paymentMethodList = PaymentMethods.getPaymentMethodListMLA();
        provider.setPaymentMethodsResponse(paymentMethodList);

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.setPublicKey("mockedPublicKey");

        presenter.initialize();

        assertTrue(mockedView.validStart);
    }

    @Test
    public void ifPaymentRecoverySetThenSaveCardholderNameAndIdentification() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        List<PaymentMethod> paymentMethodList = PaymentMethods.getPaymentMethodListMLA();
        provider.setPaymentMethodsResponse(paymentMethodList);

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        Token mockedToken = Tokens.getToken();
        PaymentMethod mockedPaymentMethod = PaymentMethods.getPaymentMethodOnVisa();
        PayerCost mockedPayerCost = PayerCosts.getPayerCost();
        Issuer mockedIssuer = Issuers.getIssuerMLA();
        String paymentStatus = Payment.StatusCodes.STATUS_REJECTED;
        String paymentStatusDetail = Payment.StatusCodes.STATUS_DETAIL_CC_REJECTED_CALL_FOR_AUTHORIZE;
        PaymentRecovery mockedPaymentRecovery = new PaymentRecovery(mockedToken, mockedPaymentMethod, mockedPayerCost, mockedIssuer, paymentStatus, paymentStatusDetail);

        presenter.setPublicKey("mockedPublicKey");
        presenter.setPaymentRecovery(mockedPaymentRecovery);

        presenter.initialize();

        assertTrue(mockedView.validStart);
        assertEquals(presenter.getCardholderName(), mockedPaymentRecovery.getToken().getCardHolder().getName());
        assertEquals(presenter.getIdentificationNumber(), mockedPaymentRecovery.getToken().getCardHolder().getIdentification().getNumber());
        assertEquals(mockedView.savedCardholderName, mockedPaymentRecovery.getToken().getCardHolder().getName());
        assertEquals(mockedView.savedIdentificationNumber, mockedPaymentRecovery.getToken().getCardHolder().getIdentification().getNumber());
    }

    @Test
    public void ifPaymentMethodListSetWithOnePaymentMethodThenSelectIt() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        List<PaymentMethod> paymentMethodList = PaymentMethods.getPaymentMethodListMLA();
        provider.setPaymentMethodsResponse(paymentMethodList);

        List<IdentificationType> identificationTypesList = IdentificationTypes.getIdentificationTypes();
        provider.setIdentificationTypesResponse(identificationTypesList);

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.setPublicKey("mockedPublicKey");

        presenter.initialize();

        List<PaymentMethod> mockedGuessedPaymentMethods = new ArrayList<>();
        mockedGuessedPaymentMethods.add(PaymentMethods.getPaymentMethodOnVisa());

        presenter.resolvePaymentMethodListSet(mockedGuessedPaymentMethods, Cards.MOCKED_BIN_VISA);

        assertTrue(mockedView.cardDataViewLoaded);
    }

    @Test
    public void ifPaymentMethodListSetIsEmptyThenShowError() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        List<PaymentMethod> paymentMethodList = PaymentMethods.getPaymentMethodListMLA();
        provider.setPaymentMethodsResponse(paymentMethodList);

        List<IdentificationType> identificationTypesList = IdentificationTypes.getIdentificationTypes();
        provider.setIdentificationTypesResponse(identificationTypesList);

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.setPublicKey("mockedPublicKey");

        presenter.initialize();

        List<PaymentMethod> mockedGuessedPaymentMethods = new ArrayList<>();

        presenter.resolvePaymentMethodListSet(mockedGuessedPaymentMethods, Cards.MOCKED_BIN_VISA);

        assertFalse(mockedView.cardDataViewLoaded);
        assertTrue(mockedView.formDataErrorState);
        assertEquals(mockedView.formDataErrorCause, MockedProvider.INVALID_PAYMENT_METHOD);
    }

    @Test
    public void ifPaymentMethodListSetWithTwoOptionsThenAskForPaymentType() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        List<PaymentMethod> paymentMethodList = PaymentMethods.getPaymentMethodListMLA();
        provider.setPaymentMethodsResponse(paymentMethodList);

        List<IdentificationType> identificationTypesList = IdentificationTypes.getIdentificationTypes();
        provider.setIdentificationTypesResponse(identificationTypesList);

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.setPublicKey("mockedPublicKey");

        presenter.initialize();

        List<PaymentMethod> mockedGuessedPaymentMethods = new ArrayList<>();
        mockedGuessedPaymentMethods.add(PaymentMethods.getPaymentMethodOnVisa());
        mockedGuessedPaymentMethods.add(PaymentMethods.getPaymentMethodOnDebit());

        presenter.resolvePaymentMethodListSet(mockedGuessedPaymentMethods, Cards.MOCKED_BIN_VISA);

        assertTrue(presenter.hasToShowPaymentTypes());

    }

    @Test
    public void ifPaymentMethodListSetWithTwoOptionsThenChooseFirstOne() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        List<PaymentMethod> paymentMethodList = PaymentMethods.getPaymentMethodListMLA();
        provider.setPaymentMethodsResponse(paymentMethodList);

        List<IdentificationType> identificationTypesList = IdentificationTypes.getIdentificationTypes();
        provider.setIdentificationTypesResponse(identificationTypesList);

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.setPublicKey("mockedPublicKey");

        presenter.initialize();

        List<PaymentMethod> mockedGuessedPaymentMethods = new ArrayList<>();
        mockedGuessedPaymentMethods.add(PaymentMethods.getPaymentMethodOnVisa());
        mockedGuessedPaymentMethods.add(PaymentMethods.getPaymentMethodOnDebit());

        presenter.resolvePaymentMethodListSet(mockedGuessedPaymentMethods, Cards.MOCKED_BIN_VISA);

        assertTrue(mockedView.cardDataViewLoaded);
        assertEquals(presenter.getPaymentMethod().getId(), mockedGuessedPaymentMethods.get(0).getId());
    }

    @Test
    public void ifPaymentMethodExclusionSetAndUserSelectsItThenShowErrorMessage() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        List<PaymentMethod> paymentMethodList = PaymentMethods.getPaymentMethodListMLA();
        provider.setPaymentMethodsResponse(paymentMethodList);

        List<IdentificationType> identificationTypesList = IdentificationTypes.getIdentificationTypes();
        provider.setIdentificationTypesResponse(identificationTypesList);

        List<String> excludedPaymentMethodIds = new ArrayList<>();
        excludedPaymentMethodIds.add("master");
        PaymentPreference paymentPreference = new PaymentPreference();
        paymentPreference.setExcludedPaymentMethodIds(excludedPaymentMethodIds);

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.setPublicKey("mockedPublicKey");
        presenter.setPaymentPreference(paymentPreference);

        presenter.initialize();

        PaymentMethodGuessingController controller = presenter.getGuessingController();
        List<PaymentMethod> guessedPaymentMethods = controller.guessPaymentMethodsByBin(Cards.MOCKED_BIN_MASTER);

        presenter.resolvePaymentMethodListSet(guessedPaymentMethods, Cards.MOCKED_BIN_MASTER);

        assertFalse(mockedView.cardDataViewLoaded);
        assertTrue(mockedView.formDataErrorState);
        assertEquals(mockedView.formDataErrorCause, MockedProvider.INVALID_PAYMENT_METHOD);
    }

    @Test
    public void ifPaymentMethodSetAndDeletedThenClearConfiguration() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        List<PaymentMethod> paymentMethodList = PaymentMethods.getPaymentMethodListMLA();
        provider.setPaymentMethodsResponse(paymentMethodList);

        List<IdentificationType> identificationTypesList = IdentificationTypes.getIdentificationTypes();
        provider.setIdentificationTypesResponse(identificationTypesList);

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.setPublicKey("mockedPublicKey");

        presenter.initialize();

        List<PaymentMethod> mockedGuessedPaymentMethods = new ArrayList<>();
        mockedGuessedPaymentMethods.add(PaymentMethods.getPaymentMethodOnVisa());

        presenter.resolvePaymentMethodListSet(mockedGuessedPaymentMethods, Cards.MOCKED_BIN_VISA);

        assertTrue(mockedView.cardDataViewLoaded);

        presenter.setPaymentMethod(null);

        assertEquals(presenter.getSecurityCodeLength(), GuessingCardPresenter.CARD_DEFAULT_SECURITY_CODE_LENGTH);
        assertEquals(presenter.getSecurityCodeLocation(), CardView.CARD_SIDE_BACK);
        assertTrue(presenter.isSecurityCodeRequired());
        assertEquals(presenter.getSavedBin().length(), 0);
    }

    @Test
    public void ifPaymentMethodSetAndDeletedThenClearViews() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        List<PaymentMethod> paymentMethodList = PaymentMethods.getPaymentMethodListMLA();
        provider.setPaymentMethodsResponse(paymentMethodList);

        List<IdentificationType> identificationTypesList = IdentificationTypes.getIdentificationTypes();
        provider.setIdentificationTypesResponse(identificationTypesList);

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.setPublicKey("mockedPublicKey");

        presenter.initialize();

        List<PaymentMethod> mockedGuessedPaymentMethods = new ArrayList<>();
        mockedGuessedPaymentMethods.add(PaymentMethods.getPaymentMethodOnVisa());

        presenter.resolvePaymentMethodListSet(mockedGuessedPaymentMethods, Cards.MOCKED_BIN_VISA);

        assertTrue(mockedView.cardDataViewLoaded);

        presenter.resolvePaymentMethodCleared();

        assertFalse(mockedView.errorState);
        assertTrue(mockedView.cardNumberLengthDefault);
        assertTrue(mockedView.cardNumberMaskDefault);
        assertTrue(mockedView.securityCodeInputErased);
        assertTrue(mockedView.clearCardView);
    }

    @Test
    public void ifPaymentMethodSetHasIdentificationTypeRequiredThenShowIdentificationView() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        List<PaymentMethod> paymentMethodList = PaymentMethods.getPaymentMethodListMLA();
        provider.setPaymentMethodsResponse(paymentMethodList);

        List<IdentificationType> identificationTypesList = IdentificationTypes.getIdentificationTypes();
        provider.setIdentificationTypesResponse(identificationTypesList);

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.setPublicKey("mockedPublicKey");

        presenter.initialize();

        List<PaymentMethod> mockedGuessedPaymentMethods = new ArrayList<>();
        mockedGuessedPaymentMethods.add(PaymentMethods.getPaymentMethodOnVisa());

        presenter.resolvePaymentMethodListSet(mockedGuessedPaymentMethods, Cards.MOCKED_BIN_VISA);

        assertTrue(mockedView.cardDataViewLoaded);
        assertTrue(presenter.isIdentificationNumberRequired());
        assertTrue(mockedView.identificationTypesInitialized);
    }

    @Test
    public void ifPaymentMethodSetDoesntHaveIdentificationTypeRequiredThenHideIdentificationView() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        List<PaymentMethod> paymentMethodList = PaymentMethods.getPaymentMethodListMLA();
        provider.setPaymentMethodsResponse(paymentMethodList);

        List<IdentificationType> identificationTypesList = IdentificationTypes.getIdentificationTypes();
        provider.setIdentificationTypesResponse(identificationTypesList);

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.setPublicKey("mockedPublicKey");

        presenter.initialize();

        List<PaymentMethod> mockedGuessedPaymentMethods = new ArrayList<>();
        mockedGuessedPaymentMethods.add(PaymentMethods.getPaymentMethodWithIdNotRequired());

        presenter.resolvePaymentMethodListSet(mockedGuessedPaymentMethods, Cards.MOCKED_BIN_CORDIAL);

        assertTrue(mockedView.cardDataViewLoaded);
        assertFalse(presenter.isIdentificationNumberRequired());
        assertFalse(mockedView.identificationTypesInitialized);
        assertTrue(mockedView.hideIdentificationInput);
    }

    @Test
    public void initializeGuessingFormWithPaymentMethodListFromCardVault() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        List<PaymentMethod> paymentMethodList = PaymentMethods.getPaymentMethodListMLA();

        List<IdentificationType> identificationTypesList = IdentificationTypes.getIdentificationTypes();
        provider.setIdentificationTypesResponse(identificationTypesList);

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.setPublicKey("mockedPublicKey");
        presenter.setPaymentMethodList(paymentMethodList);

        presenter.initialize();

        assertTrue(mockedView.showInputContainer);
        assertTrue(mockedView.initializeGuessingForm);
        assertTrue(mockedView.initializeGuessingListeners);
    }

    @Test
    public void ifBankDealsNotEnabledThenHideBankDeals() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        List<PaymentMethod> paymentMethodList = PaymentMethods.getPaymentMethodListMLA();

        List<IdentificationType> identificationTypesList = IdentificationTypes.getIdentificationTypes();
        provider.setIdentificationTypesResponse(identificationTypesList);

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.setPublicKey("mockedPublicKey");
        presenter.setPaymentMethodList(paymentMethodList);
        presenter.setShowBankDeals(false);

        presenter.initialize();

        assertTrue(mockedView.hideBankDeals);
    }

    @Test
    public void ifGetPaymentMethodFailsThenShowErrorMessage() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        ApiException apiException = PaymentMethods.getDoNotFindPaymentMethodsException();
        MercadoPagoError mpException = new MercadoPagoError(apiException);
        provider.setPaymentMethodsResponse(mpException);

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.setPublicKey("mockedPublicKey");

        presenter.initialize();

        assertTrue(provider.failedResponse.getApiException().getError().equals(MockedProvider.PAYMENT_METHODS_NOT_FOUND));
    }

    @Test
    public void ifPaymentTypeSetAndTwoPaymentMethodssThenChooseByPaymentType() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        List<PaymentMethod> paymentMethodList = PaymentMethods.getPaymentMethodListMLM();
        provider.setPaymentMethodsResponse(paymentMethodList);

        List<IdentificationType> identificationTypesList = IdentificationTypes.getIdentificationTypes();
        provider.setIdentificationTypesResponse(identificationTypesList);

        PaymentPreference paymentPreference = new PaymentPreference();
        paymentPreference.setDefaultPaymentTypeId(PaymentTypes.DEBIT_CARD);

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.setPublicKey("mockedPublicKey");
        presenter.setPaymentPreference(paymentPreference);

        presenter.initialize();

        PaymentMethodGuessingController controller = new PaymentMethodGuessingController(
                paymentMethodList, PaymentTypes.DEBIT_CARD, null);
        List<PaymentMethod> paymentMethodsWithExclusionsList = controller.guessPaymentMethodsByBin(Cards.MOCKED_BIN_MASTER);

        presenter.resolvePaymentMethodListSet(paymentMethodsWithExclusionsList, Cards.MOCKED_BIN_MASTER);

        assertEquals(paymentMethodsWithExclusionsList.size(), 1);
        assertEquals(presenter.getPaymentMethod().getId(), "debmaster");
        assertFalse(presenter.hasToShowPaymentTypes());
    }

    @Test
    public void ifSecurityCodeSettingsAreWrongThenHideSecurityCodeView() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        List<PaymentMethod> paymentMethodList = PaymentMethods.getPaymentMethodListMLA();
        provider.setPaymentMethodsResponse(paymentMethodList);

        List<IdentificationType> identificationTypesList = IdentificationTypes.getIdentificationTypes();
        provider.setIdentificationTypesResponse(identificationTypesList);

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.setPublicKey("mockedPublicKey");

        presenter.initialize();

        List<PaymentMethod> mockedGuessedPaymentMethods = new ArrayList<>();
        mockedGuessedPaymentMethods.add(PaymentMethods.getPaymentMethodWithWrongSecurityCodeSettings());

        presenter.resolvePaymentMethodListSet(mockedGuessedPaymentMethods, Cards.MOCKED_BIN_VISA);

        assertTrue(mockedView.cardDataViewLoaded);
        assertTrue(mockedView.hideSecurityCodeInput);
    }

    @Test
    public void ifPaymentMethodSettingsAreEmptyThenShowErrorMessage() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        List<PaymentMethod> paymentMethodList = PaymentMethods.getPaymentMethodListMLA();
        provider.setPaymentMethodsResponse(paymentMethodList);

        List<IdentificationType> identificationTypesList = IdentificationTypes.getIdentificationTypes();
        provider.setIdentificationTypesResponse(identificationTypesList);

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.setPublicKey("mockedPublicKey");

        presenter.initialize();

        List<PaymentMethod> mockedGuessedPaymentMethods = new ArrayList<>();
        PaymentMethod mockedPaymentMethod = PaymentMethods.getPaymentMethodOnVisa();
        mockedPaymentMethod.setSettings(null);
        mockedGuessedPaymentMethods.add(mockedPaymentMethod);

        presenter.resolvePaymentMethodListSet(mockedGuessedPaymentMethods, Cards.MOCKED_BIN_VISA);

        assertEquals(MockedProvider.SETTING_NOT_FOUND_FOR_BIN, mockedView.errorShown.getMessage());

    }

    @Test
    public void ifGetIdentificationTypesFailsThenShowErrorMessage() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        List<PaymentMethod> paymentMethodList = PaymentMethods.getPaymentMethodListMLA();
        provider.setPaymentMethodsResponse(paymentMethodList);

        ApiException apiException = IdentificationTypes.getDoNotFindIdentificationTypesException();
        MercadoPagoError mpException = new MercadoPagoError(apiException);
        provider.setIdentificationTypesResponse(mpException);

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.setPublicKey("mockedPublicKey");

        presenter.initialize();

        assertTrue(provider.failedResponse.getApiException().getError().equals(MockedProvider.IDENTIFICATION_TYPES_NOT_FOUND));
    }

    @Test
    public void ifGetIdentificationTypesIsEmptyThenShowErrorMessage() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        List<PaymentMethod> paymentMethodList = PaymentMethods.getPaymentMethodListMLA();
        provider.setPaymentMethodsResponse(paymentMethodList);

        List<IdentificationType> identificationTypes = new ArrayList<>();
        provider.setIdentificationTypesResponse(identificationTypes);

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.setPublicKey("mockedPublicKey");

        presenter.initialize();

        presenter.resolvePaymentMethodListSet(paymentMethodList, Cards.MOCKED_BIN_VISA);

        assertEquals(MockedProvider.MISSING_IDENTIFICATION_TYPES, mockedView.errorShown.getMessage());

    }

    @Test
    public void ifBankDealsNotEmptyThenShowThem() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        List<PaymentMethod> paymentMethodList = PaymentMethods.getPaymentMethodListMLA();
        provider.setPaymentMethodsResponse(paymentMethodList);

        List<IdentificationType> identificationTypes = IdentificationTypes.getIdentificationTypes();
        provider.setIdentificationTypesResponse(identificationTypes);

        List<BankDeal> bankDeals = BankDeals.getBankDealsListMLA();
        provider.setBankDealsResponse(bankDeals);

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.setPublicKey("mockedPublicKey");

        presenter.initialize();

        presenter.resolvePaymentMethodListSet(paymentMethodList, Cards.MOCKED_BIN_VISA);

        assertTrue(mockedView.bankDealsShown);

    }

    @Test
    public void ifCardNumberSetThenValidateItAndSaveItInCardToken() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        List<PaymentMethod> paymentMethodList = PaymentMethods.getPaymentMethodListMLA();
        provider.setPaymentMethodsResponse(paymentMethodList);

        List<IdentificationType> identificationTypesList = IdentificationTypes.getIdentificationTypes();
        provider.setIdentificationTypesResponse(identificationTypesList);

        PaymentMethod mockedPaymentMethod = PaymentMethods.getPaymentMethodOnMaster();

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.setPublicKey("mockedPublicKey");

        presenter.initialize();

        DummyCard card = CardTestUtils.getDummyCard("master");
        presenter.saveCardNumber(card.getCardNumber());
        presenter.setPaymentMethod(mockedPaymentMethod);

        boolean valid = presenter.validateCardNumber();

        assertTrue(valid);
        assertEquals(presenter.getCardToken().getCardNumber(), card.getCardNumber());
    }

    @Test
    public void ifCardholderNameSetThenValidateItAndSaveItInCardToken() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        List<PaymentMethod> paymentMethodList = PaymentMethods.getPaymentMethodListMLA();
        provider.setPaymentMethodsResponse(paymentMethodList);

        List<IdentificationType> identificationTypesList = IdentificationTypes.getIdentificationTypes();
        provider.setIdentificationTypesResponse(identificationTypesList);

        PaymentMethod mockedPaymentMethod = PaymentMethods.getPaymentMethodOnMaster();

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.setPublicKey("mockedPublicKey");

        presenter.initialize();

        DummyCard card = CardTestUtils.getDummyCard("master");
        presenter.saveCardNumber(card.getCardNumber());
        presenter.setPaymentMethod(mockedPaymentMethod);
        presenter.saveCardholderName(CardTestUtils.DUMMY_CARDHOLDER_NAME);

        boolean valid = presenter.validateCardName();

        assertTrue(valid);
        assertEquals(presenter.getCardToken().getCardholder().getName(), CardTestUtils.DUMMY_CARDHOLDER_NAME);
    }

    @Test
    public void ifCardExpiryDateSetThenValidateItAndSaveItInCardToken() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        List<PaymentMethod> paymentMethodList = PaymentMethods.getPaymentMethodListMLA();
        provider.setPaymentMethodsResponse(paymentMethodList);

        List<IdentificationType> identificationTypesList = IdentificationTypes.getIdentificationTypes();
        provider.setIdentificationTypesResponse(identificationTypesList);

        PaymentMethod mockedPaymentMethod = PaymentMethods.getPaymentMethodOnMaster();

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.setPublicKey("mockedPublicKey");

        presenter.initialize();

        DummyCard card = CardTestUtils.getDummyCard("master");
        presenter.saveCardNumber(card.getCardNumber());
        presenter.setPaymentMethod(mockedPaymentMethod);
        presenter.saveCardholderName(CardTestUtils.DUMMY_CARDHOLDER_NAME);
        presenter.saveExpiryMonth(CardTestUtils.DUMMY_EXPIRY_MONTH);
        presenter.saveExpiryYear(CardTestUtils.DUMMY_EXPIRY_YEAR_SHORT);

        boolean valid = presenter.validateExpiryDate();

        assertTrue(valid);
        assertEquals(presenter.getCardToken().getExpirationMonth(), Integer.valueOf(CardTestUtils.DUMMY_EXPIRY_MONTH));
        assertEquals(presenter.getCardToken().getExpirationYear(), Integer.valueOf(CardTestUtils.DUMMY_EXPIRY_YEAR_LONG));
    }

    @Test
    public void ifCardSecurityCodeSetThenValidateItAndSaveItInCardToken() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        List<PaymentMethod> paymentMethodList = PaymentMethods.getPaymentMethodListMLA();
        provider.setPaymentMethodsResponse(paymentMethodList);

        List<IdentificationType> identificationTypesList = IdentificationTypes.getIdentificationTypes();
        provider.setIdentificationTypesResponse(identificationTypesList);

        PaymentMethod mockedPaymentMethod = PaymentMethods.getPaymentMethodOnMaster();

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.setPublicKey("mockedPublicKey");

        presenter.initialize();

        DummyCard card = CardTestUtils.getDummyCard("master");
        presenter.saveCardNumber(card.getCardNumber());
        presenter.setPaymentMethod(mockedPaymentMethod);
        presenter.saveCardholderName(CardTestUtils.DUMMY_CARDHOLDER_NAME);
        presenter.saveExpiryMonth(CardTestUtils.DUMMY_EXPIRY_MONTH);
        presenter.saveExpiryYear(CardTestUtils.DUMMY_EXPIRY_YEAR_SHORT);
        presenter.saveSecurityCode(card.getSecurityCode());

        boolean validCardNumber = presenter.validateCardNumber();
        boolean validSecurityCode = presenter.validateSecurityCode();

        assertTrue(validCardNumber && validSecurityCode);
        assertEquals(presenter.getCardToken().getSecurityCode(), card.getSecurityCode());
    }

    @Test
    public void ifIdentificationNumberSetThenValidateItAndSaveItInCardToken() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        List<PaymentMethod> paymentMethodList = PaymentMethods.getPaymentMethodListMLA();
        provider.setPaymentMethodsResponse(paymentMethodList);

        List<IdentificationType> identificationTypesList = IdentificationTypes.getIdentificationTypes();
        provider.setIdentificationTypesResponse(identificationTypesList);

        PaymentMethod mockedPaymentMethod = PaymentMethods.getPaymentMethodOnMaster();

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.setPublicKey("mockedPublicKey");

        presenter.initialize();

        DummyCard card = CardTestUtils.getDummyCard("master");
        presenter.saveCardNumber(card.getCardNumber());
        presenter.setPaymentMethod(mockedPaymentMethod);
        presenter.saveCardholderName(CardTestUtils.DUMMY_CARDHOLDER_NAME);
        presenter.saveExpiryMonth(CardTestUtils.DUMMY_EXPIRY_MONTH);
        presenter.saveExpiryYear(CardTestUtils.DUMMY_EXPIRY_YEAR_SHORT);
        presenter.saveSecurityCode(card.getSecurityCode());
        presenter.saveIdentificationNumber(CardTestUtils.DUMMY_IDENTIFICATION_NUMBER_DNI);
        presenter.saveIdentificationType(IdentificationTypes.getIdentificationType());

        boolean valid = presenter.validateIdentificationNumber();

        assertTrue(valid);
        assertEquals(presenter.getCardToken().getCardholder().getIdentification().getNumber(), CardTestUtils.DUMMY_IDENTIFICATION_NUMBER_DNI);
    }

    @Test
    public void ifCardDataSetAndValidThenCreateToken() {
        MockedView mockedView = new MockedView();
        MockedProvider provider = new MockedProvider();

        List<PaymentMethod> paymentMethodList = PaymentMethods.getPaymentMethodListMLA();
        provider.setPaymentMethodsResponse(paymentMethodList);

        List<IdentificationType> identificationTypesList = IdentificationTypes.getIdentificationTypes();
        provider.setIdentificationTypesResponse(identificationTypesList);

        PaymentMethod mockedPaymentMethod = PaymentMethods.getPaymentMethodOnMaster();

        Token mockedtoken = Tokens.getToken();
        provider.setTokenResponse(mockedtoken);

        GuessingCardPresenter presenter = new GuessingCardPresenter();
        presenter.attachView(mockedView);
        presenter.attachResourcesProvider(provider);

        presenter.setPublicKey("mockedPublicKey");

        presenter.initialize();

        DummyCard card = CardTestUtils.getDummyCard("master");
        presenter.saveCardNumber(card.getCardNumber());
        presenter.setPaymentMethod(mockedPaymentMethod);
        presenter.saveCardholderName(CardTestUtils.DUMMY_CARDHOLDER_NAME);
        presenter.saveExpiryMonth(CardTestUtils.DUMMY_EXPIRY_MONTH);
        presenter.saveExpiryYear(CardTestUtils.DUMMY_EXPIRY_YEAR_SHORT);
        presenter.saveSecurityCode(card.getSecurityCode());
        presenter.saveIdentificationNumber(CardTestUtils.DUMMY_IDENTIFICATION_NUMBER_DNI);
        presenter.saveIdentificationType(IdentificationTypes.getIdentificationType());

        boolean valid = presenter.validateCardNumber();
        valid = valid & presenter.validateCardName();
        valid = valid & presenter.validateExpiryDate();
        valid = valid & presenter.validateSecurityCode();
        valid = valid & presenter.validateIdentificationNumber();

        assertTrue(valid);

        presenter.checkFinishWithCardToken();

        presenter.resolveTokenRequest(mockedtoken);

        assertTrue(mockedView.issuerFlowStarted);
        assertEquals(presenter.getToken(), mockedtoken);

    }

    private class MockedProvider implements GuessingCardProvider {

        private static final String MULTIPLE_INSTALLMENTS = "multiple installments";
        private static final String MISSING_INSTALLMENTS = "missing installments";
        private static final String MISSING_PAYER_COSTS = "missing payer costs";
        private static final String MISSING_PUBLIC_KEY = "missing public key";
        private static final String MISSING_IDENTIFICATION_TYPES = "missing identification types";
        private static final String INVALID_IDENTIFICATION_NUMBER = "invalid identification number";
        private static final String INVALID_EMPTY_NAME = "invalid empty name";
        private static final String INVALID_PAYMENT_METHOD = "invalid payment method";
        private static final String INVALID_EXPIRY_DATE = "invalid expiry date";
        private static final String SETTING_NOT_FOUND_FOR_BIN = "setting not found for bin";
        private static final String PAYMENT_METHODS_NOT_FOUND = "payment methods not found error";
        private static final String IDENTIFICATION_TYPES_NOT_FOUND = "identification types not found error";

        private boolean shouldFail;
        private MercadoPagoError failedResponse;
        private List<Installment> successfulInstallmentsResponse;
        private List<IdentificationType> successfulIdentificationTypesResponse;
        private List<BankDeal> successfulBankDealsResponse;
        private Token successfulTokenResponse;
        private List<Issuer> successfulIssuersResponse;
        private Discount successfulDiscountResponse;
        private List<PaymentMethod> successfulPaymentMethodsResponse;


        public void setResponse(MercadoPagoError exception) {
            shouldFail = true;
            failedResponse = exception;
        }

        public void setInstallmentsResponse(List<Installment> installmentList) {
            shouldFail = false;
            successfulInstallmentsResponse = installmentList;
        }

        public void setIdentificationTypesResponse(List<IdentificationType> identificationTypes) {
            shouldFail = false;
            successfulIdentificationTypesResponse = identificationTypes;
        }

        public void setIdentificationTypesResponse(MercadoPagoError exception) {
            shouldFail = true;
            failedResponse = exception;
        }

        public void setBankDealsResponse(List<BankDeal> bankDeals) {
            shouldFail = false;
            successfulBankDealsResponse = bankDeals;
        }

        public void setTokenResponse(Token token) {
            shouldFail = false;
            successfulTokenResponse = token;
        }

        public void setIssuersResponse(List<Issuer> issuers) {
            shouldFail = false;
            successfulIssuersResponse = issuers;
        }

        public void setDiscountResponse(Discount discount) {
            shouldFail = false;
            successfulDiscountResponse = discount;
        }

        public void setPaymentMethodsResponse(List<PaymentMethod> paymentMethods) {
            shouldFail = false;
            successfulPaymentMethodsResponse = paymentMethods;
        }

        public void setPaymentMethodsResponse(MercadoPagoError exception) {
            shouldFail = true;
            failedResponse = exception;
        }

        @Override
        public String getMissingIdentificationTypesErrorMessage() {
            return MISSING_IDENTIFICATION_TYPES;
        }

        @Override
        public String getInvalidIdentificationNumberErrorMessage() {
            return INVALID_IDENTIFICATION_NUMBER;
        }

        @Override
        public String getInvalidEmptyNameErrorMessage() {
            return INVALID_EMPTY_NAME;
        }

        @Override
        public String getMissingPayerCostsErrorMessage() {
            return MISSING_PAYER_COSTS;
        }

        @Override
        public String getMissingInstallmentsForIssuerErrorMessage() {
            return MISSING_INSTALLMENTS;
        }

        @Override
        public String getInvalidPaymentMethodErrorMessage() {
            return INVALID_PAYMENT_METHOD;
        }

        @Override
        public String getInvalidExpiryDateErrorMessage() {
            return INVALID_EXPIRY_DATE;
        }

        @Override
        public String getMultipleInstallmentsForIssuerErrorMessage() {
            return MULTIPLE_INSTALLMENTS;
        }

        @Override
        public String getSettingNotFoundForBinErrorMessage() {
            return SETTING_NOT_FOUND_FOR_BIN;
        }

        @Override
        public String getMissingPublicKeyErrorMessage() {
            return MISSING_PUBLIC_KEY;
        }

        @Override
        public void getInstallmentsAsync(String bin, BigDecimal amount, Long issuerId, String paymentMethodId, OnResourcesRetrievedCallback<List<Installment>> onResourcesRetrievedCallback) {
            if (shouldFail) {
                onResourcesRetrievedCallback.onFailure(failedResponse);
            } else {
                onResourcesRetrievedCallback.onSuccess(successfulInstallmentsResponse);
            }
        }

        @Override
        public void getIdentificationTypesAsync(OnResourcesRetrievedCallback<List<IdentificationType>> onResourcesRetrievedCallback) {
            if (shouldFail) {
                onResourcesRetrievedCallback.onFailure(failedResponse);
            } else {
                onResourcesRetrievedCallback.onSuccess(successfulIdentificationTypesResponse);
            }
        }

        @Override
        public void getBankDealsAsync(OnResourcesRetrievedCallback<List<BankDeal>> onResourcesRetrievedCallback) {
            if (shouldFail) {
                onResourcesRetrievedCallback.onFailure(failedResponse);
            } else {
                onResourcesRetrievedCallback.onSuccess(successfulBankDealsResponse);
            }
        }

        @Override
        public void createTokenAsync(CardToken cardToken, Device device, OnResourcesRetrievedCallback<Token> onResourcesRetrievedCallback) {
            if (shouldFail) {
                onResourcesRetrievedCallback.onFailure(failedResponse);
            } else {
                onResourcesRetrievedCallback.onSuccess(successfulTokenResponse);
            }
        }

        @Override
        public void getIssuersAsync(String paymentMethodId, String bin, OnResourcesRetrievedCallback<List<Issuer>> onResourcesRetrievedCallback) {
            if (shouldFail) {
                onResourcesRetrievedCallback.onFailure(failedResponse);
            } else {
                onResourcesRetrievedCallback.onSuccess(successfulIssuersResponse);
            }
        }

        @Override
        public void getDirectDiscountAsync(String transactionAmount, String payerEmail, String merchantDiscountUrl, String merchantDiscountUri, Map<String, String> discountAdditionalInfo, OnResourcesRetrievedCallback<Discount> onResourcesRetrievedCallback) {
            if (shouldFail) {
                onResourcesRetrievedCallback.onFailure(failedResponse);
            } else {
                onResourcesRetrievedCallback.onSuccess(successfulDiscountResponse);
            }
        }

        @Override
        public void getPaymentMethodsAsync(OnResourcesRetrievedCallback<List<PaymentMethod>> onResourcesRetrievedCallback) {
            if (shouldFail) {
                onResourcesRetrievedCallback.onFailure(failedResponse);
            } else {
                onResourcesRetrievedCallback.onSuccess(successfulPaymentMethodsResponse);
            }
        }


    }

    private class MockedView implements GuessingCardView {

        private MercadoPagoError errorShown;
        private CardTokenException cardTokenError;
        private List<Installment> installmentsShown;
        private String formDataErrorCause;
        private boolean formDataErrorState;
        private boolean errorState;
        private boolean validStart;
        private boolean cardDataViewLoaded;
        private boolean cardNumberLengthDefault;
        private boolean cardNumberMaskDefault;
        private boolean securityCodeInputErased;
        private boolean clearCardView;
        private boolean identificationTypesInitialized;
        private boolean hideIdentificationInput;
        private boolean showInputContainer;
        private boolean initializeGuessingForm;
        private boolean initializeGuessingListeners;
        private boolean hideBankDeals;
        private boolean hideSecurityCodeInput;
        private boolean bankDealsShown;
        private boolean issuerFlowStarted;
        private boolean paymentTypeFlowStarted;
        private String savedCardholderName;
        private String savedIdentificationNumber;

        @Override
        public void clearSecurityCodeEditText() {
            securityCodeInputErased = true;
        }

        @Override
        public void clearCardNumberEditTextMask() {
            cardNumberMaskDefault = true;
        }

        @Override
        public void clearErrorIdentificationNumber() {

        }

        @Override
        public void clearCardNumberInputLength() {
            cardNumberLengthDefault = true;
        }

        @Override
        public void clearErrorView() {
            errorState = false;
        }

        @Override
        public void eraseDefaultSpace() {

        }

        @Override
        public void loadViews() {
            validStart = true;
        }

        @Override
        public void loadCardViewData(PaymentMethod paymentMethod, Integer cardNumberLength, int securityCodeLength, String securityCodeLocation) {
            cardDataViewLoaded = true;
        }

        @Override
        public void decorate() {

        }

        @Override
        public void checkClearCardView() {
            clearCardView = true;
        }

        @Override
        public void setBackButtonListeners() {
            initializeGuessingListeners = true;
        }

        @Override
        public void setCardNumberListeners(PaymentMethodGuessingController controller) {
            initializeGuessingListeners = true;
        }

        @Override
        public void setErrorSecurityCode() {

        }

        @Override
        public void setErrorCardNumber() {

        }

        @Override
        public void setErrorView(CardTokenException exception) {
            formDataErrorState = true;
            cardTokenError = exception;
        }

        @Override
        public void setErrorView(String mErrorState) {
            formDataErrorState = true;
            formDataErrorCause = mErrorState;
        }

        @Override
        public void setSecurityCodeListeners() {
            initializeGuessingListeners = true;
        }

        @Override
        public void setIdentificationTypeListeners() {
            initializeGuessingListeners = true;
        }

        @Override
        public void setNextButtonListeners() {
            initializeGuessingListeners = true;
        }

        @Override
        public void setIdentificationNumberListeners() {
            initializeGuessingListeners = true;
        }

        @Override
        public void setSecurityCodeInputMaxLength(int length) {

        }

        @Override
        public void setSecurityCodeViewLocation(String location) {

        }

        @Override
        public void setIdentificationNumberRestrictions(String type) {

        }

        @Override
        public void setCardholderNameListeners() {
            initializeGuessingListeners = true;
        }

        @Override
        public void setExpiryDateListeners() {
            initializeGuessingListeners = true;
        }

        @Override
        public void setCardholderName(String cardholderName) {
            this.savedCardholderName = cardholderName;
        }

        @Override
        public void setNormalState() {

        }

        @Override
        public void setCardNumberInputMaxLength(int length) {

        }

        @Override
        public void setErrorCardholderName() {

        }

        @Override
        public void setErrorExpiryDate() {

        }

        @Override
        public void setErrorIdentificationNumber() {

        }

        @Override
        public void setIdentificationNumber(String identificationNumber) {
            this.savedIdentificationNumber = identificationNumber;
        }

        @Override
        public void showDiscountRow(BigDecimal transactionAmount) {

        }

        @Override
        public void showIdentificationInput() {

        }

        @Override
        public void showSecurityCodeInput() {

        }

        @Override
        public void showInputContainer() {
            showInputContainer = true;
        }

        @Override
        public void showError(MercadoPagoError mercadoPagoError) {
            errorShown = mercadoPagoError;
            errorState = true;
        }

        @Override
        public void showBankDeals() {
            bankDealsShown = true;
        }

        @Override
        public void showApiExceptionError(ApiException exception) {

        }

        @Override
        public void hideBankDeals() {
            hideBankDeals = true;
        }

        @Override
        public void hideIdentificationInput() {
            hideIdentificationInput = true;
        }

        @Override
        public void hideSecurityCodeInput() {
            hideSecurityCodeInput = true;
        }

        @Override
        public void initializeTimer() {

        }

        @Override
        public void initializeIdentificationTypes(List<IdentificationType> identificationTypes) {
            identificationTypesInitialized = true;
        }

        @Override
        public void initializeTitle() {
            initializeGuessingForm = true;
        }

        @Override
        public void startDiscountActivity(BigDecimal transactionAmount) {

        }

        @Override
        public void finishCardFlow(PaymentMethod paymentMethod, Token token, Discount discount, Boolean directDiscountEnabled, Issuer issuer, PayerCost payerCost) {

        }

        @Override
        public void finishCardFlow(PaymentMethod paymentMethod, Token token, Discount discount, Boolean directDiscountEnabled, Issuer issuer, List<PayerCost> payerCosts) {

        }

        @Override
        public void finishCardFlow(PaymentMethod paymentMethod, Token token, Discount discount, Boolean directDiscountEnabled, List<Issuer> issuers) {

        }

        @Override
        public void askForIssuers() {
            issuerFlowStarted = true;
        }

        @Override
        public void askForPaymentType() {
            paymentTypeFlowStarted = true;
        }

        @Override
        public void showFinishCardFlow() {

        }

    }
}
