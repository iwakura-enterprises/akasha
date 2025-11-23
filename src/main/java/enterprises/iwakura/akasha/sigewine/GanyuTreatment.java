package enterprises.iwakura.akasha.sigewine;

import java.util.List;

import enterprises.iwakura.akasha.command.StopCommand;
import enterprises.iwakura.ganyu.Ganyu;
import enterprises.iwakura.ganyu.GanyuCommand;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Bean
public class GanyuTreatment {

    private final List<GanyuCommand> commands = List.of(new StopCommand());

    @Bean
    public Ganyu ganyu() {
        var ganyu = Ganyu.console();
        commands.forEach(command -> {
            log.info("Registering Ganyu command: {}", command.getClass().getSimpleName());
            ganyu.registerCommands(command);
        });
        return ganyu;
    }
}
