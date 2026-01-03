package enterprises.iwakura.akasha.service;

import java.util.Optional;

import com.google.gson.Gson;

import enterprises.iwakura.akasha.AkashaConfiguration;
import enterprises.iwakura.akasha.AkashaVersion;
import enterprises.iwakura.akasha.object.ReadContext;
import enterprises.iwakura.akasha.util.ContentTypeResolver;
import enterprises.iwakura.amber.Version;
import enterprises.iwakura.kirara.akasha.response.AkashaResponse;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import io.javalin.http.Context;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
@RequiredArgsConstructor
public class ResponseHandlerService {

    public static final String HTML_RESPONSE_TEMPLATE = "<html><body><h1>HTTP Status %d</h1><pre>%s</pre></body><footer>Akasha %s</footer></html>";
    public static final String PLAIN_TEXT_RESPONSE_TEMPLATE = "HTTP Status %d: %s";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_HTML = "text/html";
    public static final String CONTENT_TYPE_PLAIN = "text/plain";

    private final AkashaConfiguration configuration;
    private final Gson gson;

    /**
     * Responds javalin's context with read context.
     *
     * @param readContext ReadContext
     * @param ctx         Context
     */
    public void respondWithReadContext(ReadContext readContext, Context ctx) {
        // Resolves content type based on the file extension
        ctx.contentType(ContentTypeResolver.getContentType(readContext.getFileName()));
        ctx.header("Content-Disposition", "inline; filename=\"%s\"".formatted(readContext.getFileName()));
        ctx.header("Content-Length", String.valueOf(readContext.getFileSizeBytes()));
        // Makes sure the content-encoding matches the actual encoding if any proxy is used
        ctx.header("Content-Encoding", "identity");
        ctx.header("Cache-Control",
            "public, max-age=%d".formatted(configuration.getFileCache().getHttpCacheMaxAgeSeconds()));
        ctx.result(readContext.getInputStream());
        ctx.status(200);
    }

    /**
     * Responds javalin's context with message based on the preferred content type.
     *
     * @param ctx       Javalin context
     * @param statusCode HTTP status code
     * @param message   Message
     */
    public void respondWithMessage(Context ctx, int statusCode, String message) {
        String preferredContentType = Optional.ofNullable(ctx.header("Accept")).orElse(CONTENT_TYPE_PLAIN);

        ctx.status(statusCode);

        if (preferredContentType.contains(CONTENT_TYPE_JSON)) {
            ctx.contentType(CONTENT_TYPE_JSON);
            ctx.result(gson.toJsonTree(new AkashaResponse(statusCode, message)).toString());
        } else if (preferredContentType.contains(CONTENT_TYPE_HTML)) {
            ctx.contentType(CONTENT_TYPE_HTML);
            ctx.result(HTML_RESPONSE_TEMPLATE.formatted(statusCode, message, AkashaVersion.VERSION));
        } else {
            ctx.contentType(CONTENT_TYPE_PLAIN);
            ctx.result(PLAIN_TEXT_RESPONSE_TEMPLATE.formatted(statusCode, message));
        }
    }
}
