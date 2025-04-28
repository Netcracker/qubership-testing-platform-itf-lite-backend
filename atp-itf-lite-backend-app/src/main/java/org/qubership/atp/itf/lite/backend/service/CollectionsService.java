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

package org.qubership.atp.itf.lite.backend.service;

import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpException;
import org.qubership.atp.ei.ntt.dto.AbstractTestScenario;
import org.qubership.atp.itf.lite.backend.catalog.models.Directive;
import org.qubership.atp.itf.lite.backend.catalog.models.DirectiveEnum;
import org.qubership.atp.itf.lite.backend.enums.EntityType;
import org.qubership.atp.itf.lite.backend.enums.ImportCollectionError;
import org.qubership.atp.itf.lite.backend.enums.ImportCollectionStatus;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.enums.ValueType;
import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.qubership.atp.itf.lite.backend.enums.http.RequestBodyMode;
import org.qubership.atp.itf.lite.backend.enums.http.RequestBodyType;
import org.qubership.atp.itf.lite.backend.exceptions.collection.ItfLiteImportCollectionFileExtensionNotFoundException;
import org.qubership.atp.itf.lite.backend.exceptions.collection.ItfLiteImportCollectionFileParseProcessException;
import org.qubership.atp.itf.lite.backend.exceptions.collection.ItfLiteImportCollectionFileProcessException;
import org.qubership.atp.itf.lite.backend.feign.dto.ActionEntityDto;
import org.qubership.atp.itf.lite.backend.feign.dto.DirectiveDto;
import org.qubership.atp.itf.lite.backend.feign.dto.EnrichedCompoundDto;
import org.qubership.atp.itf.lite.backend.feign.dto.EnrichedScenarioDto;
import org.qubership.atp.itf.lite.backend.feign.dto.ExecuteRequestDto;
import org.qubership.atp.itf.lite.backend.feign.dto.TypeEnum;
import org.qubership.atp.itf.lite.backend.feign.dto.history.ActionParameterDto;
import org.qubership.atp.itf.lite.backend.feign.service.CatalogueService;
import org.qubership.atp.itf.lite.backend.feign.service.EnvironmentFeignService;
import org.qubership.atp.itf.lite.backend.feign.service.RamService;
import org.qubership.atp.itf.lite.backend.model.api.request.CollectionExecuteRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderUpsetRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.ImportCollectionsRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.GroupResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.ImportCollectionsResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.environments.System;
import org.qubership.atp.itf.lite.backend.model.entities.Cookie;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.model.entities.RequestBody;
import org.qubership.atp.itf.lite.backend.model.entities.auth.InheritFromParentRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.http.FormDataPart;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestParam;
import org.qubership.atp.itf.lite.backend.model.entities.http.methods.HttpMethod;
import org.qubership.atp.itf.lite.backend.utils.AuthorizationUtils;
import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.qubership.atp.itf.lite.backend.utils.StreamUtils;
import org.qubership.atp.itf.lite.backend.utils.UrlParsingUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CollectionsService {

    private final FolderService folderService;
    private final RequestService requestService;
    private final RequestAuthorizationService requestAuthorizationService;
    private final EnvironmentFeignService environmentService;
    private final RamService ramService;
    private final CatalogueService catalogueService;
    private final MetricService metricService;
    private final DynamicVariablesService dynamicVariablesService;
    private final CookieService cookieService;

    private static final List<HttpMethod> availableHTTPMethods = Arrays.asList(HttpMethod.values());
    private static final String REQUEST_WITHOUT_FILE_MESSAGE_FORMAT = "Request was imported without a file %s";

    /**
     * Import postman collection specified in multipart file.
     *
     * @param file    zip or json file with postman collection
     * @param request import postman collection request
     * @return list of non-imported requests
     */
    @Transactional
    public List<ImportCollectionsResponse> importCollections(MultipartFile file, ImportCollectionsRequest request) {
        final UUID projectId = request.getProjectId();
        final UUID updatedId = request.getTargetFolderId();
        final String collectionName = request.getCollectionName();
        log.info("Import collection: [projectId: {}, collectionName: {}, targetFolderId: {}]",
                projectId, collectionName, updatedId);

        List<String> collections = new ArrayList<>();
        try (InputStream inputStream = file.getInputStream()) {
            String extension = FilenameUtils.getExtension(file.getOriginalFilename());
            if (extension == null) {
                log.error("The extension of the imported file is null. ImportCollectionRequest: [projectId:{}, "
                        + "collectionName:{}, targetFolderId:{}]", projectId, collectionName, updatedId);
                throw new ItfLiteImportCollectionFileExtensionNotFoundException();
            }
            if (Constants.ZIP_EXTENSION.equals(extension)) {
                collections.addAll(readFilesFromZip(inputStream));
            } else {
                collections.add(IOUtils.toString(inputStream));
            }
        } catch (IOException e) {
            log.error("Error while processing file. ImportCollectionsRequest: [projectId:{}, collectionName:{}]",
                    projectId, collectionName, e);
            throw new ItfLiteImportCollectionFileProcessException();
        }
        List<ImportCollectionsResponse> results;
        try {
            results = parseCollections(request, collections);
        } catch (AtpException ex) {
            log.error("Error while parsing file. ImportCollectionsRequest: [projectId:{}, collectionName:{}]",
                    projectId, collectionName, ex);
            throw ex;
        } catch (Exception e) {
            log.error("Error while parsing file. ImportCollectionsRequest: [projectId:{}, collectionName:{}]",
                    projectId, collectionName, e);
            throw new ItfLiteImportCollectionFileParseProcessException();
        }
        // results contain info about not imported requests
        return results;
    }

    private List<String> readFilesFromZip(InputStream inputStream) throws IOException {
        List<String> collections = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(inputStream)) {
            for (ZipEntry entry; (entry = zis.getNextEntry()) != null; ) {
                if (!entry.isDirectory()) {
                    String extension = FilenameUtils.getExtension(entry.getName());
                    if (Constants.JSON_EXTENSION.equals(extension)) {
                        collections.add(IOUtils.toString(zis));
                    }
                }
            }
        }
        return collections;
    }

    private List<ImportCollectionsResponse> parseCollections(ImportCollectionsRequest request,
                                                             List<String> collections) throws Exception {
        if (collections.isEmpty()) {
            return Collections.emptyList();
        }
        //folder for all collections
        Folder collectionsFolder = folderService.createFolder(new FolderUpsetRequest(
                request.getCollectionName(),
                request.getProjectId(),
                request.getTargetFolderId(),
                null,
                false, false, false, false, false,
                "",
                null,
                new Date()
        ));
        List<ImportCollectionsResponse> result = new ArrayList<>();
        String collectionName = request.getCollectionName();

        for (String collection : collections) {
            JsonObject jsonObject = JsonParser.parseString(collection).getAsJsonObject();
            // get folders and requests array
            JsonArray items = jsonObject.getAsJsonArray(Constants.ITEM);
            // create folder for current collection
            Folder collectionFolder = collectionsFolder;

            // If there are multiple collections, create a folder for each collection
            if (collections.size() > 1) {
                // get collection name
                collectionName = jsonObject
                        .getAsJsonObject(Constants.INFO)
                        .asMap().get(Constants.NAME).toString();
                String collectionDescription = jsonObject.getAsJsonObject(Constants.INFO)
                        .asMap().get(Constants.DESCRIPTION).toString();

                RequestAuthorization authorization = parseAuthorization(collectionsFolder, jsonObject);
                AuthorizationSaveRequest authorizationSaveRequest = null;
                if (nonNull(authorization)) {
                    authorizationSaveRequest = AuthorizationUtils.castToAuthorizationSaveRequest(authorization);
                }

                collectionFolder = folderService.createFolder(new FolderUpsetRequest(
                        collectionName,
                        request.getProjectId(),
                        collectionsFolder.getId(),
                        null,
                        false, false, false, false, false,
                        collectionDescription, authorizationSaveRequest,
                        new Date()
                ));
            } else {
                RequestAuthorization authorization = parseAuthorization(null, jsonObject);
                collectionFolder.setAuthorization(authorization);
                folderService.save(collectionFolder);
            }
            result.addAll(parseCollection(collectionName, collectionFolder, items));
        }
        return result;
    }

    private List<ImportCollectionsResponse> parseCollection(String collectionName,
                                                            Folder parentFolder, JsonArray items) throws Exception {
        List<ImportCollectionsResponse> result = new ArrayList<>();
        for (Object obj : items) {
            JsonObject item = (JsonObject) obj;
            if (item.has(Constants.ITEM)) {
                result.addAll(parseFolder(collectionName, parentFolder, item));
            } else if (item.has(Constants.REQUEST)) {
                List<ImportCollectionsResponse> response = parseRequest(collectionName, parentFolder, item);
                if (response != null) {
                    result.addAll(response);
                }
            }
        }
        return result;
    }

    private List<ImportCollectionsResponse> parseFolder(String collectionName, Folder parentFolder,
                                                        JsonObject folder) throws Exception {
        String folderName = folder.getAsJsonPrimitive(Constants.NAME).getAsString();
        String description = folder.getAsJsonPrimitive(Constants.DESCRIPTION) != null
                ? folder.getAsJsonPrimitive(Constants.DESCRIPTION).getAsString() : "";
        RequestAuthorization authorization = parseAuthorization(parentFolder, folder);
        AuthorizationSaveRequest authorizationSaveRequest = null;
        if (nonNull(authorization)) {
            authorizationSaveRequest = AuthorizationUtils.castToAuthorizationSaveRequest(authorization);
        }
        Folder childFolder = folderService.createFolder(new FolderUpsetRequest(
                folderName,
                parentFolder.getProjectId(),
                parentFolder.getId(),
                //method inheritance parsing
                null,
                false,
                false,
                false,
                false,
                false,
                description,
                authorizationSaveRequest,
                new Date()
        ));
        log.info("Parsed folder: {}, projectId: {}, folderId: {}, parentFolderId: {}",
                folderName, childFolder.getProjectId(), childFolder.getId(), childFolder.getParentId());
        JsonArray childFolders = folder.getAsJsonArray(Constants.ITEM);
        return parseCollection(collectionName, childFolder, childFolders);
    }

    private List<ImportCollectionsResponse> parseRequest(String collectionName,
                                                         Folder parentFolder, JsonObject requestItem) {
        String requestName = requestItem.getAsJsonPrimitive(Constants.NAME).getAsString();
        HttpRequest newRequest = new HttpRequest();
        newRequest.setDescription(requestItem.getAsJsonPrimitive(Constants.DESCRIPTION) != null
                ? requestItem.getAsJsonPrimitive(Constants.DESCRIPTION).getAsString() : "");
        newRequest.setName(requestName);
        newRequest.setProjectId(parentFolder.getProjectId());
        newRequest.setFolderId(parentFolder.getId());
        newRequest.setTransportType(TransportType.REST);
        log.info("Parse postman request: \"{}\", projectId: {} folderId: {}, collectionName: {}",
                requestName, parentFolder.getProjectId(), parentFolder.getId(), collectionName);

        Set<String> dynamicVariables = dynamicVariablesService.getDynamicVariables(requestItem.toString());

        JsonObject jsonRequest = requestItem.getAsJsonObject(Constants.REQUEST);
        String method = jsonRequest.getAsJsonPrimitive(Constants.METHOD).getAsString();
        HttpMethod httpMethod = HttpMethod.resolve(method);
        if (!availableHTTPMethods.contains(httpMethod)) {
            log.info("Parse postman request \"{}\" failed: Unsupported method is used: {}", requestName, method);
            return Arrays.asList(new ImportCollectionsResponse(
                    requestName, null, collectionName, "Unsupported method is used: " + method,
                    ImportCollectionStatus.ERROR, null, null));
        }
        newRequest.setHttpMethod(httpMethod);
        // get headers if exists
        newRequest.setRequestHeaders(parseRequestHeaders(jsonRequest));
        // get url if exist
        if (jsonRequest.has(Constants.URL)) {
            if (jsonRequest.get(Constants.URL).isJsonObject()) {
                JsonObject urlItem = jsonRequest.getAsJsonObject(Constants.URL);
                newRequest.setUrl(parseRequestUrl(urlItem));
                newRequest.setRequestParams(parseRequestParameters(urlItem));
            } else {
                UrlParsingUtils.parseUrlAndRequestParams(newRequest, jsonRequest.get(Constants.URL).toString());
            }
        }
        if (requestItem.has(Constants.EVENT)) {
            parseEvents(newRequest, requestItem, dynamicVariables);
        }
        // get body if exist
        RequestBody requestBody = new RequestBody();
        List<ImportCollectionsResponse> result = parseRequestBody(requestBody, jsonRequest, newRequest);
        // process content type header
        processContentTypeHeader(newRequest.getRequestHeaders(), requestBody);
        // check that in response no Error messages
        if (!isEmpty(result)) {
            boolean isError = false;
            for (ImportCollectionsResponse res : result) {
                if (ImportCollectionStatus.ERROR.equals(res.getImportStatus())) {
                    log.error("Found import result with error status - break request importing");
                    isError = true;
                }
                res.setRequestName(requestName);
                res.setCollectionName(collectionName);
            }
            if (isError) {
                return result;
            }
        }
        newRequest.setBody(requestBody);
        // get auth with configured encryption if exists
        RequestAuthorization requestAuthorization = parseAuthorization(parentFolder, jsonRequest);
        newRequest.setAuthorization(requestAuthorization);
        Request resultRequest = requestService.createRequest(newRequest);
        if (!isEmpty(result)) {
            result.forEach(res -> {
                res.setRequestId(resultRequest.getId());
            });
        }
        log.info("Request {} parsed", requestName);
        return result;
    }

    private void parseEvents(HttpRequest request, JsonObject requestItem, Set<String> dynamicVariables) {
        JsonArray events = requestItem.getAsJsonArray(Constants.EVENT);
        for (Object eventObj : events) {
            JsonObject event = (JsonObject) eventObj;
            JsonObject script = event.getAsJsonObject(Constants.SCRIPT);
            StringJoiner joiner = new StringJoiner("\n");
            for (Object partOfScript : script.getAsJsonArray(Constants.EXEC)) {
                JsonPrimitive part = (JsonPrimitive) partOfScript;
                joiner.add(String.valueOf(part.getAsString()).trim());
            }
            if (Constants.PREREQUEST.equals(event.getAsJsonPrimitive(Constants.LISTEN).getAsString())) {
                request.setPreScripts(
                        dynamicVariablesService.insertDynamicVariablesIntoPreScripts(joiner.toString(),
                                dynamicVariables));
            } else if (!dynamicVariables.isEmpty()) {
                request.setPreScripts(
                        dynamicVariablesService.insertDynamicVariablesIntoPreScripts("", dynamicVariables));
            }
            if (Constants.TEST.equals(event.getAsJsonPrimitive(Constants.LISTEN).getAsString())) {
                request.setPostScripts(joiner.toString());
            }
        }
    }

    private List<RequestHeader> parseRequestHeaders(JsonObject request) {
        List<RequestHeader> headers = new ArrayList<>();
        if (request.asMap().containsKey(Constants.HEADER)) {
            JsonArray headersArray = request.getAsJsonArray(Constants.HEADER);
            for (Object header : headersArray) {
                JsonObject headerItem = (JsonObject) header;
                String key = headerItem.getAsJsonPrimitive(Constants.KEY).getAsString();
                String value = headerItem.getAsJsonPrimitive(Constants.VALUE).getAsString();
                String description = headerItem.asMap()
                        .getOrDefault(Constants.DESCRIPTION, new JsonPrimitive(""))
                        .getAsJsonPrimitive().getAsString();
                boolean isDisabled = Boolean.parseBoolean(headerItem.asMap()
                        .getOrDefault(Constants.DISABLED, new JsonPrimitive(false))
                        .getAsJsonPrimitive().getAsString());
                headers.add(new RequestHeader(key, value, description, isDisabled));
            }
        }
        return headers;
    }

    private String parseRequestUrl(JsonObject urlItem) {
        return urlItem.getAsJsonPrimitive(Constants.RAW).getAsString().split("\\?")[0];
    }

    private List<RequestParam> parseRequestParameters(JsonObject urlItem) {
        List<RequestParam> parameters = new ArrayList<>();
        // get params if exists
        if (urlItem.has(Constants.PARAM)) {
            JsonArray paramsArray = urlItem.getAsJsonArray(Constants.PARAM);
            for (Object parameter : paramsArray) {
                JsonObject paramItem = (JsonObject) parameter;
                String key = StringUtils.strip(paramItem.asMap()
                        .getOrDefault(Constants.KEY, new JsonPrimitive("")).toString(), "\"");
                String value = StringUtils.strip(paramItem.asMap()
                        .getOrDefault(Constants.VALUE, new JsonPrimitive("")).toString(), "\"");
                String description = StringUtils.strip(paramItem.asMap()
                        .getOrDefault(Constants.DESCRIPTION, new JsonPrimitive("")).toString(), "\"");
                boolean isDisabled = Boolean.parseBoolean(paramItem.asMap()
                        .getOrDefault(Constants.DISABLED, new JsonPrimitive(false)).toString());
                parameters.add(new RequestParam(key, value, description, isDisabled));
            }
        }
        return parameters;
    }

    private List<ImportCollectionsResponse> parseRequestBody(RequestBody requestBody, JsonObject request,
                                                             HttpRequest newRequest) {
        if (request.has(Constants.BODY)) {
            List<ImportCollectionsResponse> results = new ArrayList<>();
            JsonObject body = request.getAsJsonObject(Constants.BODY);
            Pair<ImportCollectionsResponse, RequestBodyMode> result = parseRequestBodyMode(body);
            if (result.getLeft() != null) {
                results.add(result.getLeft());
                if (result.getRight() == null) {
                    return Arrays.asList(result.getLeft());
                }
            }
            parseRequestBodyType(requestBody, result.getRight(), body);
            results.addAll(parseRequestBodyContent(requestBody, result.getRight(), body, newRequest));
            return results;
        }
        return null;
    }

    private Pair<ImportCollectionsResponse, RequestBodyMode> parseRequestBodyMode(JsonObject bodyItem) {
        String bodyMode = bodyItem.getAsJsonPrimitive(Constants.MODE).getAsString();
        RequestBodyMode mode = RequestBodyMode.valueOfIgnoreCase(bodyMode);
        if (mode == null) {
            log.info("Parse postman request failed: Unsupported data mode is used: {}", bodyMode);
            ImportCollectionsResponse result = new ImportCollectionsResponse();
            result.setComment("Unsupported data mode is used: " + bodyMode);
            result.setImportStatus(ImportCollectionStatus.ERROR);
            return Pair.of(result, null);
        }
        if (RequestBodyMode.FILE.equals(mode)) {
            log.info("Parse postman request failed: Data mode is FILE. The file must be uploaded separately.");
            ImportCollectionsResponse result = new ImportCollectionsResponse();
            result.setComment("Request was imported without a file.");
            result.setImportStatus(ImportCollectionStatus.WARNING);
            result.setErrorType(ImportCollectionError.BINARY_FILE_REQUIRED);
            return Pair.of(result, mode);
        }
        return Pair.of(null, mode);
    }

    private void parseRequestBodyType(RequestBody body, RequestBodyMode bodyMode, JsonObject bodyItem) {
        RequestBodyType type = RequestBodyType.JSON;
        if (RequestBodyMode.RAW.equals(bodyMode)) {
            try {
                String bodyTypeString =
                        bodyItem.getAsJsonObject(Constants.OPTIONS)
                                .getAsJsonObject(Constants.RAW)
                                .getAsJsonPrimitive(Constants.LANGUAGE).getAsString();
                type = RequestBodyType.valueOfIgnoreCase(bodyTypeString);
            } catch (Exception e) {
                log.warn("Can't get extra constraints from 'options'", e);
            }
        } else if (RequestBodyMode.FORMDATA.equals(bodyMode)) {
            type = RequestBodyType.FORM_DATA;
        } else if (RequestBodyMode.GRAPHQL.equals(bodyMode)) {
            type = RequestBodyType.GraphQL;
        } else if (RequestBodyMode.FILE.equals(bodyMode)) {
            type = RequestBodyType.Binary;
        }
        body.setType(type);
    }

    private List<ImportCollectionsResponse> parseRequestBodyContent(RequestBody body,
                                                                    RequestBodyMode bodyMode,
                                                                    JsonObject bodyItem,
                                                                    HttpRequest newRequest) {
        switch (bodyMode) {
            case URLENCODED:
                body.setContent(parseRequestBodyUrlEncodedParams(
                        bodyItem.getAsJsonArray(Constants.URLENCODED)));
                addUrlEncodedHeader(newRequest);
                break;
            case RAW:
                body.setContent(bodyItem.getAsJsonPrimitive(Constants.RAW).getAsString());
                break;
            case GRAPHQL:
                JsonObject graphQlObject = bodyItem.getAsJsonObject(Constants.GRAPHQL);
                body.setQuery(graphQlObject.getAsJsonPrimitive(Constants.QUERY).getAsString());
                body.setVariables(graphQlObject.getAsJsonPrimitive(Constants.VARIABLES).getAsString());
                body.setContent(body.computeAndGetContent());
                break;
            case FORMDATA:
                Pair<List<FormDataPart>, List<ImportCollectionsResponse>> result =
                        parseRequestBodyFormData(bodyItem.getAsJsonArray(Constants.FORMDATA));
                body.setType(RequestBodyType.FORM_DATA);
                body.setFormDataBody(result.getLeft());
                return result.getRight();
            default:
                // null and unsupported modes are filtered before, in parseRequestBodyMode method.
        }
        return new ArrayList<>();
    }

    private void addUrlEncodedHeader(HttpRequest httpRequest) {
        if (httpRequest == null) {
            return;
        }
        if (httpRequest.getRequestHeaders() == null) {
            httpRequest.setRequestHeaders(new ArrayList<>());
        }
        if (httpRequest.getRequestHeaders().stream()
                .noneMatch(header -> Constants.CONTENT_TYPE_HEADER_NAME.equalsIgnoreCase(header.getKey()))) {
            httpRequest.getRequestHeaders().add(generateUrlEncodedHeader());
        }
    }

    private RequestHeader generateUrlEncodedHeader() {
        RequestHeader urlEncodedHeader = new RequestHeader();
        urlEncodedHeader.setDescription(Constants.EMPTY_STRING);
        urlEncodedHeader.setDisabled(false);
        urlEncodedHeader.setKey(Constants.CONTENT_TYPE_HEADER_NAME);
        urlEncodedHeader.setValue(Constants.URL_ENCODED_HEADER_VALUE);
        return urlEncodedHeader;
    }

    private String parseRequestBodyUrlEncodedParams(JsonArray values) {
        StringJoiner content = new StringJoiner("&");
        for (Object keyValue : values) {
            JsonObject kv = (JsonObject) keyValue;
            if (kv.has(Constants.KEY)) {
                String key = kv.getAsJsonPrimitive(Constants.KEY).getAsString();
                String value = "";
                if (kv.has(Constants.VALUE)) {
                    value = kv.getAsJsonPrimitive(Constants.VALUE).getAsString();
                }
                content.add(key + "=" + value);
            }
        }
        return content.toString();
    }

    private Pair<List<FormDataPart>, List<ImportCollectionsResponse>> parseRequestBodyFormData(JsonArray values) {
        List<FormDataPart> formDataParts = new ArrayList<>();
        List<ImportCollectionsResponse> formDataParseResult = new ArrayList<>();
        for (Object fdp : values) {
            JsonObject fdpJson = (JsonObject) fdp;
            FormDataPart newFdp = new FormDataPart();
            newFdp.setId(UUID.randomUUID());
            JsonPrimitive keyPrimitive = fdpJson.getAsJsonPrimitive(Constants.KEY);
            if (nonNull(keyPrimitive)) {
                newFdp.setKey(keyPrimitive.getAsString());
            }
            ValueType vt = ValueType.valueOf(fdpJson.getAsJsonPrimitive(Constants.TYPE).getAsString().toUpperCase());
            newFdp.setType(vt);
            JsonPrimitive contentTypePrimitive = fdpJson.getAsJsonPrimitive(Constants.CONTENT_TYPE);
            if (nonNull(contentTypePrimitive)) {
                newFdp.setContentType(contentTypePrimitive.getAsString());
            }
            JsonPrimitive descriptionPrimitive = fdpJson.getAsJsonPrimitive(Constants.DESCRIPTION);
            if (nonNull(descriptionPrimitive)) {
                newFdp.setDescription(descriptionPrimitive.getAsString());
            }
            JsonPrimitive disabledPrimitive = fdpJson.getAsJsonPrimitive(Constants.DISABLED);
            if (nonNull(disabledPrimitive)) {
                newFdp.setDisabled(disabledPrimitive.getAsBoolean());
            }
            if (ValueType.TEXT.equals(vt) || ValueType.DEFAULT.equals(vt)) {
                JsonPrimitive valuePrimitive = fdpJson.getAsJsonPrimitive(Constants.VALUE);
                if (nonNull(valuePrimitive)) {
                    newFdp.setValue(valuePrimitive.getAsString());
                }
            } else {
                JsonPrimitive scrPrimitive = null;
                JsonElement srcElement = fdpJson.get(Constants.SRC);
                if (nonNull(srcElement)) {
                    if (srcElement.isJsonArray()) {
                        JsonArray srcArray = srcElement.getAsJsonArray();
                        if (!srcArray.isEmpty()) {
                            scrPrimitive = srcArray.get(0).getAsJsonPrimitive();
                        }
                    } else {
                        scrPrimitive = fdpJson.getAsJsonPrimitive(Constants.SRC);
                    }
                }
                String fileName = "";

                if (nonNull(scrPrimitive)) {
                    fileName = FilenameUtils.getName(scrPrimitive.getAsString());
                }
                newFdp.setValue(fileName);
                ImportCollectionsResponse result = new ImportCollectionsResponse();
                result.setComment(String.format(REQUEST_WITHOUT_FILE_MESSAGE_FORMAT, fileName));
                result.setImportStatus(ImportCollectionStatus.WARNING);
                result.setErrorType(ImportCollectionError.FORMDATA_FILE_REQUIRED);
                result.setFormDataPartId(newFdp.getId());
                formDataParseResult.add(result);
            }
            formDataParts.add(newFdp);
        }
        return Pair.of(formDataParts, formDataParseResult);
    }

    private RequestAuthorization parseAuthorization(Folder parentFolder, JsonObject request) {
        if (request.has(Constants.AUTH)) {
            JsonObject auth = request.getAsJsonObject(Constants.AUTH);
            if (auth.has(Constants.TYPE)) {
                Optional<RequestAuthorizationType> optionalRequestAuthorizationType =
                        Arrays.stream(RequestAuthorizationType.values())
                                .filter(authType -> authType.getName()
                                        .equals(auth.getAsJsonPrimitive(Constants.TYPE).getAsString().toUpperCase()))
                                .findFirst();
                if (!optionalRequestAuthorizationType.isPresent()
                        || !auth.has(auth.getAsJsonPrimitive(Constants.TYPE).getAsString())) {
                    return null;
                }

                JsonElement authByType = auth.get(auth.getAsJsonPrimitive(Constants.TYPE).getAsString());
                if (nonNull(authByType)) {
                    Map<String, String> authorizationParametersMap = getAuthParametersAsMap(authByType);

                    return requestAuthorizationService.parseAuthorizationFromMap(
                            authorizationParametersMap, optionalRequestAuthorizationType.get());
                }
            }
        } else {
            // inherit from parent is default auth type in postman
            InheritFromParentRequestAuthorization requestAuth = new InheritFromParentRequestAuthorization();
            requestAuth.setType(RequestAuthorizationType.INHERIT_FROM_PARENT);
            if (nonNull(parentFolder)) {
                requestAuth.setAuthorizationFolderId(parentFolder.getId());
            }
            return requestAuth;
        }
        return null;
    }

    private Map<String, String> getAuthParametersAsMap(JsonElement auth) {
        Map<String, String> authorizationParametersMap = new HashMap<>();
        if (auth.isJsonObject()) {
            Map<String, JsonElement> kvMap = auth.getAsJsonObject().asMap();
            if (!isEmpty(kvMap)) {
                for (Map.Entry<String, JsonElement> keyValue : kvMap.entrySet()) {
                    String key = keyValue.getKey();
                    JsonPrimitive value = keyValue.getValue().getAsJsonPrimitive();
                    if (nonNull(key) && nonNull(value)) {
                        authorizationParametersMap.put(key, value.getAsString());
                    }
                }
            }
        } else if (auth.isJsonArray()) {
            for (Object keyValue : auth.getAsJsonArray()) {
                JsonObject kv = (JsonObject) keyValue;
                JsonPrimitive key = kv.getAsJsonPrimitive(Constants.KEY);
                JsonPrimitive value = kv.getAsJsonPrimitive(Constants.VALUE);
                if (nonNull(key) && nonNull(value)) {
                    authorizationParametersMap.put(key.getAsString(), value.getAsString());
                }
            }
        }
        return authorizationParametersMap;
    }

    /**
     * Execute collection.
     *
     * @param authToken authorization token
     * @param request   execute request
     * @return execution request id
     */
    public List<UUID> executeCollection(String authToken, CollectionExecuteRequest request) {
        final UUID projectId = request.getProjectId();
        final UUID defaultTestPlanId = ramService.getDefaultCollectionRunTestPlanId(projectId);

        final EnrichedScenarioDto testScenario = generateIftLiteRunCollectionScenario(request);

        ExecuteRequestDto executeRequestDto = new ExecuteRequestDto()
                .name(request.getName())
                .environmentIds(request.getEnvironmentIds())
                .emailRecipients(request.getEmailRecipients())
                .emailTemplateId(request.getEmailTemplateId())
                .emailSubject(request.getEmailSubject())
                .taToolIds(request.getTaToolIds())
                .flags(request.getFlags()
                        .stream()
                        .map(flag -> ExecuteRequestDto.FlagsEnum.valueOf(flag.name()))
                        .collect(Collectors.toList()))
                .logCollectorTemplateId(request.getLogCollectorTemplateId())
                .projectId(request.getProjectId())
                .testPlanId(defaultTestPlanId)
                .threadCount(1)
                .isMandatoryCheck(request.isMandatoryCheck())
                .isSsmCheck(request.isSsmCheck())
                .isIgnoreFailedChecks(request.isIgnoreFailedChecks())
                .testScenarios(singletonList(testScenario))
                .dataSetStorageId(request.getDataSetStorageId())
                .datasetId(request.getDataSetId())
                .contextVariables(request.convertContextVariablesToMap());
        List<UUID> listExecutedId = catalogueService.execute(authToken, executeRequestDto);

        if (request.isPropagateCookies() && !isEmpty(listExecutedId)) {
            log.info("Propagating cookies in collection execution enabled. ErId: {}", listExecutedId);
            copyCookiesForCollectionExecution(projectId, listExecutedId.get(0));
        }

        metricService.registerCountRunCollections(projectId);
        log.debug("List collection executed id: {}", listExecutedId);
        return listExecutedId;
    }

    /**
     * Copies user cookies for using in collection execution.
     *
     * @param projectId project id
     * @param erId      execution request id
     */
    private void copyCookiesForCollectionExecution(UUID projectId, UUID erId) {
        List<Cookie> cookies = cookieService.getNotExpiredCookiesByUserIdAndProjectId(projectId);
        // Clearing userId field to avoid duplicating cookies for user
        cookies.forEach(cookie -> cookie.setUserId(null));
        cookieService.fillCookieInfoWithExecutionRequestInfo(cookies, erId, null);
        cookieService.save(cookies);
    }

    private EnrichedScenarioDto generateIftLiteRunCollectionScenario(CollectionExecuteRequest request) {
        List<EnrichedCompoundDto> compounds = generateCompounds(request);

        EnrichedScenarioDto testScenario = new EnrichedScenarioDto();
        testScenario.setTestScenarioId(UUID.randomUUID());
        testScenario.setTestScenarioName(request.getName());
        testScenario.setCompounds(compounds);

        return testScenario;
    }

    private List<EnrichedCompoundDto> generateCompounds(CollectionExecuteRequest request) {
        final UUID environmentId = StreamUtils.getFirstElem(request.getEnvironmentIds());
        final DirectiveDto directive = getExecutionDirective(environmentId);
        final List<GroupResponse> treeNodes = request.getTreeNodes();

        return treeNodes.stream()
                .map(node -> mapItfLiteTreeNodeToCompound(node, directive, request.getProjectId()))
                .collect(Collectors.toList());
    }

    private EnrichedCompoundDto mapItfLiteTreeNodeToCompound(GroupResponse node, DirectiveDto directive,
                                                             UUID projectId) {
        EnrichedCompoundDto enrichedCompound = new EnrichedCompoundDto();
        enrichedCompound.setDirectives(singletonList(directive));

        final EntityType type = node.getType();
        if (type.equals(EntityType.FOLDER)) {
            enrichedCompound.setId(UUID.randomUUID());
            enrichedCompound.setContent(node.getName());
            enrichedCompound.setType(TypeEnum.valueOf(AbstractTestScenario.Type.COMPOUND.name()));

            final List<GroupResponse> children = node.getChildren();
            if (!isEmpty(children)) {
                List<EnrichedCompoundDto> childrenEnrichedCompounds = children.stream()
                        .map(childNode -> mapItfLiteTreeNodeToCompound(childNode, directive, projectId))
                        .collect(Collectors.toList());
                enrichedCompound.setChildCompounds(childrenEnrichedCompounds);
            }
        } else {
            final String requestId = node.getId().toString();
            final String actionName = "Execute request \"uuid\"";
            enrichedCompound.setContent(actionName);
            enrichedCompound.setEntity(new ActionEntityDto()
                    .name(actionName)
                    .parameters(singletonList(
                            new ActionParameterDto()
                                    .name("uuid")
                                    .value(requestId))
                    ));
            metricService.registerCountCollectionRequests(projectId);
        }

        return enrichedCompound;
    }

    private DirectiveDto getExecutionDirective(UUID environmentId) {
        System firstEnvironmentSystem = getFirstEnvironmentSystem(environmentId);

        DirectiveDto directive = new DirectiveDto();
        directive.setName(DirectiveEnum.USE.getName());
        directive.putParametersItem(Directive.DirectiveParameter.VALUE.name(), firstEnvironmentSystem.getName());

        return directive;
    }

    private System getFirstEnvironmentSystem(UUID environmentId) {
        List<System> environmentSystems = environmentService.getEnvironmentSystems(environmentId);

        return StreamUtils.getFirstElem(environmentSystems);
    }

    /**
     * Process Content-Type headers and choose which one we should use.
     * User header(generated=false) has a highest priority and should be used instead of auto generated(generated=true).
     */
    void processContentTypeHeader(List<RequestHeader> requestHeaders, RequestBody requestBody) {
        List<RequestHeader> contentTypeHeaders = requestHeaders.stream()
                .filter(header -> HttpHeaders.CONTENT_TYPE.equals(header.getKey()))
                .collect(Collectors.toList());

        if (isEmpty(contentTypeHeaders)) {
            RequestBodyType bodyType = requestBody.getType();
            if (nonNull(bodyType)) {
                String bodyContentTypeValue = null;
                if (nonNull(bodyType.getContentTypes())) {
                    // get first content type from list of content types if not null
                    bodyContentTypeValue = bodyType.getContentTypes().get(0);
                }
                RequestHeader contentTypeHeader =
                        new RequestHeader(HttpHeaders.CONTENT_TYPE, bodyContentTypeValue, "", false, true);
                requestHeaders.add(contentTypeHeader);
            }
        } else {
            boolean isEnabledUserContentTypeHeaderPresent = contentTypeHeaders.stream()
                    .anyMatch(header -> !header.isDisabled() && !header.isGenerated());

            if (isEnabledUserContentTypeHeaderPresent) {
                contentTypeHeaders.stream()
                        .filter(RequestHeader::isGenerated)
                        .forEach(header -> header.setDisabled(true));
            }
        }
    }
}
