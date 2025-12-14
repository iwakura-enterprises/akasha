package enterprises.iwakura.akasha;

import java.io.InputStream;

import enterprises.iwakura.akasha.controller.DataSourceController;
import enterprises.iwakura.akasha.service.CacheService;
import enterprises.iwakura.akasha.service.DataSourceService;
import enterprises.iwakura.akasha.service.FileCacheService;
import enterprises.iwakura.ganyu.Ganyu;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import io.javalin.Javalin;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

@Bean
@Slf4j
@RequiredArgsConstructor
public class Akasha {

    private final AkashaConfiguration configuration;

    private final DataSourceController dataSourceController;
    private final FileCacheService fileCacheService;
    private final CacheService cacheService;
    private final Ganyu ganyu;

    public void start(String[] args) {
        log.info("Starting Akasha...");
        log.info("Made by Mayuna");

        log.info("Java Runtime Information:");
        log.info("Java Version: {}", System.getProperty("java.version"));
        log.info("Java Vendor: {}", System.getProperty("java.vendor"));
        log.info("Java VM Name: {}", System.getProperty("java.vm.name"));
        log.info("Java VM Version: {}", System.getProperty("java.vm.version"));
        log.info("Java VM Vendor: {}", System.getProperty("java.vm.vendor"));

        log.info("""
                \s
                \033[0;35M     ___    __              __
                \033[0;35M    /   |  / /______ ______/ /_  ____ _
                \033[0;35M   / /| | / //_/ __ `/ ___/ __ \\/ __ `/
                \033[0;35M  / ___ |/ ,< / /_/ (__  ) / / / /_/ /
                \033[0;35M /_/  |_/_/|_|\\__,_/____/_/ /_/\\__,_/
                \s
                \033[0;35M ================== Akasha ===================
                \033[0;35M ==     Created by: Iwakura Enterprises     ==
                \033[0;35M =============================================\033[0m
                """
        );

        log.info("Loading...");
        var startMillis = System.currentTimeMillis();

        ganyu.run();
        cacheService.init();
        fileCacheService.init();
        dataSourceController.registerRoutes();

        log.info("Successfully started Akasha in {} ms", System.currentTimeMillis() - startMillis);
    }
}
