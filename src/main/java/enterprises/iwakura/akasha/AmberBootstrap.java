package enterprises.iwakura.akasha;

import java.io.IOException;

import enterprises.iwakura.amber.Amber;
import enterprises.iwakura.amber.BootstrapOptions;

public class AmberBootstrap {

    public static final long STARTUP_TIME = System.currentTimeMillis();

    public static void main(String[] args) throws IOException {
        System.out.println("Bootstrapping Akasha...");
        Amber amber = Amber.classLoader();
        amber.bootstrap(BootstrapOptions.builder()
            .exitCodeAfterDownload(-1225)
            .exitMessageAfterDownload("Please, restart the application.")
            .downloaderThreadCount(64)
            .build()
        );

        AkashaMain.main(args);
    }
}
