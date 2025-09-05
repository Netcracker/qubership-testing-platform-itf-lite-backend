package org.qubership.atp.itf.lite.backend.service.kafka.listeners;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.junit.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator;
import org.qubership.atp.itf.lite.backend.service.CertificateService;
import org.qubership.atp.itf.lite.backend.utils.FileUtils;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@ExtendWith(MockitoExtension.class)
@Isolated
public class ProjectEventKafkaListenerTest {

    private static ProjectEventKafkaListener eventListener = new ProjectEventKafkaListener();
    private UUID projectId;

    @Before
    public void set() {
        projectId = UUID.randomUUID();
    }

    @AfterEach
    public void after() throws IOException {
        File folder = new File(String.format(CertificateService.CERTIFICATE_FOLDER, projectId));
        FileUtils.deleteDirectoryRecursively(folder.toPath());
    }

    @Test
    void listen_FileInsideFolderShouldClean() throws IOException{
        //given
        File file = createFolderAndCopyFile(projectId);
        assertTrue(file.exists());
        //when
        eventListener.listen(EntitiesGenerator.generateRandomProjectEvent(projectId));
        //then
        assertTrue(file.getParentFile().exists());
        assertFalse(file.exists());
    }

    @Test
    void listenFileIsNotPresent_LogError() {
        //when
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        ((Logger) LoggerFactory.getLogger(ProjectEventKafkaListener.class)).addAppender(listAppender);

        eventListener.listen(EntitiesGenerator.generateRandomProjectEvent(projectId));

        //then
        assertTrue(listAppender.list.stream().anyMatch( m ->
                m.getFormattedMessage().contains("does not exist")));

    }

    private File createFolderAndCopyFile(UUID projectId) throws IOException{
        File folder = new File(String.format(CertificateService.CERTIFICATE_FOLDER, projectId));
        folder.mkdirs();
        File sourceFile = new File("./src/test/resources/identity.jks");
        File destinationFile = new File(folder.getAbsolutePath() + File.separator + "identity.jks");
        org.apache.commons.io.FileUtils.copyFile(sourceFile, destinationFile);
        return destinationFile;
    }
}
