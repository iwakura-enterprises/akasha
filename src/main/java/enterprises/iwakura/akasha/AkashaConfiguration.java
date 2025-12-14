package enterprises.iwakura.akasha;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import enterprises.iwakura.akasha.object.DataSource;
import enterprises.iwakura.keqing.Keqing;
import enterprises.iwakura.keqing.impl.GsonSerializer;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@RequiredArgsConstructor
public class AkashaConfiguration {

    private final static String CONFIG_DIRECTORY_PATH = "./";

    private final Gson gson;
    private Config config = new Config();

    public AkashaConfiguration reload() {
        try {
            Files.createDirectories(Path.of(CONFIG_DIRECTORY_PATH));

            // Create default config if not exists
            var defaultConfigPath = Path.of(CONFIG_DIRECTORY_PATH + "config.json");
            if (Files.notExists(defaultConfigPath)) {
                var defaultConfigResource = AkashaConfiguration.class.getResourceAsStream("/default_config.json");
                if (defaultConfigResource != null) {
                    Files.copy(defaultConfigResource, defaultConfigPath);
                }
            }

            var keqing = Keqing.loadFromFileSystem(CONFIG_DIRECTORY_PATH + "config", '-', new GsonSerializer(gson));
            keqing.setPostfixPriorities(List.of("override", "production", "development"));
            this.config = keqing.readProperty("", Config.class);
            return this;
        } catch (Exception exception) {
            log.error("Failed to load UncLibConfig config", exception);
            throw new RuntimeException("Failed to load UncLibConfig config", exception);
        }
    }

    public Config get() {
        return this.config;
    }

    @Data
    public static class Config {

        private Javalin javalin = new Javalin();
        private FileCache fileCache = new FileCache();
        private List<DataSource> sources = new ArrayList<>();

        @Data
        public static class Javalin {

            private int port = 7000;
        }

        @Data
        public static class FileCache {

            private boolean enabled = true;
            private boolean resetTtlOnAccess = true;
            private String directory = "./file_cache/";
            private long ttlSeconds = 3600; // 1 hour
            private long maxSizePerFileBytes = 10 * 1024 * 1024; // 10 MB
            private long maxTotalSizeBytes = 1000 * 1024 * 1024; // 1000 MB
        }
    }
}
