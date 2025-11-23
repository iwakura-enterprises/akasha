package enterprises.iwakura.akasha;

import enterprises.iwakura.sigewine.core.Sigewine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AkashaMain {

    public static final Sigewine sigewine = new Sigewine();

    public static void main(String[] args) {
        log.info("Preparing Akasha...");

        log.info("Initializing Sigewine...");
        sigewine.scan(AkashaMain.class);
        log.info("Sigewine initialized with {} beans", sigewine.getSingletonBeans().size());

        log.info("Getting Akasha bean...");
        var akasha = sigewine.inject(Akasha.class);

        log.info("Starting Akasha...");
        akasha.start(args);
    }
}
