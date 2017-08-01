package com.mercadopago.px_tracking.utils;



/**
 * Created by vaserber on 6/5/17.
 */

public class TrackingUtil {

    //Screen IDs
    public static final String SCREEN_ID_CHECKOUT = "/checkout_off/init";
    public static final String SCREEN_ID_PAYMENT_VAULT = "/checkout_off/payment_option";
    public static final String SCREEN_ID_PAYMENT_VAULT_TICKET = "/checkout_off/payment_option/ticket";
    public static final String SCREEN_ID_PAYMENT_VAULT_BANK_TRANSFER = "/checkout_off/payment_option/bank_transfer";
    public static final String SCREEN_ID_PAYMENT_VAULT_CARDS = "/checkout_off/payment_option/card";
    public static final String SCREEN_ID_REVIEW_AND_CONFIRM = "/checkout_off/review";
    public static final String SCREEN_ID_PAYMENT_RESULT_APPROVED = "/checkout_off/congrats/approved";
    public static final String SCREEN_ID_PAYMENT_RESULT_PENDING = "/checkout_off/congrats/pending";
    public static final String SCREEN_ID_PAYMENT_RESULT_REJECTED = "/checkout_off/congrats/rejected";
    public static final String SCREEN_ID_PAYMENT_RESULT_INSTRUCTIONS = "/checkout_off/congrats/instructions";
    public static final String SCREEN_ID_BANK_DEALS = "/checkout_off/bank_deals";
    public static final String SCREEN_ID_CARD_FORM = "/checkout_off/card/";

    public static final String SCREEN_ID_ERROR = "/checkout_off/failure";

    //Screen Names
    public static final String SCREEN_NAME_CHECKOUT = "Init checkout";
    public static final String SCREEN_NAME_PAYMENT_VAULT = "Payment method selection";
    public static final String SCREEN_NAME_PAYMENT_VAULT_TICKET = "Payment method selection Ticket";
    public static final String SCREEN_NAME_PAYMENT_VAULT_BANK_TRANSFER = "Payment method selection Bank Transfer";
    public static final String SCREEN_NAME_PAYMENT_VAULT_CARDS = "Payment method selection Cards";
    public static final String SCREEN_NAME_REVIEW_AND_CONFIRM = "Review and confirm";
    public static final String SCREEN_NAME_PAYMENT_RESULT_APPROVED = "Payment Approved";
    public static final String SCREEN_NAME_PAYMENT_RESULT_PENDING = "Payment Pending";
    public static final String SCREEN_NAME_PAYMENT_RESULT_REJECTED = "Payment Rejected";
    public static final String SCREEN_NAME_PAYMENT_RESULT_INSTRUCTIONS = "Payment Instructions";
    public static final String SCREEN_NAME_BANK_DEALS = "Bank Deals";
    public static final String SCREEN_NAME_CARD_FORM = "Card Form";


    public static final String SCREEN_NAME_ERROR = "Error View";

    //Sufix
    public static final String CARD_NUMBER = "/number";
    public static final String CARD_HOLDER_NAME = "/name";
    public static final String CARD_EXPIRATION_DATE = "/expiration";
    public static final String CARD_SECURITY_CODE = "/cvv";
    public static final String CARD_IDENTIFICATION = "/identification";

    public static final String CARD_NUMBER_NAME = "Number";
    public static final String CARD_HOLDER_NAME_NAME = "CardholderName";
    public static final String CARD_EXPIRATION_DATE_NAME = "Expiration date";
    public static final String CARD_SECURITY_CODE_NAME = "Cvv";
    public static final String CARD_IDENTIFICATION_NAME = "Identification number";


    //Payment Vault Group Ids
    public static final String GROUP_TICKET = "ticket";
    public static final String GROUP_BANK_TRANSFER = "bank_transfer";
    public static final String GROUP_CARDS = "cards";

    //Additional Info Keys
    public static final String ADDITIONAL_PAYMENT_METHOD_ID = "payment_method";
    public static final String ADDITIONAL_PAYMENT_TYPE_ID = "payment_type";
    public static final String ADDITIONAL_ISSUER_ID = "issuer";
    public static final String ADDITIONAL_SHIPPING_INFO = "has_shipping";
    public static final String ADDITIONAL_PAYMENT_STATUS = "payment_status";
    public static final String ADDITIONAL_PAYMENT_ID = "payment_id";
    public static final String ADDITIONAL_PAYMENT_STATUS_DETAIL = "payment_status_detail";
    public static final String ADDITIONAL_PAYMENT_IS_EXPRESS = "is_express";

    //Default values
    public static final String HAS_SHIPPING_DEFAULT_VALUE = "false";
    public static final String IS_EXPRESS_DEFAULT_VALUE = "false";
}
