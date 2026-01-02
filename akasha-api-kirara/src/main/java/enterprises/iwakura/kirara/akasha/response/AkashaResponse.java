package enterprises.iwakura.kirara.akasha.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * AkashaResponse represents a response from the Akasha service.
 */
@Data
@Builder
@AllArgsConstructor
public class AkashaResponse {

    private int status;
    private String message;
    private String contentType;
    private byte[] content;

    public AkashaResponse(int status, String message) {
        this.status = status;
        this.message = message;
    }
}
