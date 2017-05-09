package com.mercadopago.mocks;

import com.google.gson.reflect.TypeToken;
import com.mercadopago.model.ApiException;
import com.mercadopago.model.IdentificationType;
import com.mercadopago.util.JsonUtil;
import com.mercadopago.utils.ResourcesUtil;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by vaserber on 5/9/17.
 */

public class IdentificationTypes {

    private static String doNotFindIdentificationTypesException = "{\"message\":\"doesn't find identification types\",\"error\":\"identification types not found error\",\"cause\":[]}";

    private IdentificationTypes() {

    }

    public static List<IdentificationType> getIdentificationTypes() {
        List<IdentificationType> identificationTypesList;
        String json = ResourcesUtil.getStringResource("identification_types.json");

        try {
            Type listType = new TypeToken<List<IdentificationType>>() {
            }.getType();
            identificationTypesList = JsonUtil.getInstance().getGson().fromJson(json, listType);
        } catch (Exception ex) {
            identificationTypesList = null;
        }
        return identificationTypesList;
    }

    public static IdentificationType getIdentificationType() {
        List<IdentificationType> identificationTypesList;
        String json = ResourcesUtil.getStringResource("identification_types.json");

        try {
            Type listType = new TypeToken<List<IdentificationType>>() {
            }.getType();
            identificationTypesList = JsonUtil.getInstance().getGson().fromJson(json, listType);
        } catch (Exception ex) {
            identificationTypesList = null;
        }
        return identificationTypesList.get(0);
    }

    public static ApiException getDoNotFindIdentificationTypesException() {
        return JsonUtil.getInstance().fromJson(doNotFindIdentificationTypesException, ApiException.class);
    }
}
