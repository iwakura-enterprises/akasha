package enterprises.iwakura.akasha.object;

import java.io.InputStream;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ReadContext {

    private InputStream inputStream;
    private final String fileName;
    private final long fileSizeBytes;

}
