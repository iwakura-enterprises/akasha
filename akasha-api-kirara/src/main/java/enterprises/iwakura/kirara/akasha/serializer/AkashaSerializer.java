package enterprises.iwakura.kirara.akasha.serializer;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import enterprises.iwakura.kirara.akasha.response.AkashaResponse;
import enterprises.iwakura.kirara.core.Serializer;

/**
 * Implementation of {@link Serializer} to handle Akasha-specific serialization and deserialization logic.
 */
public class AkashaSerializer implements Serializer {

    /**
     * Json serializer used for non-byte[] types.
     */
    protected final Serializer jsonSerializer;

    /**
     * Constructor for AkashaSerializer.
     *
     * @param jsonSerializer JSON serializer to use for non-byte[] types.
     */
    public AkashaSerializer(Serializer jsonSerializer) {
        this.jsonSerializer = jsonSerializer;
    }

    @Override
    public byte[] serialize(Object object) {
        if (object.getClass() == byte[].class) {
            // Pass byte[] directly
            return (byte[]) object;
        }

        // Fallback to JSON serialization for other types
        return jsonSerializer.serialize(object);
    }

    @Override
    public <T> T deserialize(
        Class<T> specifiedResponseClass,
        int statusCode,
        Map<String, List<String>> responseHeaders,
        byte[] response
    ) {
        String contentType = Optional.ofNullable(responseHeaders.get("Content-Type"))
            .map(list -> list.isEmpty() ? "" : list.get(0))
            .orElse("");

        if (contentType.startsWith("application/json")) {
            return jsonSerializer.deserialize(specifiedResponseClass, statusCode, responseHeaders, response);
        } else {
            if (specifiedResponseClass == AkashaResponse.class) {
                //noinspection unchecked
                return (T) AkashaResponse.builder()
                    .status(statusCode) // Usually 200
                    .contentType(contentType)
                    .content(response)
                    .build();
            } else {
                throw new UnsupportedOperationException(
                    "Cannot deserialize non-JSON response to type: " + specifiedResponseClass.getName());
            }
        }
    }
}
