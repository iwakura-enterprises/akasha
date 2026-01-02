package enterprises.iwakura.kirara.akasha;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import enterprises.iwakura.kirara.akasha.response.AkashaResponse;
import enterprises.iwakura.kirara.core.impl.HttpUrlConnectionHttpCore;
import enterprises.iwakura.kirara.gson.GsonSerializer;

public class TestAkashaApi {

    @Test
    @Disabled
    public void testAkashaApi() {
        AkashaApi api = new AkashaApi(new HttpUrlConnectionHttpCore(), new GsonSerializer(), "http://localhost:7000");
        api.setDefaultToken("write-access-token");

        String text = "Hello, Akasha!";

        AkashaResponse writeResult = api.write("hetzner", "/public/writable/test.txt", text.getBytes(StandardCharsets.UTF_8)).send().join();
        assertNotNull(writeResult);
        assertEquals(200, writeResult.getStatus(), "Write operation failed: " + writeResult.getMessage());

        AkashaResponse readResult = api.read("hetzner", "/public/writable/test.txt").send().join();
        assertNotNull(readResult);
        assertEquals(200, readResult.getStatus(), "Read operation failed: " + readResult.getMessage());
        assertEquals(text, new String(readResult.getContent(), StandardCharsets.UTF_8), "Read data does not match written data");
    }
}
