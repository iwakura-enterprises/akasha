package enterprises.iwakura.akasha;

import enterprises.iwakura.sigewine.core.Sigewine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AkashaMain {

    public static final Sigewine sigewine = new Sigewine();

    public static void main(String[] args) {
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

        log.info("Initializing Sigewine...");
        sigewine.scan(AkashaMain.class);
        log.info("Sigewine initialized with {} beans", sigewine.getSingletonBeans().size());

        log.info("Getting Akasha bean...");
        var akasha = sigewine.inject(Akasha.class);

        log.info("Starting Akasha...");
        try {
            akasha.start(args);
        } catch (Exception exception) {
            log.error("Failed to start Akasha!", exception);
            System.exit(1);
        }

        log.info("Successfully started Akasha in {} ms", System.currentTimeMillis() - AmberBootstrap.STARTUP_TIME);
    }
}
