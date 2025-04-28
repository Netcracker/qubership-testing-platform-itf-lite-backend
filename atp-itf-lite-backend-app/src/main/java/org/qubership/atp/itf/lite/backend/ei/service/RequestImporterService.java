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

package org.qubership.atp.itf.lite.backend.ei.service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.modelmapper.ModelMapper;
import org.qubership.atp.ei.node.dto.ExportImportData;
import org.qubership.atp.ei.node.services.ObjectLoaderFromDiskService;
import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteImportRequestFileLoadException;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.model.entities.auth.BasicRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.BearerRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.InheritFromParentRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.OAuth2RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileInfo;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.service.GridFsService;
import org.qubership.atp.itf.lite.backend.service.RequestService;
import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.qubership.atp.itf.lite.backend.utils.JsonObject;
import org.qubership.atp.itf.lite.backend.utils.StreamUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestImporterService {
    private static final String DESCRIPTOR_FILE_NAME_REGEXP =
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.json";

    private final ObjectLoaderFromDiskService objectLoaderFromDiskService;
    private final RequestService requestService;
    private final ModelMapper modelMapper;
    private final GridFsService gridFsService;

    /**
     * Imports requests.
     *
     * @param workDir    directory where request's files store
     * @param importData data about imported objects
     */
    public void importRequests(Path workDir, ExportImportData importData) {
        Map<UUID, Path> requestFiles = getListOfRequests(workDir);
        Map<UUID, UUID> replacementMap = importData.getReplacementMap();
        log.debug("importRequests list: {}", requestFiles);
        JSONParser parser = new JSONParser();
        boolean isReplacement = importData.isInterProjectImport() || importData.isCreateNewProject();
        List<Request> parsedRequests = new ArrayList<>();
        requestFiles.forEach((requestId, filePath) -> {
            log.debug("importRequest starts import: {}.", requestId);
            Request requestObject = loadRequest(filePath, replacementMap, isReplacement, parser);
            log.debug("Imports request:{}", requestObject);
            if (requestObject == null) {
                final String path = filePath.toString();
                log.error("Failed to upload file using path: {}", filePath);
                throw new ItfLiteImportRequestFileLoadException(path);
            }
            requestObject.setSourceId(requestId);
            parsedRequests.add(requestObject);
        });

        Map<UUID, List<Request>> requestsGroupedByFolders = parsedRequests.stream()
                .filter(request -> nonNull(request.getFolderId()))
                .collect(Collectors.groupingBy(Request::getFolderId));

        for (Map.Entry<UUID, List<Request>> requestsByFolder: requestsGroupedByFolders.entrySet()) {
            // list is not empty or null
            UUID projectId = requestsByFolder.getValue().get(0).getProjectId();
            List<Request> folderRequests = requestService.getAllRequestsByProjectIdFolderIdsRequestIds(
                    projectId, Sets.newHashSet(requestsByFolder.getKey()), null);
            requestsByFolder.getValue().forEach(requestObject -> {
                List<Request> requestsWithoutCurrentRequest = requestsByFolder.getValue()
                        .stream()
                        .filter(request -> !request.equals(requestObject))
                        .collect(Collectors.toList());
                requestsWithoutCurrentRequest.addAll(folderRequests);
                // distinct by id to exclude requests with same id in
                requestsWithoutCurrentRequest = requestsWithoutCurrentRequest
                        .stream()
                        .filter(StreamUtils.distinctByKey(Request::getId))
                        .collect(Collectors.toList());
                requestService.addPostfixIfNameIsTaken(requestsWithoutCurrentRequest, requestObject);
            });
        }

        List<Request> rootFolderRequestObjects = parsedRequests.stream()
                .filter(request -> isNull(request.getFolderId()))
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(rootFolderRequestObjects)) {
            UUID projectId = rootFolderRequestObjects.get(0).getProjectId();
            List<Request> rootFolderRequests = requestService.getAllRequestsByProjectIdFolderIdsRequestIds(
                    projectId, null, null);
            rootFolderRequestObjects.forEach(requestObject -> {
                List<Request> requestsWithoutCurrentRequest = rootFolderRequestObjects
                        .stream()
                        .filter(request -> !request.equals(requestObject))
                        .collect(Collectors.toList());
                requestsWithoutCurrentRequest.addAll(rootFolderRequests);
                requestsWithoutCurrentRequest = requestsWithoutCurrentRequest
                        .stream()
                        .filter(StreamUtils.distinctByKey(Request::getId))
                        .collect(Collectors.toList());
                requestService.addPostfixIfNameIsTaken(requestsWithoutCurrentRequest, requestObject);
            });
        }

        // add all requests grouped by folders
        rootFolderRequestObjects.addAll(requestsGroupedByFolders.entrySet().stream().flatMap(uuidListEntry ->
                        uuidListEntry.getValue().stream())
                .collect(Collectors.toList()));
        requestService.saveAll(rootFolderRequestObjects);
    }

    /**
     * Import files in needed directory.
     */
    public void importFiles(ExportImportData importData, Path workDir) {
        log.info("start importFiles(workDir: {})", workDir);
        Map<UUID, Path> list = getFileDescriptors(workDir, Constants.FILES);
        log.debug("importFiles list: {}", list);
        list.forEach((id, path) -> {
            log.debug("importFiles start import id: {}", id);
            FileInfo fileInfo;
            if (importData.isCreateNewProject() || importData.isInterProjectImport()) {
                Map<UUID, UUID> map = new HashMap<>(importData.getReplacementMap());
                fileInfo = objectLoaderFromDiskService.loadFileAsObjectWithReplacementMap(path, FileInfo.class, map);
            } else {
                fileInfo = objectLoaderFromDiskService.loadFileAsObject(path, FileInfo.class);
            }

            log.debug("importFiles import fileData: {}", fileInfo);
            if (fileInfo == null) {
                log.info("Selected file is no a file descriptor. Path {}", path.toString());
                return;
            }
            Path filePath = path.getParent().resolve(id.toString());
            try (InputStream inputStream = Files.newInputStream(filePath)) {
                gridFsService.saveByFileInfo(fileInfo, inputStream);
            } catch (Exception e) {
                log.error("Cannot read file {}. File Data {}", filePath, fileInfo, e);
            }
        });
        log.info("end importFiles()");
    }

    private Map<UUID, Path> getFileDescriptors(Path workDir, String folderName) {
        Path dirWithObjects = workDir.resolve(folderName);

        log.debug("start getListOfObjectIdByFolder(dirWithObjects: {})", dirWithObjects);
        Map<UUID, Path> res = new HashMap<>();
        try (Stream<Path> result = Files.find(dirWithObjects, 3,
                (path, basicFileAttributes) -> basicFileAttributes.isRegularFile()
                        && path.getFileName().toString().matches(DESCRIPTOR_FILE_NAME_REGEXP))) {
            result.forEach(pathToFile -> {
                UUID fileId;
                try {
                    fileId = UUID.fromString(pathToFile.getFileName().toString().split("\\.")[0]);
                } catch (IllegalArgumentException e) {
                    log.warn("Can't get uuid from filename.", e);
                    return;
                }
                res.put(fileId, pathToFile);
            });
        } catch (Exception e) {
            log.error("Cannot find dir {}", dirWithObjects, e);
        }
        log.debug("end getListOfObjectIdByFolder(): {}", res);
        return res;
    }

    /**
     * Reads and parses request json file.
     * @param filePath request file path
     * @param replacementMap replacement map
     * @param isReplacement if true then need to replace request authorization id
     * @param parser json parser
     * @return Request
     */
    private Request loadRequest(Path filePath, Map<UUID, UUID> replacementMap, boolean isReplacement,
                                JSONParser parser) {
        Request request;
        if (isReplacement) {
            log.debug("Load request by path [{}] with replacementMap: {}", filePath, replacementMap);
            request = objectLoaderFromDiskService.loadFileAsObjectWithReplacementMap(filePath, Request.class,
                    replacementMap, true, false);
        } else {
            log.debug("Load request by path [{}] without replacementMap", filePath);
            request = objectLoaderFromDiskService.loadFileAsObject(filePath, Request.class);
        }
        if (nonNull(request)) {
            log.debug("RequestAuthorization parsing for requestId = {}", request.getId());
            request.setAuthorization(parseRequestAuthorization(filePath, replacementMap, isReplacement,
                    request.getAuthorization(), parser));
            return request;
        }
        return null;
    }

    /**
     * Reads request json file and parses request authorization according to RequestAuthorizationType.
     * @param filePath request file path
     * @param replacementMap replacement map
     * @param isReplacement if true then need to replace request authorization id
     * @param requestAuthorization request authorization
     * @param parser json parser
     * @return RequestAuthorization extension according to RequestAuthorizationType
     */
    private RequestAuthorization parseRequestAuthorization(Path filePath, Map<UUID, UUID> replacementMap,
                                                           boolean isReplacement,
                                                           RequestAuthorization requestAuthorization,
                                                           JSONParser parser) {
        if (nonNull(requestAuthorization)) {
            RequestAuthorizationType authType = requestAuthorization.getType();
            switch (authType) {
                case OAUTH2:
                    return prepareRequestAuth(filePath, replacementMap, isReplacement, parser,
                            OAuth2RequestAuthorization.class);
                case BASIC:
                    return prepareRequestAuth(filePath, replacementMap, isReplacement, parser,
                            BasicRequestAuthorization.class);
                case BEARER:
                    return prepareRequestAuth(filePath, replacementMap, isReplacement, parser,
                            BearerRequestAuthorization.class);
                case INHERIT_FROM_PARENT:
                    return prepareRequestAuth(filePath, replacementMap, isReplacement, parser,
                            InheritFromParentRequestAuthorization.class);
                default:
                    log.warn("Request with type {} will not be parsing", authType);
                    break;
            }
        }
        log.debug("Request authorization not found in {}", filePath);
        return requestAuthorization;
    }

    /**
     * Prepare request authorization by auth type.
     *
     * @param filePath path to json file
     * @param replacementMap replacement map
     * @param isReplacement flag is need replacement?
     * @param parser {@link JSONParser}
     * @param neededClass for cast to this class
     * @return preparing request authorization
     */
    private <T extends RequestAuthorization> T prepareRequestAuth(Path filePath, Map<UUID, UUID> replacementMap,
                                                                  boolean isReplacement,
                                                                  JSONParser parser,
                                                                  Class<T> neededClass) {
        try {
            log.debug("Read request from {}", filePath);
            String requestString = IOUtils.toString(Files.newInputStream(filePath));
            JsonObject parsedRequest = new JsonObject(parser.parse(requestString));
            JsonObject authorizationJson = parsedRequest.getObject("authorization");
            log.debug("Map parsed request authorization into OAuth2RequestAuthorization");
            T parsedRequestAuthorization =
                    modelMapper.map(authorizationJson.getObj(), neededClass);
            UUID parsedRequestAuthorizationId = parsedRequestAuthorization.getId();
            if (isReplacement && replacementMap.containsKey(parsedRequestAuthorizationId)) {
                log.debug("Replace request authorization id");
                parsedRequestAuthorization.setId(replacementMap.get(parsedRequestAuthorizationId));
            }
            return parsedRequestAuthorization;
        } catch (IOException | ParseException e) {
            log.error("Can't get authorization from file {}.", filePath, e);
        }
        return null;
    }

    /**
     * Gets existing by source id.
     *
     * @param workDir        work directory
     * @param replacementMap replacement map for object loader
     * @return the existing by source id
     */
    public Map<UUID, UUID> getSourceTargetMap(Path workDir, Map<UUID, UUID> replacementMap) {
        log.debug("Get source target replacement map");
        Map<UUID, UUID> result = new HashMap<>();
        Map<UUID, Path> objectsToImport = getListOfRequests(workDir);
        objectsToImport.forEach((uuid, filePath) -> {
            Request requestObject = objectLoaderFromDiskService
                    .loadFileAsObjectWithReplacementMap(filePath, Request.class, replacementMap);
            Request existingObject = requestService.getByProjectIdAndSourceId(requestObject.getProjectId(), uuid);
            if (existingObject == null) {
                log.debug("Request by projectId: [{}] and sourceId: [{}] not found",
                        requestObject.getProjectId(), uuid);
                log.debug("Put {}: null to replacementMap", uuid);
                result.put(uuid, null);
            } else {
                log.debug("Request by projectId: [{}] and sourceId: [{}] found", requestObject.getProjectId(), uuid);
                log.debug("Put {}: {} to replacementMap", uuid, existingObject.getId());
                result.put(uuid, existingObject.getId());
            }
        });
        return result;
    }

    /**
     * Returns replacementMap for request parameters and headers.
     *
     * @param workDir work directory
     */
    public Map<UUID, UUID> getReplacementMap(Path workDir) {
        log.debug("Get replacementMap for requests");
        Map<UUID, UUID> result = new HashMap<>();
        Map<UUID, Path> objectsToImport = getListOfRequests(workDir);
        objectsToImport.forEach((uuid, filePath) -> {
            Request requestObject = objectLoaderFromDiskService.loadFileAsObject(filePath, Request.class);
            RequestAuthorization auth = requestObject.getAuthorization();
            if (auth != null) {
                UUID newId = UUID.randomUUID();
                log.debug("Put new uuid for authorization - {}: {}", auth.getId(), newId);
                result.put(auth.getId(), newId);
            }
        });
        return result;
    }

    private Map<UUID, Path> getListOfRequests(Path dir) {
        Map<UUID, Path> objectsToImport = objectLoaderFromDiskService.getListOfObjects(dir, HttpRequest.class);
        return objectsToImport;
    }
}
