package enterprises.iwakura.kirara.akasha;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import enterprises.iwakura.kirara.akasha.Version;
import enterprises.iwakura.kirara.akasha.response.AkashaResponse;
import enterprises.iwakura.kirara.akasha.serializer.AkashaSerializer;
import enterprises.iwakura.kirara.core.ApiRequest;
import enterprises.iwakura.kirara.core.HttpCore;
import enterprises.iwakura.kirara.core.Kirara;
import enterprises.iwakura.kirara.core.PathParameter;
import enterprises.iwakura.kirara.core.RequestHeader;
import enterprises.iwakura.kirara.core.RequestQuery;
import enterprises.iwakura.kirara.core.Serializer;
import enterprises.iwakura.kirara.core.impl.HttpUrlConnectionHttpCore;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

/**
 * Akasha API client extending Kirara framework for making requests to Akasha service endpoints.
 */
@Getter
@Setter
public class AkashaApi extends Kirara {

    /**
     * Default request headers for Akasha API
     */
    public static final List<RequestHeader> DEFAULT_HEADERS = new ArrayList<>(Arrays.asList(
        RequestHeader.of("User-Agent", String.format("Akasha-Kirara-Client/%s (Java) Kirara/%s", Version.VERSION, Version.KIRARA_VERSION)),
        RequestHeader.of("Accept", "application/json")
    ));

    /**
     * Default access token for API requests
     */
    protected String defaultToken;

    /**
     * Constructor for {@link AkashaApi}. Sets up the API with default headers and an optional default token.
     *
     * @param httpCore       {@link HttpCore} instance for making HTTP requests (e.g.,
     *                       {@link HttpUrlConnectionHttpCore} or see <a href="https://mvnrepository.com/artifact/enterprises.iwakura/kirara-httpclient">kirara-httpclient</a>)
     * @param jsonSerializer A {@link Serializer} instance for JSON deserialization (see <a href="https://mvnrepository.com/artifact/enterprises.iwakura/kirara-gson">kirara-gson</a>)
     * @param apiUrl         Base URL of the Akasha API (e.g., <pre>https://akasha.example.com</pre>)
     */
    public AkashaApi(
        HttpCore httpCore,
        Serializer jsonSerializer,
        String apiUrl
    ) {
        this(httpCore, jsonSerializer, apiUrl, null);
    }

    /**
     * Constructor for {@link AkashaApi}. Sets up the API with default headers and an optional default token.
     *
     * @param httpCore       {@link HttpCore} instance for making HTTP requests (e.g.,
     *                       {@link HttpUrlConnectionHttpCore} or see <a href="https://mvnrepository.com/artifact/enterprises.iwakura/kirara-httpclient">kirara-httpclient</a>)
     * @param jsonSerializer A {@link Serializer} instance for JSON deserialization (see <a href="https://mvnrepository.com/artifact/enterprises.iwakura/kirara-gson">kirara-gson</a>)
     * @param apiUrl         Base URL of the Akasha API (e.g., <pre>https://akasha.example.com</pre>)
     * @param defaultToken   (Optional) Default access token for API requests
     */
    public AkashaApi(
        HttpCore httpCore,
        Serializer jsonSerializer,
        String apiUrl,
        String defaultToken
    ) {
        super(httpCore, new AkashaSerializer(jsonSerializer), apiUrl);
        setDefaultRequestHeaders(DEFAULT_HEADERS);
        this.defaultToken = defaultToken;
    }

    /**
     * Reads a file from the data source. Uses {@link #defaultToken} if available.
     *
     * @param dataSource Data source name
     * @param filePath   File path
     *
     * @return ApiRequest for AkashaResponse, invoke {@link ApiRequest#send()} to execute
     */
    public ApiRequest<AkashaResponse> read(String dataSource, String filePath) {
        return read(dataSource, filePath, defaultToken);
    }

    /**
     * Reads a file from the data source.
     *
     * @param dataSource Data source name
     * @param filePath   File path
     * @param token      Access token
     *
     * @return ApiRequest for AkashaResponse, invoke {@link ApiRequest#send()} to execute
     */
    public ApiRequest<AkashaResponse> read(String dataSource, String filePath, String token) {
        if (filePath.startsWith("/")) {
            filePath = filePath.substring(1);
        }

        ApiRequest<AkashaResponse> request = this.createRequest("GET", "/data-source/{dataSource}/{filePath}", AkashaResponse.class)
            .withPathParameter(PathParameter.of("dataSource", dataSource))
            .withPathParameter(PathParameter.of("filePath", filePath));

        if (token != null) {
            request.withRequestQuery(RequestQuery.of("token", token));
        }

        return request;
    }

    /**
     * Writes a file to the data source. Uses {@link #defaultToken} if available.
     *
     * @param dataSource Data source name
     * @param filePath   File path
     * @param content    File content as byte array
     *
     * @return ApiRequest for AkashaResponse, invoke {@link ApiRequest#send()} to execute
     */
    public ApiRequest<AkashaResponse> write(String dataSource, String filePath, byte[] content) {
        return write(dataSource, filePath, defaultToken, content);
    }

    /**
     * Writes a file to the data source.
     *
     * @param dataSource Data source name
     * @param filePath   File path
     * @param token      Access token
     * @param content    File content as byte array
     *
     * @return ApiRequest for AkashaResponse, invoke {@link ApiRequest#send()} to execute
     */
    @SneakyThrows
    public ApiRequest<AkashaResponse> write(String dataSource, String filePath, String token, byte[] content) {
        if (filePath.startsWith("/")) {
            filePath = filePath.substring(1);
        }

        String boundary = "------------------------" + System.currentTimeMillis();
        String crlf = "\r\n";
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(("--" + boundary + crlf).getBytes());
        baos.write(("Content-Disposition: form-data; name=\"file\"; filename=\"file\"" + crlf).getBytes());
        baos.write(("Content-Type: application/octet-stream" + crlf).getBytes());
        baos.write(crlf.getBytes());
        baos.write(content);
        baos.write(crlf.getBytes());
        baos.write(("--" + boundary + "--" + crlf).getBytes());

        ApiRequest<AkashaResponse> request = this.createRequest("PUT", "/data-source/{dataSource}/{filePath}", AkashaResponse.class)
            .withPathParameter(PathParameter.of("dataSource", dataSource))
            .withPathParameter(PathParameter.of("filePath", filePath))
            .withHeader(RequestHeader.of("Content-Type", "multipart/form-data; boundary=" + boundary))
            .withBody(baos.toByteArray());

        if (token != null) {
            request.withRequestQuery(RequestQuery.of("token", token));
        }

        return request;
    }
}
