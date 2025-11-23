package enterprises.iwakura.akasha.command;

import enterprises.iwakura.ganyu.GanyuCommand;
import enterprises.iwakura.ganyu.annotation.Command;
import enterprises.iwakura.ganyu.annotation.DefaultCommand;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
@Command("stop")
public class StopCommand implements GanyuCommand {

    @DefaultCommand
    public void stop() {
        log.info("Stopping Akasha...");
        System.exit(0);
    }
}
