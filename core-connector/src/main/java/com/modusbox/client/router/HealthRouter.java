package com.modusbox.client.router;

import com.modusbox.client.exception.RouteExceptionHandlingConfigurer;
import com.modusbox.client.processor.CorsFilter;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

public class HealthRouter extends RouteBuilder {

	private final RouteExceptionHandlingConfigurer exception = new RouteExceptionHandlingConfigurer();
	private final CorsFilter corsFilter = new CorsFilter();

	private static final String ROUTE_ID = "com.modusbox.getHealth";
	private static final String COUNTER_NAME = "counter_get_health_requests";
	private static final String TIMER_NAME = "histogram_get_health_timer";
	private static final String HISTOGRAM_NAME = "histogram_get_health_requests_latency";

	public static final Counter requestCounter = Counter.build()
		.name(COUNTER_NAME)
		.help("Total requests for GET /health.")
		.register();

	private static final Histogram requestLatency = Histogram.build()
		.name(HISTOGRAM_NAME)
		.help("Request latency in seconds for GET /health.")
		.register();

    public void configure() {

		// Add custom global exception handling strategy
		exception.configureExceptionHandling(this);

		from("direct:getHealth").routeId(ROUTE_ID).doTry()
			.process(exchange -> {
				requestCounter.inc(1); // increment Prometheus Counter metric
				exchange.setProperty(TIMER_NAME, requestLatency.startTimer()); // initiate Prometheus Histogram metric
			})
//			.to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
//					"'Request received, " + ROUTE_ID + "', null, null, null)") // default logging
			/*
			 * BEGIN processing
			 */
			// Add CORS headers
			.process(corsFilter)

			.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
			.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
			.setBody(constant("OK"))
			/*
			 * END processing
			 */
//			.to("bean:customJsonMessage?method=logJsonMessage('info', ${header.X-CorrelationId}, " +
//					"'Send response, " + ROUTE_ID + "', null, null, 'Output Payload: ${body}')") // default logging
			.doFinally().process(exchange -> {
				((Histogram.Timer) exchange.getProperty(TIMER_NAME)).observeDuration(); // stop Prometheus Histogram metric
			}).end()
		;
    }
}
