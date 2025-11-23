package enterprises.iwakura.akasha.object;

public interface DataSource {

    String getName();

    DataSourceType getType();

    Permission getPermission();

}
