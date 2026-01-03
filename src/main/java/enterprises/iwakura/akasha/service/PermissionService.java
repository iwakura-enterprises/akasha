package enterprises.iwakura.akasha.service;

import java.util.List;

import enterprises.iwakura.akasha.object.DataSource;
import enterprises.iwakura.akasha.object.Permission.Entry;
import enterprises.iwakura.akasha.util.PathUtils;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import io.javalin.http.Context;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
@RequiredArgsConstructor
public class PermissionService {

    /**
     * Checks if the context has permission to access the file path in the data source.
     *
     * @param dataSource Data source
     * @param filePath   File path
     * @param write      Whether write access is required
     * @param ctx        Javalin context
     *
     * @return true if the context has permission, false otherwise
     */
    public boolean hasPermission(DataSource dataSource, String filePath, boolean write, Context ctx) {
        var permission = dataSource.getPermission();
        var entries = permission.getEntries() == null ? List.<Entry>of() : permission.getEntries();

        if (!entries.isEmpty()) {
            for (Entry entry : entries) {
                var entryPath = PathUtils.normalizePath(entry.getPath());
                if (filePath.startsWith(entryPath)) {
                    var tokens = entry.getTokens() == null ? List.<String>of() : entry.getTokens();
                    if (hasValidToken(tokens, ctx)) {
                        if (write) {
                            if (entry.isWrite()) {
                                return true;
                            }
                        } else {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean hasValidToken(List<String> tokens, Context ctx) {
        if (!tokens.isEmpty()) {
            var queryToken = ctx.queryParam("token");
            return tokens.contains(queryToken);
        } else {
            return true;
        }
    }
}
