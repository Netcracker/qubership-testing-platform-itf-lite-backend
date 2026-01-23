package org.qubership.atp.itf.lite.backend.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileData;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@Isolated
public class FileUtilsTest {

    @Test
    public void deleteDirectoryRecursively_whenDirectoryWithFilesExists_shouldDeleteWholeDirectory()
            throws IOException {
        // given
        Path testDirectory = Paths.get("testDirectory");
        Path testSubdirectory = Paths.get(testDirectory.toString(), "testSubdirectory");
        Files.createDirectories(testSubdirectory);
        Path testFile1 = Paths.get(testDirectory.toString(), "testFile1.txt");
        Path testFile2 = Paths.get(testSubdirectory.toString(), "testFile2.txt");
        Files.createFile(testFile1);
        Files.createFile(testFile2);
        // when
        FileUtils.deleteDirectoryRecursively(testDirectory);
        // then
        assertFalse(Files.exists(testDirectory));
    }

    @Test
    public void saveMultipartFileDictionaryToFileSystem_shouldSaveFile()
            throws IOException {
        // given
        UUID requestId = UUID.randomUUID();
        Path testDirectory = Paths.get("target/testDirectory");
        MultipartFile dictionary = mock(MultipartFile.class);
        // when
        when(dictionary.getOriginalFilename()).thenReturn("test.txt");
        doNothing().when(dictionary).transferTo(any(Path.class));
        FileUtils.saveMultipartFileDictionaryToFileSystem(testDirectory, requestId, dictionary);
        // then
        verify(dictionary).transferTo(any(Path.class));
        // cleanup
        FileUtils.deleteDirectoryRecursively(testDirectory);
    }

    @Test
    public void saveFileDataDictionaryToFileSystem_shouldSaveFile()
            throws IOException {
        // given
        UUID requestId = UUID.randomUUID();
        Path testDirectory = Paths.get("target/testDirectory");
        FileData dictionaryFileData = new FileData(new byte[]{}, "test.txt");
        // when
        FileUtils.saveFileDataDictionaryToFileSystem(testDirectory, requestId, dictionaryFileData);
        // then
        assertTrue(Files.exists(Paths.get(
                testDirectory.toString(), requestId.toString(), dictionaryFileData.getFileName())));
        // cleanup
        FileUtils.deleteDirectoryRecursively(testDirectory);
    }

    @Test
    public void guessContentTypeFromName_allTypesShouldBeGuessed() {
        Map<String, String> fileNames = new HashMap();
        // Pair of file name and expected Content-Type
        fileNames.put("new_file.css", "text/css");
        fileNames.put("new_file.csv", "text/csv");
        fileNames.put("new_file.html", "text/html");
        fileNames.put("new_file.json", "application/json");
        fileNames.put("new_file.zip", "application/zip");
        fileNames.put("new_file.gz", "application/gzip");
        fileNames.put("new_file.xml", "application/xml");
        fileNames.put("new_file.xhtml", "application/xhtml+xml");
        fileNames.put("new_file.dtd", "application/xml-dtd");
        fileNames.put("new_file.doc", "application/msword");
        fileNames.put("new_file.yaml", "text/x-yaml");
        fileNames.put("new_file.pdf", "application/pdf");
        fileNames.put("new_file.js", "application/javascript|text/javascript");
        //fileNames.put("new_file.js", "application/javascript");
        fileNames.put("new_file.avif", "image/avif");
        fileNames.put("new_file.jpeg", "image/jpeg");
        fileNames.put("new_file.gif", "image/gif");
        fileNames.put("new_file.png", "image/png");
        fileNames.put("new_file.svg", "image/svg+xml");
        fileNames.put("new_file.tiff", "image/tiff");
        fileNames.put("new_file.webp", "image/webp");
        fileNames.put("new_file.another", "application/octet-stream");
        /*fileNames.keySet().forEach(fileName -> {
            assertEquals(fileNames.get(fileName), FileUtils.guessContentTypeFromName(fileName).toString());
        });*/
        fileNames.forEach((fileName, expected) -> {
            String actual = FileUtils.guessContentTypeFromName(fileName).toString();
            if (expected.contains("|")) {
                assertTrue(
                        Arrays.asList(expected.split("\\|")).contains(actual),
                        "Unexpected content type for " + fileName + ": " + actual
                );
            } else {
                assertEquals(expected, actual);
            }
        });
    }
}
