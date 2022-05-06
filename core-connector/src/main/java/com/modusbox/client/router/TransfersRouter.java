package com.modusbox.client.router;

import com.modusbox.client.customexception.CCCustomException;
import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.json.JSONException;

public class TransfersRouter extends RouteBuilder {

    private final RouteExceptionHandlingConfigurer exception = new RouteExceptionHandlingConfigurer();

    private static final String ROUTE_ID_POST = "com.modusbox.postTransfers";
    private static final String ROUTE_ID_PUT = "com.modusbox.putTransfersByTransferId";
    private static final String ROUTE_ID_GET = "com.modusbox.getTransfersByTransferId";
    private static final String COUNTER_NAME_POST = "counter_post_transfers_requests";
    private static final String COUNTER_NAME_PUT = "counter_put_transfers_requests";
    private static final String COUNTER_NAME_GET = "counter_get_transfers_requests";
    private static final String TIMER_NAME_POST = "histogram_post_transfers_timer";
    private static final String TIMER_NAME_PUT = "histogram_put_transfers_timer";
    private static final String TIMER_NAME_GET = "histogram_get_transfers_timer";
    private static final String HISTOGRAM_NAME_POST = "histogram_post_transfers_requests_latency";
    private static final String HISTOGRAM_NAME_PUT = "histogram_put_transfers_requests_latency";
    private static final String HISTOGRAM_NAME_GET = "histogram_get_transfers_requests_latency";

    public static final Counter requestCounter = Counter.build()
            .name(COUNTER_NAME_POST)
            .help("Total requests for POST /transfers.")
            .register();

    private static final Histogram requestLatency = Histogram.build()
            .name(HISTOGRAM_NAME_POST)
            .help("Request latency in seconds for POST /transfers.")
            .register();

    public static final Counter requestCounterPut = Counter.build()
            .name(COUNTER_NAME_PUT)
            .help("Total requests for PUT /transfers/{transferId}.")
            .register();

    private static final Histogram requestLatencyPut = Histogram.build()
            .name(HISTOGRAM_NAME_PUT)
            .help("Request latency in seconds for PUT /transfers/{transferId}.")
            .register();

    public static final Counter requestCounterGet = Counter.build()
            .name(COUNTER_NAME_GET)
            .help("Total requests for GET /transfers/{transferId}.")
            .register();

    private static final Histogram requestLatencyGet = Histogram.build()
            .name(HISTOGRAM_NAME_GET)
            .help("Request latency in seconds for GET /transfers/{transferId}.")
            .register();

    public void configure() {

        // Add custom global exception handling strategy
        exception.configureExceptionHandling(this);

        from("direct:getTransfersByTransferId").routeId(ROUTE_ID_GET).doTry()
                .process(exchange -> {
                    requestCounterGet.inc(1); // increment Prometheus Counter metric
                    exchange.setProperty(TIMER_NAME_GET, requestLatencyGet.startTimer()); // initiate Prometheus Histogram metric
                })
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Request received, GET /transfers/${header.transferId}', " +
                        "null, null, null)")
                /*
                 * BEGIN processing
                 */

                .removeHeaders("CamelHttp*")
                .setHeader("Content-Type", constant("application/json"))
                .setHeader("Accept", constant("application/json"))
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))

                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Calling Hub API, get transfers, GET {{outbound.endpoint}}', " +
                        "'Tracking the request', 'Track the response', 'Input Payload: ${body}')")
                .toD("{{outbound.endpoint}}/transfers/${header.transferId}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .unmarshal().json(JsonLibrary.Gson)
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Response from Hub API, get transfers: ${body}', " +
                        "'Tracking the response', 'Verify the response', null)")
//                .process(exchange -> System.out.println())

                .choice()
                .when(simple("${body['statusCode']} != null"))
//                .process(exchange -> System.out.println())
                .to("direct:catchMojaloopError")
                .endDoTry()

//                .process(exchange -> System.out.println())

                .choice()
                .when(simple("${body['fulfil']} != null"))
//                .process(exchange -> System.out.println())
                .marshal().json()
                .transform(datasonnet("resource:classpath:mappings/getTransfersResponse.ds"))
                .setBody(simple("${body.content}"))
                .marshal().json()
                .endDoTry()

                /*
                 * END processing
                 */
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Final Response: ${body}', " +
                        "null, null, 'Response of GET /transfers/${header.transferId} API')")

                .doCatch(CCCustomException.class, HttpOperationFailedException.class, JSONException.class)
                .to("direct:extractCustomErrors")
                .doFinally().process(exchange -> {
                    ((Histogram.Timer) exchange.getProperty(TIMER_NAME_GET)).observeDuration(); // stop Prometheus Histogram metric
                }).end()
        ;

        from("direct:postTransfers").routeId(ROUTE_ID_POST).doTry()
                .process(exchange -> {
                    requestCounter.inc(1); // increment Prometheus Counter metric
                    exchange.setProperty(TIMER_NAME_POST, requestLatency.startTimer()); // initiate Prometheus Histogram metric
                })
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Request received, " + ROUTE_ID_POST + "', null, null, 'Input Payload: ${body}')") // default logging
                /*
                 * BEGIN processing
                 */
                .setProperty("origPayload", simple("${body}"))
                .removeHeaders("CamelHttp*")
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("Content-Type", constant("application/json"))
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Calling backend API, postTransfers, POST {{backend.endpoint}}', " +
                        "'Tracking the request', 'Track the response', 'Input Payload: ${body}')")
                .marshal().json(JsonLibrary.Gson)
                .toD("{{backend.endpoint}}/transfers?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .unmarshal().json(JsonLibrary.Gson)
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Response from backend API, postTransfers: ${body}', " +
                        "'Tracking the response', 'Verify the response', null)")
                /*
                 * END processing
                 */
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Send response, " + ROUTE_ID_POST + "', null, null, 'Output Payload: ${body}')") // default logging
                .doFinally().process(exchange -> {
            ((Histogram.Timer) exchange.getProperty(TIMER_NAME_POST)).observeDuration(); // stop Prometheus Histogram metric
        }).end()
        ;

        from("direct:putTransfersByTransferId").routeId(ROUTE_ID_PUT)
                .process(exchange -> {
                    requestCounterPut.inc(1); // increment Prometheus Counter metric
                    exchange.setProperty(TIMER_NAME_PUT, requestLatencyPut.startTimer()); // initiate Prometheus Histogram metric
                })
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Request received, PUT /transfers/${header.transferId}', " +
                        "null, null, 'Input Payload: ${body}')")
           
                .marshal().json(JsonLibrary.Gson)
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Request received, PUT /transfers/${header.transferId}', " +
                        "null, null, 'Input Payload in JSON: ${body}')")
                .unmarshal().json(JsonLibrary.Gson)

                /*
                 * BEGIN processing
                 */
            
                .setProperty("origPayload", simple("${body}"))
                .removeHeaders("CamelHttp*")
            
                .choice()
                .when(simple("${body['currentState']} == 'COMPLETED'"))
//                .process(exchange -> System.out.println())
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Transfer current state COMPLETED, PUT /transfers/${header.transferId}', " +
                        "null, null, null)")
                .setHeader(Exchange.HTTP_METHOD, constant("PUT"))
                .setHeader("Content-Type", constant("application/json"))
                .marshal().json(JsonLibrary.Gson)
                .toD("{{backend.endpoint}}/transfers/${header.transferId}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .unmarshal().json(JsonLibrary.Gson)
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Response from backend API, putTransfers: ${body}', " +
                        "'Tracking the response', 'Verify the response', null)") 
                .endDoTry()
            
                .choice()
                .when(simple("${body['currentState']} == 'ABORTED'"))
//                .process(exchange -> System.out.println())
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Transfer current state ABORTED, PUT /transfers/${header.transferId}', " +
                        "null, null, null)")
                .setHeader(Exchange.HTTP_METHOD, constant("PUT"))
                .setHeader("Content-Type", constant("application/json"))
                .marshal().json(JsonLibrary.Gson)
                .toD("{{backend.endpoint}}/transfers/${header.transferId}?bridgeEndpoint=true&throwExceptionOnFailure=false")
                .unmarshal().json(JsonLibrary.Gson)
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Response from backend API, putTransfers: ${body}', " +
                        "'Tracking the response', 'Verify the response', null)")  
                .endDoTry()             

                /*
                 * END processing
                 */
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Final Response: ${body}', " +
                        "null, null, 'Response of PUT /transfers/${header.transferId} API')")
                .process(exchange -> {
                    ((Histogram.Timer) exchange.getProperty(TIMER_NAME_PUT)).observeDuration(); // stop Prometheus Histogram metric
                }).end()
        ;

    }
}
