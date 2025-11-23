package enterprises.iwakura.akasha.controller;

import java.io.InputStream;

import enterprises.iwakura.akasha.service.DataSourceService;
import enterprises.iwakura.akasha.util.PathUtils;
import enterprises.iwakura.sigewine.core.annotations.Bean;
import io.javalin.Javalin;
import io.javalin.http.UploadedFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Bean
@Slf4j
@RequiredArgsConstructor
public class DataSourceController {

    private final Javalin javalin;
    private final DataSourceService dataSourceService;

    public void registerRoutes() {
        log.info("Registering data source routes...");

        javalin.get("/data-source/{dataSourceName}/<filepath>", ctx -> {
            String dataSourceName = ctx.pathParam("dataSourceName");
            String filePath = ctx.pathParam("filepath");
            dataSourceService.read(dataSourceName, PathUtils.normalizePath(filePath), ctx);
        });

        javalin.put("/data-source/{dataSourceName}/<filepath>", ctx -> {
            String dataSourceName = ctx.pathParam("dataSourceName");
            String filePath = ctx.pathParam("filepath");
            UploadedFile file = ctx.uploadedFile("file");

            if (file == null) {
                ctx.status(400).result("No file uploaded");
                return;
            }

            try (InputStream inputStream = file.content()) {
                dataSourceService.write(dataSourceName, PathUtils.normalizePath(filePath), inputStream, ctx);
            }
        });
    }
}
