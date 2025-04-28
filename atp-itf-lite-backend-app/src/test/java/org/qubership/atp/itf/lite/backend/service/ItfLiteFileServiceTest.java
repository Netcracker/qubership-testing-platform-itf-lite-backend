package org.qubership.atp.itf.lite.backend.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.qubership.atp.itf.lite.backend.components.replacer.ContextVariablesReplacer;
import org.qubership.atp.itf.lite.backend.model.entities.FileBody;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = ItfLiteFileService.class, properties = {"atp.itf.lite.clean.file.cache.time-sec=3600"})
@MockBeans({
        @MockBean(GridFsService.class)
})
@Isolated
public class ItfLiteFileServiceTest {
    private static final UUID requestUuid = UUID.fromString("38812ea8-e0b2-4acd-ab1e-34be7ad9b502");
    private static final UUID requestUuid2 = UUID.fromString("38812ea8-e0b2-4acd-ab1e-34be7ad9b501");
    @Autowired
    ItfLiteFileService itfLiteFileService;
    @MockBean
    private GridFsService gridFsService;
    @MockBean
    private ContextVariablesReplacer replacer;
    private Path path;
    private Path pathToFile;

    @BeforeEach
    public void setUp() throws Exception {
        path = Paths.get("src/test/resources/tests");
        pathToFile = Paths.get(path.toString(), requestUuid2.toString(), "file.txt");
        Files.write(pathToFile, "string".getBytes());
    }

    @Test
    public void testGetRequestPathToBinaryFile_OldFile_GetFileFromGridFs() throws Exception {

        itfLiteFileService.getRequestPathToFile(requestUuid, new Date(), path);

        verify(gridFsService, times(1)).downloadFile(any());
    }

    @Test
    public void testCopyFileForCopiedRequest_OldFileExists_CreateNewDirectoryAndFile() throws Exception {
        UUID newUuid = UUID.randomUUID();
        when(gridFsService.saveBinaryByRequestId(any(), any(), any(), any(), any()))
                .thenReturn(new FileBody("name", UUID.randomUUID()));

        itfLiteFileService.copyFileForCopiedRequest(requestUuid2, new Date(1707413388860L), newUuid, path);

        Path resultFile = Paths.get(path.toString(), newUuid.toString(), "file.txt");
        Assertions.assertTrue(resultFile.toFile().exists());
        Files.delete(resultFile);
        Files.delete(Paths.get(path.toString(), newUuid.toString()));
    }

    @Test
    public void testGetRequestPathToBinaryFile_NewFile_GetFileFromFileSystem() throws Exception {
        Date currentDate = new Date(pathToFile.toFile().lastModified() - 1000);

        itfLiteFileService.getRequestPathToFile(requestUuid2, currentDate, path);

        verify(gridFsService, times(0)).downloadFile(any());
    }

    @Test
    public void testGetFileAsMultipartFileByRequestId_OldFile_CreateMultipartFile() throws Exception {
        Date currentDate = new Date(pathToFile.toFile().lastModified() - 1000);

        Optional<MultipartFile> file = itfLiteFileService.getFileAsMultipartFileByRequestId(requestUuid2, currentDate, path);

        Assertions.assertEquals("file.txt", file.get().getOriginalFilename());
    }

    @Test
    public void getRequestBinaryFileTest_withCheckContentType_getFileDataWithContentType() throws Exception {
        Date currentDate = new Date(pathToFile.toFile().lastModified() - 1000);

        FileData result = itfLiteFileService.getRequestFileData(requestUuid2, currentDate, path);

        Assertions.assertEquals("text/plain", result.getContentType());
    }

    @Test
    public void getFileAsMultipartFileByRequestIdTest_withCheckContentType_getMultipartFileWithContentType()
            throws Exception {
        Date currentDate = new Date(pathToFile.toFile().lastModified() - 1000);

        Optional<MultipartFile> result = itfLiteFileService.getFileAsMultipartFileByRequestId(requestUuid2, currentDate, path);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("text/plain", result.get().getContentType());
    }
}
