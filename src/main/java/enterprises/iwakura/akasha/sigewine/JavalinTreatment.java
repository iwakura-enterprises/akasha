package enterprises.iwakura.akasha.sigewine;

import java.io.IOException;
import java.util.EnumSet;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import enterprises.iwakura.akasha.AkashaConfiguration;
import enterprises.iwakura.akasha.service.PrometheusService;
import enterprises.iwakura.akasha.service.ResponseHandlerService;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import io.javalin.Javalin;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JavalinTreatment {

    private final ResponseHandlerService responseHandlerService;
    private final AkashaConfiguration configuration;
    private final PrometheusService prometheusService;

    @Bean
    public Javalin javalin() {
        var javalin = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.useVirtualThreads = true;
            config.contextResolver.ip = ctx -> {
                String forwardedFor = ctx.header("X-Forwarded-For");
                if (forwardedFor != null && !forwardedFor.isEmpty()) {
                    return forwardedFor.split(",")[0].trim();
                }
                // Fallback to the remote address if no forwarded header is present
                return ctx.req().getRemoteAddr();
            };

            // Adds Prometheus metrics endpoint if enabled
            var prometheusConfiguration = configuration.getPrometheus();
            if (prometheusConfiguration.isEnabled()) {
                log.info("Javalin will expose Prometheus metrics at /metrics");
                config.jetty.modifyServletContextHandler(handler -> {
                    handler.addServlet(new ServletHolder(prometheusService.getPrometheusMetricsServlet()), "/metrics");

                    if (prometheusConfiguration.isBearerTokenSet()) {
                        handler.addFilter(new FilterHolder((request, response, chain) -> {
                            HttpServletRequest req = (HttpServletRequest) request;
                            HttpServletResponse res = (HttpServletResponse) response;

                            String authHeader = req.getHeader("Authorization");
                            String expectedToken = configuration.getPrometheus().getBearerToken();

                            if (authHeader == null || !authHeader.startsWith("Bearer ") || !authHeader.substring(7).equals(expectedToken)) {
                                res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or missing Authorization");
                                return;
                            }
                            chain.doFilter(request, response);
                        }), "/metrics", EnumSet.of(DispatcherType.REQUEST));
                    } else {
                        log.warn("Prometheus metrics endpoint is enabled but no bearer token is set; the endpoint will be publicly accessible! This could be a security risk.");
                    }
                });
            } else {
                log.warn("Prometheus metrics endpoint is disabled in configuration");
            }
        }).exception(Exception.class, (e, ctx) -> {
            log.error("Unhandled exception occurred", e);
            responseHandlerService.respondWithMessage(ctx, 500, "Internal Server Error: " + e.getMessage());
        }).error(404, ctx -> {
            String resultString = ctx.result();
            if (resultString == null || resultString.startsWith("Endpoint") && resultString.endsWith("not found")) {
                responseHandlerService.respondWithMessage(ctx, 404, "The requested resource was not found.");
            }
        });
        javalin.start(configuration.getJavalin().getPort());
        return javalin;
    }
}
