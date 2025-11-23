package enterprises.iwakura.akasha.sigewine;

import com.google.gson.Gson;

import enterprises.iwakura.akasha.AkashaConfiguration;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.RequiredArgsConstructor;

@Bean
@RequiredArgsConstructor
public class AkashaConfigTreatment {

    private final Gson gson;

    @Bean
    public AkashaConfiguration akashaConfig() {
        return new AkashaConfiguration(gson).reload();
    }
}
