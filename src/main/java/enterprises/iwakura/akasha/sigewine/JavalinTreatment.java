package enterprises.iwakura.akasha.sigewine;

import org.eclipse.jetty.util.thread.ThreadPool;

import enterprises.iwakura.akasha.AkashaConfiguration;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import io.javalin.Javalin;
import lombok.RequiredArgsConstructor;

@Bean
@RequiredArgsConstructor
public class JavalinTreatment {

    private final AkashaConfiguration configuration;

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
        });
        javalin.start(configuration.get().getJavalin().getPort());
        return javalin;
    }
}
