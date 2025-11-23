package enterprises.iwakura.akasha.service.handler;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Set;

import com.google.common.cache.Cache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

import enterprises.iwakura.akasha.exception.HandledException;
import enterprises.iwakura.akasha.object.DataSourceType;
import enterprises.iwakura.akasha.object.ReadContext;
import enterprises.iwakura.akasha.object.SftpContext;
import enterprises.iwakura.akasha.object.SftpDataSource;
import enterprises.iwakura.akasha.service.CacheService;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.OpenMode;
import net.schmizz.sshj.sftp.SFTPException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

@Bean
@Slf4j
public class SftpDataSourceHandler implements DataSourceHandler<SftpDataSource>, RemovalListener<SftpDataSource, SftpContext> {

    public static final int MAX_UNCONFIRMED_READS = 16;

    private final Cache<SftpDataSource, SftpContext> clientMap = CacheService.createCache(cacheBuilder -> cacheBuilder
        .expireAfterAccess(Duration.ofHours(1))
        .removalListener(this));

    @Override
    public DataSourceType getType() {
        return DataSourceType.SFTP;
    }

    @Override
    public ReadContext read(SftpDataSource dataSource, String path) throws IOException {
        var sftpClient = getOrConnect(dataSource).getSftpClient();
        try {
            var remoteFile = sftpClient.open(path);
            var inputStream = remoteFile.new ReadAheadRemoteFileInputStream(MAX_UNCONFIRMED_READS);
            var fileName = path.substring(path.lastIndexOf('/') + 1);
            return new ReadContext(inputStream, fileName, remoteFile.length());
        } catch (SFTPException sftpException) {
            switch (sftpException.getStatusCode()) {
                case NO_SUCH_FILE, NO_SUCH_PATH -> throw new HandledException("No such file", sftpException);
                case PERMISSION_DENIED -> throw new HandledException("Permission denied", sftpException);
            }
            throw sftpException;
        }
    }

    @Override
    public long write(SftpDataSource dataSource, String path, InputStream data) throws IOException {
        var sftpClient = getOrConnect(dataSource).getSftpClient();
        long totalWritten = 0;
        try (var remoteFile = sftpClient.open(path, Set.of(OpenMode.CREAT, OpenMode.WRITE, OpenMode.TRUNC))) {
            byte[] buffer = new byte[8192];
            int length;
            // FIXME: Very slow! Make asynchronous.
            while ((length = data.read(buffer)) > 0) {
                remoteFile.write(totalWritten, buffer, 0, length);
            }
        } catch (SFTPException sftpException) {
            switch (sftpException.getStatusCode()) {
                case NO_SUCH_FILE, NO_SUCH_PATH -> throw new HandledException("No such path", sftpException);
                case PERMISSION_DENIED -> throw new HandledException("Permission denied", sftpException);
            }
            throw sftpException;
        }
        return totalWritten;
    }

    @SneakyThrows
    private SftpContext getOrConnect(SftpDataSource dataSource) {
        return clientMap.get(dataSource, () -> {
            try {
                log.info("Connecting to SFTP server {}:{} for data source {}", dataSource.getHostname(), dataSource.getPort(), dataSource.getName());
                var sshClient = new SSHClient();
                sshClient.addHostKeyVerifier(new PromiscuousVerifier());
                sshClient.connect(dataSource.getHostname(), dataSource.getPort());
                sshClient.useCompression();
                sshClient.authPassword(dataSource.getUsername(), dataSource.getPassword());
                return new SftpContext(sshClient, sshClient.newSFTPClient());
            } catch (Exception e) {
                throw new RuntimeException("Failed to connect to SFTP server", e);
            }
        });
    }

    @Override
    public void onRemoval(RemovalNotification<SftpDataSource, SftpContext> notification) {
        var dataSource = notification.getKey();
        var ctx = notification.getValue();

        if (dataSource != null && ctx != null) {
            try {
                log.info("Closing SFTP connection for data source {}", dataSource.getName());
                ctx.getSshClient().disconnect();
            } catch (Exception e) {
                log.error("Failed to close SFTP connection for data source {}", dataSource.getName(), e);
            }
        }
    }
}
