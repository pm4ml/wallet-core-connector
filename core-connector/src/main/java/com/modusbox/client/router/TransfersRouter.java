package com.modusbox.client.router;

import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

public class TransfersRouter extends RouteBuilder {

    private final RouteExceptionHandlingConfigurer exception = new RouteExceptionHandlingConfigurer();

    private static final String ROUTE_ID = "com.modusbox.postTransfers";
    private static final String COUNTER_NAME = "counter_post_transfers_requests";
    private static final String TIMER_NAME = "histogram_post_transfers_timer";
    private static final String HISTOGRAM_NAME = "histogram_post_transfers_requests_latency";

    public static final Counter requestCounter = Counter.build()
            .name(COUNTER_NAME)
            .help("Total requests for POST /transfers.")
            .register();

    private static final Histogram requestLatency = Histogram.build()
            .name(HISTOGRAM_NAME)
            .help("Request latency in seconds for POST /transfers.")
            .register();

    public void configure() {

        // Add custom global exception handling strategy
        exception.configureExceptionHandling(this);

        from("direct:postTransfers").routeId(ROUTE_ID).doTry()
                .process(exchange -> {
                    requestCounter.inc(1); // increment Prometheus Counter metric
                    exchange.setProperty(TIMER_NAME, requestLatency.startTimer()); // initiate Prometheus Histogram metric
                })
                .to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
                        "'Request received, " + ROUTE_ID + "', null, null, 'Input Payload: ${body}')") // default logging
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
                        "'Send response, " + ROUTE_ID + "', null, null, 'Output Payload: ${body}')") // default logging
                .doFinally().process(exchange -> {
            ((Histogram.Timer) exchange.getProperty(TIMER_NAME)).observeDuration(); // stop Prometheus Histogram metric
        }).end()
        ;

    }
}
