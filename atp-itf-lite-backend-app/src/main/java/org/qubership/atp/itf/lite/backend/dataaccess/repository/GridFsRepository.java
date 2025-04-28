/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 */

package org.qubership.atp.itf.lite.backend.dataaccess.repository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.qubership.atp.itf.lite.backend.configuration.GridFsProperties;
import org.qubership.atp.itf.lite.backend.model.entities.FileBody;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileData;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import lombok.extern.slf4j.Slf4j;

@Repository("itf-lite-gridfs-repository")
@Slf4j
public class GridFsRepository {

    private static final String REQUEST_ID = "requestId";
    private static final String SESSION_REQUEST_ID = "sessionRequestId";
    private static final String SESSION_ID = "sessionId";
    private static final String METADATA_REQUEST_ID = "metadata" + "." + REQUEST_ID;
    private static final String METADATA_SESSION_ID = "metadata" + "." + SESSION_ID;
    private static final String METADATA_SESSION_REQUEST_ID = "metadata" + "." + SESSION_REQUEST_ID;
    private static final String UPLOAD_DATE = "uploadDate";
    private static final String METADATA_UPLOAD_DATE = "metadata" + "." + UPLOAD_DATE;
    private static final String ITF_LITE_DICTIONARY = "itfLiteDictionary";
    private static final String ITF_LITE_FILE = "itfLiteFile";
    private static final String BINARY_FILE = "binary";
    private static final String FILE_TYPE = "fileType";
    private static final String FILE_ID = "fileId";
    private static final String HISTORY = "history";
    private static final String CONTENT_TYPE = "contentType";
    private static final String METADATA_FILE_TYPE = "metadata" + "." + FILE_TYPE;
    private static final String METADATA_FILE_ID = "metadata" + "." + FILE_ID;
    private static final String METADATA_ITF_LITE_DICTIONARY = "metadata" + "." + ITF_LITE_DICTIONARY;

    private static final String TYPE = "type";
    private static final String CONTENT_TYPE_FILE = "file";

    private final GridFSBucket gridFsBucket;
    private final MongoDatabase gridFsMongoDatabase;
    private final GridFsProperties gridFsProperties;
    private final ObjectMapper objectMapper;

    /**
     * Constructor GridFsRepository.
     */
    @Autowired
    public GridFsRepository(GridFSBucket gridFsBucket, MongoDatabase gridFsMongoDatabase,
                            GridFsProperties gridFsProperties,
                            ObjectMapper objectMapper) {
        this.gridFsBucket = gridFsBucket;
        this.gridFsMongoDatabase = gridFsMongoDatabase;
        this.gridFsProperties = gridFsProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * Save dictionary for request id.
     *
     * @param creationTime          time of creation
     * @param requestId             request id
     * @param dictionaryInputStream dictionary
     * @param dictionaryName        dictionary name
     */
    public UUID saveDictionaryByRequestId(String creationTime, UUID requestId,
                                          InputStream dictionaryInputStream,
                                          String dictionaryName) {
        Document metadata = new Document(ITF_LITE_DICTIONARY, ITF_LITE_DICTIONARY)
                .append(CONTENT_TYPE, CONTENT_TYPE_FILE);
        return saveFileWithMetadata(metadata, dictionaryName, creationTime, requestId,
                REQUEST_ID, dictionaryInputStream);
    }

    /**
     * Save dictionary for session id.
     *
     * @param creationTime          time of creation
     * @param sessionId             session id
     * @param dictionaryInputStream dictionary
     * @param dictionaryName        dictionary name
     */
    public UUID saveDictionaryBySessionIdAndSessionRequestId(String creationTime, UUID sessionId,
                                                             UUID sessionRequestId,
                                                             InputStream dictionaryInputStream,
                                                             String dictionaryName) {
        Document metadata = new Document(ITF_LITE_DICTIONARY, ITF_LITE_DICTIONARY)
                .append(CONTENT_TYPE, CONTENT_TYPE_FILE)
                .append(SESSION_ID, sessionId)
                .append(SESSION_REQUEST_ID, sessionRequestId);
        return saveFileWithMetadata(metadata, dictionaryName, creationTime, dictionaryInputStream);
    }

    /**
     * Save binary file for request id.
     *
     * @param creationTime    time of creation
     * @param requestId       request id
     * @param fileInputStream dictionary
     * @param fileName        dictionary name
     * @param contentType     content type
     */
    public FileBody saveBinaryByRequestId(String creationTime, UUID requestId, InputStream fileInputStream,
                                          String fileName, String contentType) {
        Document metadata = new Document(FILE_TYPE, BINARY_FILE).append(CONTENT_TYPE, contentType);
        UUID fileId = saveFileWithMetadata(metadata, fileName, creationTime, requestId, REQUEST_ID,
                fileInputStream);
        return new FileBody(fileName, fileId);
    }

    /**
     * Save binary file for session id.
     *
     * @param creationTime     time of creation.
     * @param sessionId        session id.
     * @param sessionRequestId session request id.
     * @param fileInputStream  dictionary.
     * @param fileName         dictionary name.
     * @param contentType      content type.
     */
    public FileBody saveBinaryBySessionIdAndSessionRequestId(String creationTime,
                                                             UUID sessionId,
                                                             UUID sessionRequestId,
                                                             InputStream fileInputStream,
                                                             String fileName, String contentType) {
        Document metadata = new Document(FILE_TYPE, BINARY_FILE)
                .append(CONTENT_TYPE, contentType)
                .append(SESSION_ID, sessionId)
                .append(SESSION_REQUEST_ID, sessionRequestId);
        UUID fileId = saveFileWithMetadata(metadata, fileName, creationTime,
                fileInputStream);
        return new FileBody(fileName, fileId);
    }

    /**
     * Save binary file for request id.
     *
     * @param creationTime      time of creation
     * @param fileInputStream   dictionary
     * @param fileName          dictionary name
     * @return id
     */
    public UUID saveHistoryBinary(String creationTime, InputStream fileInputStream,
                                  String fileName) {
        Document metadata = new Document()
                .append(FILE_TYPE, BINARY_FILE)
                .append(HISTORY, true)
                .append(CONTENT_TYPE, CONTENT_TYPE_FILE);
        return saveFileWithMetadata(metadata, fileName, creationTime, null, REQUEST_ID, fileInputStream);
    }

    /**
     * Save gridFs repo file information.
     */
    public void saveByFileInfo(FileInfo fileInfo, InputStream inputStream) {
        GridFSUploadOptions uploadOptions = new GridFSUploadOptions()
                .chunkSizeBytes(gridFsProperties.getChunkSizeBytes())
                .metadata(
                        new Document()
                                .append(TYPE, CONTENT_TYPE_FILE)
                                .append(UPLOAD_DATE, LocalDateTime.now().toString())
                                .append(CONTENT_TYPE, fileInfo.getContentType())
                                .append(REQUEST_ID, fileInfo.getRequestId())
                                .append(FILE_ID,
                                        fileInfo.getFileId() == null ? UUID.randomUUID() :
                                                fileInfo.getFileId())
                                .append(FILE_TYPE, fileInfo.getFileType()));
        gridFsBucket.uploadFromStream(fileInfo.getFileName(), inputStream, uploadOptions);
    }

    private UUID saveFileWithMetadata(Document metadata,
                                      String fileName,
                                      String creationTime,
                                      UUID id,
                                      String metadataIdName,
                                      InputStream fileInputStream) {
        UUID fileId = UUID.randomUUID();
        GridFSUploadOptions uploadOptions = new GridFSUploadOptions()
                .chunkSizeBytes(gridFsProperties.getChunkSizeBytes())
                .metadata(metadata
                        .append(TYPE, CONTENT_TYPE_FILE)
                        .append(UPLOAD_DATE, creationTime)
                        .append(metadataIdName, id)
                        .append(FILE_ID, fileId));
        gridFsBucket.uploadFromStream(fileName, fileInputStream, uploadOptions);
        return fileId;
    }

    private UUID saveFileWithMetadata(Document metadata,
                                      String fileName,
                                      String creationTime,
                                      InputStream fileInputStream) {
        UUID fileId = UUID.randomUUID();
        GridFSUploadOptions uploadOptions = new GridFSUploadOptions()
                .chunkSizeBytes(gridFsProperties.getChunkSizeBytes())
                .metadata(metadata
                        .append(TYPE, CONTENT_TYPE_FILE)
                        .append(UPLOAD_DATE, creationTime)
                        .append(FILE_ID, fileId));
        gridFsBucket.uploadFromStream(fileName, fileInputStream, uploadOptions);
        return fileId;
    }

    /**
     * Save file for request id.
     *
     * @param creationTime          time of creation
     * @param requestId             request id
     * @param fileInputStream file
     * @param fileName        file name
     */
    public ObjectId saveFileByRequestId(String creationTime, UUID requestId, InputStream fileInputStream,
                                        String fileName, UUID fileId) {
        return saveFile(creationTime, requestId, REQUEST_ID, fileInputStream, fileName, fileId);
    }

    /**
     * Save file for request id.
     *
     * @param creationTime          time of creation
     * @param sessionId             session id
     * @param fileInputStream file
     * @param fileName        file name
     */
    public ObjectId saveFileBySessionId(String creationTime, UUID sessionId, InputStream fileInputStream,
                                        String fileName, UUID fileId) {
        return saveFile(creationTime, sessionId, SESSION_ID, fileInputStream, fileName, fileId);
    }

    /**
     * Save file for request id.
     *
     * @param creationTime          time of creation
     * @param fileInputStream file
     * @param fileName        file name
     */
    public ObjectId saveFile(String creationTime, UUID id, String metadataIdName, InputStream fileInputStream,
                             String fileName, UUID fileId) {
        GridFSUploadOptions uploadOptions = new GridFSUploadOptions()
                .chunkSizeBytes(gridFsProperties.getChunkSizeBytes())
                .metadata(new Document(TYPE, CONTENT_TYPE_FILE)
                        .append(UPLOAD_DATE, creationTime)
                        .append(CONTENT_TYPE, CONTENT_TYPE_FILE)
                        .append(metadataIdName, id)
                        .append(ITF_LITE_FILE, ITF_LITE_FILE)
                        .append(FILE_ID, fileId));
        return gridFsBucket.uploadFromStream(fileName, fileInputStream, uploadOptions);
    }

    /**
     * Retrieves dictionary from gridFs where metadata.requestId equals requestId.
     *
     * @param requestId to get dictionary
     * @return {@link Optional#empty()} if dictionary not found for specified requestId or {@link FileData}
     *          if it present in database
     */
    public Optional<FileData> getRequestFileData(UUID requestId) {
        Document filter = getFilter(requestId, METADATA_REQUEST_ID);
        return getFileDataByFilter(filter);
    }

    /**
     * Retrieves file from gridFs where metadata.fileId equals fileId.
     *
     * @param fileId file id
     * @return {@link Optional#empty()} if file not found for specified fileId or {@link FileData} if
     *          it present in database
     */
    public Optional<FileData> getFileDataByFileId(UUID fileId) {
        Document filter = new Document().append(METADATA_FILE_ID, fileId);
        return getFileDataByFilter(filter);
    }

    /**
     * Get map with key is request id and value is list of file infos.
     * @param requestIds request ids.
     * @return {@link java.util.HashMap}
     */
    public Map<UUID, List<FileInfo>> getFileInfosByRequestIds(Set<UUID> requestIds) {
        Map<UUID, List<FileInfo>> filesToRequestId = new HashMap<>();
        requestIds.forEach(id -> filesToRequestId.put(id, getFileInfosByRequestId(id)));
        return filesToRequestId;
    }

    /**
     * Get file infos.
     *
     * @param requestId request id.
     * @return list of {@link FileInfo}
     */
    public List<FileInfo> getFileInfosByRequestId(UUID requestId) {
        List<FileInfo> fileInfos = new ArrayList<>();
        findAllByFilter(getFilter(requestId, METADATA_REQUEST_ID)).forEach(gridFsFile -> {
            Document metadata = gridFsFile.getMetadata();
            if (metadata != null) {
                try {
                    FileInfo info = objectMapper.readValue(
                            objectMapper.writeValueAsString(metadata), FileInfo.class);
                    info.setId(gridFsFile.getId());
                    info.setFileName(gridFsFile.getFilename());
                    fileInfos.add(info);
                } catch (Exception e) {
                    log.error("Error when creating file info for request {}. Metadata {}", requestId,
                            metadata, e);
                }
            }
        });
        return fileInfos;
    }

    /**
     * Get file info by request id.
     *
     * @param requestId request id.
     * @return {@link FileInfo}
     */
    public FileInfo getFileInfoByRequestId(UUID requestId) {
        return getFileInfo(requestId, METADATA_REQUEST_ID);
    }

    /**
     * Get file info by request id.
     *
     * @param fileId file id.
     * @return {@link FileInfo}
     */
    public FileInfo getFileInfoByFileId(UUID fileId) {
        return getFileInfo(fileId, METADATA_FILE_ID);
    }

    /**
     * Get file info by request id.
     *
     * @param id request id.
     * @param metaInfoIdName id param name.
     * @return {@link FileInfo}
     */
    public FileInfo getFileInfo(UUID id, String metaInfoIdName) {
        GridFSFile gridFsFile = findByFilter(getFilter(id, metaInfoIdName));
        Document metadata = gridFsFile.getMetadata();
        if (metadata != null) {
            try {
                FileInfo info = objectMapper.readValue(
                        objectMapper.writeValueAsString(metadata), FileInfo.class);
                info.setId(gridFsFile.getId());
                info.setFileName(gridFsFile.getFilename());
                info.setSize(gridFsFile.getLength());
                return info;
            } catch (Exception e) {
                log.error("Error when creating file info for {}:{}. Metadata {}", metaInfoIdName, id,
                        metadata, e);
            }
        }
        return null;
    }

    /**
     * Get map with key is file id and value is file input stream.
     * @param fileInfos info about files.
     * @return {@link java.util.HashMap}
     */
    public Map<UUID, InputStream> getFileByFileInfos(List<FileInfo> fileInfos) {
        Map<UUID, InputStream> filesToFileId = new HashMap<>();
        fileInfos.forEach(file -> {
            if (file.getFileId() == null) {
                UUID fileId = UUID.randomUUID();
                filesToFileId.put(fileId, gridFsBucket.openDownloadStream(file.getId()));
                file.setFileId(fileId);
            }
            filesToFileId.put(file.getFileId(), gridFsBucket.openDownloadStream(file.getId()));
        });
        return filesToFileId;
    }

    /**
     * Get map with key is file id and value is file input stream.
     *
     * @param fileInfo info about file.
     * @return {@link InputStream}
     */
    public InputStream getFileByFileInfo(FileInfo fileInfo) {
        return gridFsBucket.openDownloadStream(fileInfo.getId());
    }

    /**
     * Remove dictionary for requestId.
     *
     * @param requestId to remove dictionary from gridFs
     */
    public void removeFileByRequestId(UUID requestId) {
        Document filter = getFilter(requestId, METADATA_REQUEST_ID);
        GridFSFile result = findByFilter(filter);
        if (Objects.nonNull(result)) {
            gridFsBucket.delete(result.getObjectId());
        }
    }

    /**
     * Find list gridFs files by id request and remove all .
     */
    public void removeAllFilesByRequestId(UUID requestId) {
        Document filter = getFilter(requestId, METADATA_REQUEST_ID);
        List<GridFSFile> result = findAllByFilter(filter);
        if (!CollectionUtils.isEmpty(result)) {
            result.forEach(file -> gridFsBucket.delete(file.getObjectId()));
        }
    }

    /**
     * Find list gridFs files by id session and remove all .
     */
    public void removeAllFilesBySessionIdAndSessionRequestId(UUID sessionId, UUID sessionRequestId) {
        Document filter = getFilter(sessionId, METADATA_SESSION_ID);
        filter = addFilter(filter, sessionRequestId, METADATA_SESSION_REQUEST_ID);
        List<GridFSFile> result = findAllByFilter(filter);
        if (!CollectionUtils.isEmpty(result)) {
            result.forEach(file -> gridFsBucket.delete(file.getObjectId()));
        }
    }

    /**
     * Remove file by fileId.
     *
     * @param fileId to remove file from gridFs
     */
    public void removeFileByFileId(UUID fileId) {
        Document filter = new Document(METADATA_FILE_ID, fileId);
        GridFSFile result = findByFilter(filter);
        if (Objects.nonNull(result)) {
            gridFsBucket.delete(result.getObjectId());
        }
    }

    /**
     * Remove bynary file by request id.
     */
    public void removeBinaryFileByRequestId(UUID requestId) {
        Document filter = getFilter(requestId, METADATA_REQUEST_ID);
        filter.append(METADATA_FILE_TYPE, BINARY_FILE);
        GridFSFile result = findByFilter(filter);
        if (Objects.nonNull(result)) {
            gridFsBucket.delete(result.getObjectId());
        }
    }

    /**
     * Remove dictionaries by date.
     * @param days dictionaries will be removed after number of days
     */
    public void removeFilesByDate(Integer days) {
        LocalDateTime validUploadDate = LocalDateTime.now().minusDays(days);
        Document filter = new Document()
                .append(METADATA_ITF_LITE_DICTIONARY, ITF_LITE_DICTIONARY)
                .append(METADATA_UPLOAD_DATE, "{$lt: " + validUploadDate + "}");
        List<GridFSFile> result = findAllByFilter(filter);
        if (!CollectionUtils.isEmpty(result)) {
            result.forEach(file -> gridFsBucket.delete(file.getObjectId()));
        }
    }

    private FileData createFileDataFromFileInDb(GridFSFile file) {
        ByteArrayOutputStream bos = downloadFileFromDbToByteStream(file);
        return composeFileDataFromGridFsFileAndItsContent(file, bos);
    }

    private ByteArrayOutputStream downloadFileFromDbToByteStream(GridFSFile file) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        gridFsBucket.downloadToStream(file.getObjectId(), bos);
        return bos;
    }

    private FileData composeFileDataFromGridFsFileAndItsContent(GridFSFile file, ByteArrayOutputStream bos) {
        FileData fileData = new FileData();
        fileData.setContent(bos.toByteArray());
        fileData.setFileName(file.getFilename());
        Document metadata = file.getMetadata();
        if (metadata != null) {
            if (metadata.containsKey(FILE_ID)) {
                fileData.setFileId(UUID.fromString(String.valueOf(metadata.get(FILE_ID))));
            }
            fileData.setContentType(metadata.getString(CONTENT_TYPE));
        }
        return fileData;
    }

    private Optional<FileData> getFileDataByFilter(Document filter) {
        GridFSFile gridFsDictionary = findByFilter(filter);
        if (Objects.isNull(gridFsDictionary)) {
            log.debug("Cannot get dictionary by filter {}", filter);
            return Optional.empty();
        }
        FileData fileData = createFileDataFromFileInDb(gridFsDictionary);
        log.debug("File was found {} by filter {}", gridFsDictionary.getFilename(), filter);
        return Optional.of(fileData);
    }

    private GridFSFile findByFilter(Document filter) {
        return gridFsBucket.find(filter).first();
    }

    private List<GridFSFile> findAllByFilter(Bson filter) {
        List<GridFSFile> gridFsFiles = new ArrayList<>();
        return gridFsBucket.find(filter).into(gridFsFiles);
    }

    private Document getFilter(UUID id, String metadataField) {
        return new Document().append(metadataField, id);
    }

    private Document addFilter(Document document, UUID id, String metadataField) {
        return document.append(metadataField, id);
    }

    /**
     * Get all itf-lite files.
     *
     * @param requestId requestId
     * @return list of itf-lite files
     */
    public List<GridFSFile> findAllFilesByRequestId(UUID requestId) {
        Document filter = new Document()
                .append(METADATA_REQUEST_ID, requestId);
        return findAllByFilter(filter);
    }

    /**
     * Configure list of file data by GridFSFile.
     *
     * @param requestId for find files
     * @return list
     */
    public List<FileData> getFilesDataList(UUID requestId) {
        List<GridFSFile> files = findAllFilesByRequestId(requestId);
        List<FileData> res = new ArrayList<>();
        files.forEach(gridFSFile -> res.add(createFileDataFromFileInDb(gridFSFile)));
        return res;
    }

    public GridFSFile findByFileId(UUID fileId) {
        Document filter = new Document().append(METADATA_FILE_ID, fileId);
        return findByFilter(filter);
    }

    /**
     * Copy file by request id or request id in metadata.
     */
    public UUID copyFileWithFileId(UUID fileId, UUID newRequestId) {
        log.debug("Copying file with id {} to request id {}", fileId, newRequestId);
        GridFSFile file = findByFileId(fileId);
        if (Objects.nonNull(file)) {
            UUID newFileId = UUID.randomUUID();
            FileData fd = createFileDataFromFileInDb(file);
            saveFileByRequestId(
                    file.getMetadata().get(UPLOAD_DATE, String.class),
                    Objects.nonNull(newRequestId) ? newRequestId : file.getMetadata().get(REQUEST_ID,
                            UUID.class),
                    new ByteArrayInputStream(fd.getContent()),
                    file.getFilename(),
                    newFileId);
            return newFileId;
        }
        log.warn("File with id {} not found", fileId);
        return null;
    }

    /**
     * Move files from session to request while saving.
     */
    public void moveFileFromSnapshotToRequest(UUID sessionId, UUID requestId) {
        MongoCollection<Document> filesCollection = gridFsMongoDatabase.getCollection("fs.files");
        filesCollection.updateMany(
                Filters.and(Filters.eq(METADATA_SESSION_ID, sessionId), Filters.eq(METADATA_SESSION_REQUEST_ID,
                        requestId)),
                Updates.combine(
                        Updates.set(METADATA_REQUEST_ID, requestId),
                        Updates.unset(METADATA_SESSION_ID),
                        Updates.unset(METADATA_SESSION_REQUEST_ID)
                )
        );
    }

    /**
     * Remove file by session Id.
     */
    public void removeFileBySessionId(UUID sessionId) {
        Document filter = getFilter(sessionId, METADATA_SESSION_ID);
        GridFSFile result = findByFilter(filter);
        if (Objects.nonNull(result)) {
            gridFsBucket.delete(result.getObjectId());
        }
    }

    /**
     * Remove files by snapshot keys.
     */
    public void bulkRemoveFilesBySnapshotKeys(UUID sessionId, List<UUID> requestIds) {
        List<Bson> filters = new ArrayList<>();
        requestIds.forEach(requestId -> filters.add(Filters.and(getFilter(requestId,
                METADATA_SESSION_REQUEST_ID),
                getFilter(sessionId, METADATA_SESSION_ID))));
        Bson filter = Filters.or(filters);
        List<GridFSFile> result = findAllByFilter(filter);
        if (!CollectionUtils.isEmpty(result)) {
            result.forEach(file -> gridFsBucket.delete(file.getObjectId()));
        }
    }

    /**
     * Get file data by session id and request id.
     */
    public Optional<FileData> getFileDataBySessionIdAndRequestId(UUID sessionId, UUID sessionRequestId) {
        Document filter = new Document().append(METADATA_SESSION_ID, sessionId);
        filter = addFilter(filter, sessionRequestId, METADATA_SESSION_REQUEST_ID);
        return getFileDataByFilter(filter);
    }
}


