package org.qubership.atp.itf.lite.backend.schedulers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = CachedFilesCleaner.class,
        properties = {"atp.itf.lite.clean.file.cache.time-sec=1",
                "atp.itf.lite.clean.file.cache.cron.expression=0 0 * * * *"})
@Isolated
public class CachedFilesCleanerTest {

    private static final Path directory = Paths.get("src/test/resources/tests/binary");
    private static final Path pathToFile = Paths.get(directory.toString(), "file.txt");
    @Autowired
    CachedFilesCleaner cachedFilesCleaner;

    @BeforeEach
    public void init() throws Exception {
        Files.createDirectories(directory);
        Files.write(pathToFile, "string".getBytes());
    }

    @Test
    public void testDeleteOldFiles_OneOldFile_OldFileWasDeleted() throws Exception {
        Thread.sleep(2000);

        cachedFilesCleaner.deleteOldFiles(Collections.singletonList(directory));

        Assertions.assertFalse(pathToFile.toFile().exists());
    }

    @AfterEach
    public void end() throws Exception {
        Files.delete(directory);
    }
}
