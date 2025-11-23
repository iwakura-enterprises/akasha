package enterprises.iwakura.akasha.util;

import java.nio.file.Paths;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PathUtils {

    /**
     * Normalize a file path to use forward slashes and remove redundant elements.
     *
     * @param path The file path to normalize
     *
     * @return The normalized file path
     */
    public static String normalizePath(String path) {
        return Paths.get(path).normalize().toString().replace("\\", "/");
    }
}
