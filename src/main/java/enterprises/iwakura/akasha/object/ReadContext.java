package enterprises.iwakura.akasha.object;

import java.io.InputStream;

import lombok.Data;

@Data
public class ReadContext {

    private final InputStream inputStream;
    private final String fileName;
    private final long fileSizeBytes;

}
