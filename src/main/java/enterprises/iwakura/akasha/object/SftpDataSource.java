package enterprises.iwakura.akasha.object;

import java.util.List;
import java.util.UUID;

import enterprises.iwakura.akasha.object.Permission.Entry;
import lombok.Data;

@Data
public class SftpDataSource implements DataSource {

    private final DataSourceType type = DataSourceType.SFTP;
    private String name;
    private String hostname;
    private int port;
    private String username;
    private String password;
    private Permission permission = new Permission();

    /**
     * Creates an example SFTP data source.
     *
     * @return An example SftpDataSource instance.
     */
    public static SftpDataSource example() {
        SftpDataSource source = new SftpDataSource();
        source.setName("Example SFTP");
        source.setHostname("sftp.example.com");
        source.setPort(22);
        source.setUsername("exampleuser");
        source.setPassword("examplepassword");
        var entry = new Entry();
        entry.setPath("/data/files/");
        entry.setTokens(List.of(UUID.randomUUID().toString()));
        entry.setWrite(true);
        source.getPermission().getEntries().add(entry);
        return source;
    }
}
