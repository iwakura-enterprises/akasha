package enterprises.iwakura.akasha.object;

import lombok.Data;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;

@Data
public class SftpContext {

    private final SSHClient sshClient;
    private final SFTPClient sftpClient;
}
