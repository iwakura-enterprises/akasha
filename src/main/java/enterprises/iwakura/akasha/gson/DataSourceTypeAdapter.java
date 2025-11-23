package enterprises.iwakura.akasha.gson;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import enterprises.iwakura.akasha.object.DataSource;
import enterprises.iwakura.akasha.object.SftpDataSource;
import enterprises.iwakura.akasha.object.DataSourceType;

public class DataSourceTypeAdapter implements JsonDeserializer<DataSource> {

    @Override
    public DataSource deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (!json.isJsonObject()) {
            return null;
        }

        var type = DataSourceType.valueOf(json.getAsJsonObject().get("type").getAsString());
        switch (type) {
            case SFTP -> {
                return context.deserialize(json, SftpDataSource.class);
            }
            default -> throw new JsonParseException("Unsupported DataSource type: " + type);
        }
    }
}
