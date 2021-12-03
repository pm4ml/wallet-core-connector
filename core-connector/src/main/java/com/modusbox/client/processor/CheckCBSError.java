package com.modusbox.client.processor;

import com.google.gson.Gson;
import com.modusbox.client.customexception.CCCustomException;
import com.modusbox.client.enums.ErrorCode;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;

public class CheckCBSError implements Processor {

    public void process(Exchange exchange) throws Exception {
        Gson gson = new Gson();
        String s = gson.toJson(exchange.getIn().getBody(), LinkedHashMap.class);
        JSONObject respObject = new JSONObject(s);
        int errorCode = 0;

        try {
            errorCode = respObject.getInt("err");
//          respObject.getString("message");
            if (errorCode == 8057 || errorCode == 9052) {
                throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_ID_NOT_FOUND, "Account is invalid or does not exist."));
            }
            else if (errorCode == 8031) {
                throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE, "Account is locked or inactive"));
            }
            else if (errorCode == 9051) {
                throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYER, "Sender is invalid"));
            }
            else if (errorCode == 9052) {
                throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE, "Receiver is invalid"));
            }
            else if (errorCode == 9053) {
                throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE, "Service is not in offer of user"));
            }
            else if (errorCode == 9061) {
                throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYER, "Sender balance not sufficient"));
            }
            else if (errorCode == 9056) {
                throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYER, "Sender pocket balance error"));
            }
            else if (errorCode == 9057) {
                throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE, "Receiver pocket balance error"));
            }
            else if (errorCode == 9059) {
                throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.PAYEE_LIMIT_ERROR, "Amount too small"));
            }
            else if (errorCode == 9060) {
                throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.PAYEE_LIMIT_ERROR, "Amount too large"));
            }
            else if (errorCode == 9063) {
                throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.PAYEE_LIMIT_ERROR, "Amount exceeded daily limit"));
            }
            else if (errorCode == 9064) {
                throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.PAYEE_LIMIT_ERROR, "Amount exceeded monthly limit"));
            }
            else if (errorCode == 9067) {
                throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.PAYEE_LIMIT_ERROR, "Receiver balance exceeded limit"));
            }
            else {
                throw new CCCustomException(ErrorCode.getErrorResponse(ErrorCode.GENERIC_DOWNSTREAM_ERROR_PAYEE, "Payee CBS failed with error code: " + errorCode));
            }
        } catch (JSONException e) {
            System.out.println("Problem extracting error code from CBS response occurred.");
        }

    }

}