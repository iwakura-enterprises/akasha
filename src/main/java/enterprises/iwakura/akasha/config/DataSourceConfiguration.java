package enterprises.iwakura.akasha.config;

import java.util.ArrayList;
import java.util.List;

import enterprises.iwakura.akasha.object.DataSource;
import lombok.Data;

@Data
public class DataSourceConfiguration {

    private boolean validateWritePermissionEntriesHaveToken = true;
    private List<DataSource> sources = new ArrayList<>();

}
