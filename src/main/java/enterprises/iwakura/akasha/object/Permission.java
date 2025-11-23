package enterprises.iwakura.akasha.object;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class Permission {

    private List<Entry> entries = new ArrayList<>();

    @Data
    public static class Entry {

        private String path;
        private boolean write;
        private List<String> tokens = new ArrayList<>();
    }
}
