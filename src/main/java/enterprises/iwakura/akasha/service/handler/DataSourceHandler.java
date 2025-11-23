package enterprises.iwakura.akasha.service.handler;

import java.io.IOException;
import java.io.InputStream;

import enterprises.iwakura.akasha.object.DataSource;
import enterprises.iwakura.akasha.object.DataSourceType;
import enterprises.iwakura.akasha.object.ReadContext;

public interface DataSourceHandler<T extends DataSource> {

    DataSourceType getType();

    ReadContext read(T dataSource, String path) throws IOException;

    long write(T dataSource, String path, InputStream data) throws IOException;

}
