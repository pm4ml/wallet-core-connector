package com.modusbox.client.processor;

import com.modusbox.log4j2.message.CustomJsonMessage;
import com.modusbox.log4j2.message.CustomJsonMessageImpl;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

@Component("customErrorProcessor")
public class CustomErrorProcessor implements Processor {

    CustomJsonMessage customJsonMessage = new CustomJsonMessageImpl();

    @Override
    public void process(Exchange exchange) throws Exception {

        String reasonText = "{ \"statusCode\": \"5000\"," +
                "\"message\": \"Unknown\" }";
        String statusCode = "5000";
        int httpResponseCode = 500;

        // The exception may be in 1 of 2 places
        Exception exception = exchange.getException();
        if (exception == null) {
            exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        }

        if (exception != null) {
            if (exception instanceof HttpOperationFailedException) {
                HttpOperationFailedException e = (HttpOperationFailedException) exception;
                httpResponseCode = e.getStatusCode();
                String errorMessage = "Downstream API failed.";
                String detailedDescription = "Unknown";
                try {
                    if (null != e.getResponseBody()) {
                        /* Below if block needs to be changed as per the error object structure specific to 
                            CBS back end API that is being integrated in Core Connector. */

                        customJsonMessage.logJsonMessage("error", String.valueOf(exchange.getIn().getHeader("X-CorrelationId")),
                                "Logging entire error object...", null, null,
                                e.getResponseBody());

                        JSONObject respObject = new JSONObject(e.getResponseBody());
                        if (respObject.has("message")) {
//                            statusCode = String.valueOf(respObject.getInt("returnCode"));
//                            errorDescription = respObject.getString("returnStatus");
                            statusCode = String.valueOf(respObject.getInt("statusCode"));
//                            statusCode = respObject.getString("statusCode");\
                            // Replace 2 or more whitespace chars with just one
                            detailedDescription = respObject.getString("message").replaceAll("\\s+", " ");
                            try {
                                errorMessage = respObject.getJSONObject("transferState").getJSONObject("lastError").getJSONObject("mojaloopError").getJSONObject("errorInformation").getString("errorDescription");
                            } catch (JSONException ex) {
//                                ex.printStackTrace();
                                errorMessage = "Unknown - no mojaloopError message present";
                            }
                        }
                    }
                } finally {
                    reasonText = "{" +
                            "\"statusCode\": \"" + statusCode + "\"," +
                            "\"message\": \"" + errorMessage + "\"," +
                            "\"detailedDescription\": \"" + detailedDescription + "\"" +
                            "}";
                }
            }
            customJsonMessage.logJsonMessage("error", String.valueOf(exchange.getIn().getHeader("X-CorrelationId")),
                    "Processing the exception at CustomErrorProcessor", null, null,
                    exception.getMessage());
        }

        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, httpResponseCode);
        exchange.getMessage().setHeader(Exchange.CONTENT_TYPE, "application/json");
        exchange.getMessage().setBody(reasonText);
    }
}