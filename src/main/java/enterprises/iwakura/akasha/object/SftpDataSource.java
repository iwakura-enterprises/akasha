package enterprises.iwakura.akasha.object;

import lombok.Data;

@Data
public class SftpDataSource implements DataSource {

    private String name;
    private String hostname;
    private int port;
    private String username;
    private String password;
    private Permission permission = new Permission();

    @Override
    public DataSourceType getType() {
        return DataSourceType.SFTP;
    }
}
