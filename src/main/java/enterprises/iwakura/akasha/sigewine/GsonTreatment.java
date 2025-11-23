package enterprises.iwakura.akasha.sigewine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import enterprises.iwakura.akasha.gson.DataSourceTypeAdapter;
import enterprises.iwakura.akasha.object.DataSource;
import enterprises.iwakura.sigewine.core.annotations.Bean;

@Bean
public class GsonTreatment {

    @Bean
    public Gson gson() {
        return new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(DataSource.class, new DataSourceTypeAdapter())
            .create();
    }
}
