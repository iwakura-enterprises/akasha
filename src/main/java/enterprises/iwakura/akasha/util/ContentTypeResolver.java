package enterprises.iwakura.akasha.util;

import java.util.Map;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ContentTypeResolver {

    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    public static final Map<String, String> EXTENSION_TO_CONTENT_TYPE = Map.ofEntries(
        Map.entry("html", "text/html"),
        Map.entry("htm", "text/html"),
        Map.entry("css", "text/css"),
        Map.entry("js", "application/javascript"),
        Map.entry("json", "application/json"),
        Map.entry("xml", "application/xml"),
        Map.entry("png", "image/png"),
        Map.entry("jpg", "image/jpeg"),
        Map.entry("jpeg", "image/jpeg"),
        Map.entry("gif", "image/gif"),
        Map.entry("svg", "image/svg+xml"),
        Map.entry("pdf", "application/pdf"),
        Map.entry("txt", "text/plain"),
        Map.entry("zip", "application/zip")
    );

    /**
     * Resolves the content type based on the file name's extension.
     *
     * @param fileName The name of the file whose content type is to be determined.
     *
     * @return The resolved content type as a string.
     */
    public static String getContentType(String fileName) {
        var indexOfDot = fileName != null ? fileName.lastIndexOf('.') : -1;
        if (fileName == null || indexOfDot == -1) {
            return DEFAULT_CONTENT_TYPE;
        }

        var extension = fileName.substring(indexOfDot + 1).toLowerCase();
        return EXTENSION_TO_CONTENT_TYPE.getOrDefault(extension, DEFAULT_CONTENT_TYPE);
    }
}
