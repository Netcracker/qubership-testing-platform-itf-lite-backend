package org.qubership.atp.itf.lite.backend.dataaccess.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.bson.BsonObjectId;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.itf.lite.backend.configuration.GridFsProperties;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileData;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.GridFSFindIterable;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

@ExtendWith(MockitoExtension.class)
public class GridFsRepositoryTest {

    private final ThreadLocal<GridFSBucket> gridFsBucket = new ThreadLocal<>();
    private final ThreadLocal<GridFsProperties> gridFsProperties = new ThreadLocal<>();
    private final ThreadLocal<GridFSDownloadStream> stream = new ThreadLocal<>();
    private final ThreadLocal<MongoDatabase> mongoDb = new ThreadLocal<>();
    private GridFsRepository gridFsRepository;

    final String METADATA_REQUEST_ID = "metadata" + "." + "requestId";
    final String METADATA_SESSION_ID = "metadata" + "." + "sessionId";
    final String METADATA_SESSION_REQUEST_ID = "metadata" + "." + "sessionRequestId";

    @BeforeEach
    public void setUp() {
        GridFSBucket gridFSBucketMock = mock(GridFSBucket.class);
        GridFsProperties gridFsPropertiesMock = mock(GridFsProperties.class);
        MongoDatabase mongoDbMock = mock(MongoDatabase.class);
        mongoDb.set(mongoDbMock);
        gridFsBucket.set(gridFSBucketMock);
        gridFsProperties.set(gridFsPropertiesMock);
        stream.set(mock(GridFSDownloadStream.class));
        gridFsRepository = new GridFsRepository(gridFSBucketMock, mongoDbMock, gridFsPropertiesMock,
                new ObjectMapper());
    }

    @Test
    public void saveDictionaryTest_shouldSaveDictionary() throws IOException {
        // given
        final UUID requestId = UUID.randomUUID();
        String creationTime = LocalDateTime.now().toString();
        InputStream dictionary = IOUtils.toInputStream("test", "UTF-8");
        String dictionaryName = "test.zip";
        // when
        when(gridFsProperties.get().getChunkSizeBytes()).thenReturn(1024);
        when(gridFsBucket.get().uploadFromStream(any(String.class), any(), any())).thenReturn(new ObjectId());
        // then
        gridFsRepository.saveDictionaryByRequestId(creationTime, requestId, dictionary, dictionaryName);
        verify(gridFsBucket.get()).uploadFromStream(any(String.class), any(), any());
    }

    @Test
    public void getFileDataTest_shouldGetFileDataSuccessfully() {
        // given
        final UUID requestId = UUID.randomUUID();
        String fileName = "test.txt";
        GridFSFile gridFSFile = new GridFSFile(new BsonObjectId(ObjectId.get()), fileName,
                1, 1024, new Date(), new Document());
        GridFSFindIterable iterable = mock(GridFSFindIterable.class);
        // when
        when(gridFsBucket.get().find(any(Document.class))).thenReturn(iterable);
        when(iterable.first()).thenReturn(gridFSFile);
        // then
        Optional<FileData> actualFileData = gridFsRepository.getRequestFileData(requestId);
        assertTrue(actualFileData.isPresent());
        assertEquals(new FileData(new byte[0], fileName), actualFileData.get());
    }

    @Test
    public void getFileDataTest_whenFileNotFound_shouldGetEmptyFileDataOptional() {
        // given
        final UUID requestId = UUID.randomUUID();
        GridFSFindIterable iterable = mock(GridFSFindIterable.class);
        // when
        when(gridFsBucket.get().find(any(Document.class))).thenReturn(iterable);
        when(iterable.first()).thenReturn(null);
        // then
        Optional<FileData> actualFileData = gridFsRepository.getRequestFileData(requestId);
        assertFalse(actualFileData.isPresent());
    }

    @Test
    public void removeDictionaryTest_whenFileIsFound_shouldRemoveFile() {
        // given
        final UUID requestId = UUID.randomUUID();
        ObjectId objectId = ObjectId.get();
        String fileName = "test.txt";
        GridFSFile gridFSFile = new GridFSFile(new BsonObjectId(objectId), fileName,
                1, 1024, new Date(), new Document());
        GridFSFindIterable iterable = mock(GridFSFindIterable.class);
        // when
        when(gridFsBucket.get().find(any(Document.class))).thenReturn(iterable);
        when(iterable.first()).thenReturn(gridFSFile);
        // then
        gridFsRepository.removeFileByRequestId(requestId);
        ArgumentCaptor<ObjectId> objectIdCaptor = ArgumentCaptor.forClass(ObjectId.class);
        verify(gridFsBucket.get()).delete(objectIdCaptor.capture());
        assertEquals(objectId, objectIdCaptor.getValue());
    }

    @Test
    public void removeDictionaryTest_whenFileNotFound_shouldNotCallRemoveMethod() {
        // given
        final UUID requestId = UUID.randomUUID();
        GridFSFindIterable iterable = mock(GridFSFindIterable.class);
        // when
        when(gridFsBucket.get().find(any(Document.class))).thenReturn(iterable);
        when(iterable.first()).thenReturn(null);
        // then
        gridFsRepository.removeFileByRequestId(requestId);
        verify(gridFsBucket.get(), times(0)).delete(any(ObjectId.class));
    }

    @Test
    public void removeDictionariesByDateTest_whenFileIsFound_shouldRemoveFile() {
        // given
        ObjectId objectId = ObjectId.get();
        String fileName = "test.txt";
        GridFSFile gridFSFile = new GridFSFile(new BsonObjectId(objectId), fileName,
                1, 1024, new Date(), new Document());
        GridFSFindIterable iterable = mock(GridFSFindIterable.class);
        // when
        when(gridFsBucket.get().find(any(Document.class))).thenReturn(iterable);
        when(iterable.into(any())).thenReturn(Collections.singletonList(gridFSFile));
        // then
        gridFsRepository.removeFilesByDate(1);
        ArgumentCaptor<ObjectId> objectIdCaptor = ArgumentCaptor.forClass(ObjectId.class);
        verify(gridFsBucket.get()).delete(objectIdCaptor.capture());
        assertEquals(objectId, objectIdCaptor.getValue());
    }

    @Test
    public void removeDictionariesByDateTest_whenFileNotFound_shouldNotCallRemoveMethod() {
        // given
        GridFSFindIterable iterable = mock(GridFSFindIterable.class);
        // when
        when(gridFsBucket.get().find(any(Document.class))).thenReturn(iterable);
        when(iterable.into(any())).thenReturn(Collections.emptyList());
        // then
        gridFsRepository.removeFilesByDate(1);
        verify(gridFsBucket.get(), times(0)).delete(any(ObjectId.class));
    }

    @Test
    public void testGetFileInfosByRequestIds_withMetadata_returnFileInfo() {
        final UUID requestId = UUID.randomUUID();
        GridFSFindIterable iterable = mock(GridFSFindIterable.class);
        when(gridFsBucket.get().find(any(Document.class))).thenReturn(iterable);
        ObjectId objectId = ObjectId.get();
        String fileName = "test.txt";
        UUID fileId = UUID.randomUUID();
        GridFSFile gridFSFile = new GridFSFile(new BsonObjectId(objectId), fileName,
                1, 1024, new Date(), new Document().append("requestId", requestId)
                .append("fileId", fileId));
        when(iterable.into(any())).thenReturn(Collections.singletonList(gridFSFile));

        Map<UUID, List<FileInfo>> result = gridFsRepository.getFileInfosByRequestIds(Collections.singleton(requestId));

        assertEquals(1, result.get(requestId).size());
        FileInfo fileInfo = result.get(requestId).get(0);
        assertEquals(fileId, fileInfo.getFileId());
        assertEquals(requestId, fileInfo.getRequestId());
        assertEquals(fileName, fileInfo.getFileName());
        assertEquals(new BsonObjectId(objectId), fileInfo.getId());
    }

    @Test
    public void testGetFileInfosByRequestIds_withoutMetadata_listOfFileInfosIsEmpty() {
        final UUID requestId = UUID.randomUUID();
        GridFSFindIterable iterable = mock(GridFSFindIterable.class);
        when(gridFsBucket.get().find(any(Document.class))).thenReturn(iterable);
        ObjectId objectId = ObjectId.get();
        String fileName = "test.txt";
        GridFSFile gridFSFile = new GridFSFile(new BsonObjectId(objectId), fileName,
                1, 1024, new Date(), new Document());
        when(iterable.into(any())).thenReturn(Collections.singletonList(gridFSFile));

        Map<UUID, List<FileInfo>> result = gridFsRepository.getFileInfosByRequestIds(Collections.singleton(requestId));

        assertEquals(0, result.get(requestId).size());
    }

    @Test
    public void testGetFileByFileInfos_withFileId_getInputStream() {
        final UUID requestId = UUID.randomUUID();
        final UUID fileId = UUID.randomUUID();
        FileInfo fileInfo = new FileInfo(new BsonObjectId(ObjectId.get()),
                "fileName.txt", requestId, fileId, "binary", "text/html", 0L);
        when(gridFsBucket.get().openDownloadStream(any(BsonValue.class))).thenReturn(stream.get());

        Map<UUID, InputStream> result = gridFsRepository.getFileByFileInfos(Collections.singletonList(fileInfo));

        assertEquals(1, result.size());
        assertTrue(result.keySet().contains(fileId));
    }

    @Test
    public void testGetFileByFileInfos_withoutFileId_getInputStream() {
        final UUID requestId = UUID.randomUUID();
        FileInfo fileInfo = new FileInfo(new BsonObjectId(ObjectId.get()),
                "fileName.txt", requestId, null, "binary", "text/html", 0L);
        when(gridFsBucket.get().openDownloadStream(any(BsonValue.class))).thenReturn(stream.get());

        Map<UUID, InputStream> result = gridFsRepository.getFileByFileInfos(Collections.singletonList(fileInfo));

        assertEquals(1, result.size());
        assertNotNull(fileInfo.getFileId());
    }

    @Test
    public void getFileDataTest_fileWithContentTypeAndFileId_returnFileData() {
        final UUID requestId = UUID.randomUUID();
        String fileName = "fileName.json";
        UUID fileId = UUID.randomUUID();
        Document metadata = new Document().append("fileId", fileId).append("contentType", "text/plain");
        GridFSFile gridFSFile = new GridFSFile(new BsonObjectId(ObjectId.get()), fileName,
                1, 1024, new Date(), metadata);
        GridFSFindIterable iterable = mock(GridFSFindIterable.class);
        when(gridFsBucket.get().find(any(Document.class))).thenReturn(iterable);
        when(iterable.first()).thenReturn(gridFSFile);

        Optional<FileData> result = gridFsRepository.getRequestFileData(requestId);

        assertTrue(result.isPresent());
        assertEquals(fileId, result.get().getFileId());
        assertEquals("text/plain", result.get().getContentType());
    }

    @Test
    public void moveFileFromSnapshotToRequest_fileWithContentTypeAndFileId_returnFileData() {
        final UUID requestId = UUID.randomUUID();
        final UUID sessionId = UUID.randomUUID();
        MongoCollection<Document> filesCollection = mock(MongoCollection.class);
        when(mongoDb.get().getCollection(eq("fs.files"))).thenReturn(filesCollection);

        gridFsRepository.moveFileFromSnapshotToRequest(sessionId, requestId);

        verify(filesCollection).updateMany(
                Filters.and(Filters.eq(METADATA_SESSION_ID, sessionId),
                        Filters.eq(METADATA_SESSION_REQUEST_ID,
                                requestId)),
                Updates.combine(
                        Updates.set(METADATA_REQUEST_ID, requestId),
                        Updates.unset(METADATA_SESSION_ID),
                        Updates.unset(METADATA_SESSION_REQUEST_ID)
                )
        );
    }

    @Test
    public void bulkRemoveFilesBySnapshotKeys_fileWithContentTypeAndFileId_deleted() {
        final UUID requestId = UUID.randomUUID();
        final UUID sessionId = UUID.randomUUID();
        String fileName = "fileName.json";
        UUID fileId = UUID.randomUUID();
        List<Bson> filters = new ArrayList<>();
        Collections.singletonList(requestId).forEach(id -> filters.add(Filters.and(new Document().append(METADATA_SESSION_REQUEST_ID,
                id),
                new Document().append(METADATA_SESSION_ID, sessionId))));
        Bson filter = Filters.or(filters);

        Document metadata = new Document().append("fileId", fileId).append("contentType", "text/plain");
        GridFSFile gridFSFile = new GridFSFile(new BsonObjectId(ObjectId.get()), fileName,
                1, 1024, new Date(), metadata);
        GridFSFindIterable iterable = mock(GridFSFindIterable.class);
        when(gridFsBucket.get().find(eq(filter))).thenReturn(iterable);
        when(iterable.into(any())).thenReturn(Collections.singletonList(gridFSFile));

        gridFsRepository.bulkRemoveFilesBySnapshotKeys(sessionId, Collections.singletonList(requestId));

        verify(gridFsBucket.get()).delete(gridFSFile.getObjectId());
    }


}
