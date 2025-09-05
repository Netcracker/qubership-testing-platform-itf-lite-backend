package org.qubership.atp.itf.lite.backend.mocks;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.assertj.core.util.DateUtil;
import org.qubership.atp.itf.lite.backend.feign.dto.CertificateDto;
import org.qubership.atp.itf.lite.backend.enums.DatasetFormat;
import org.qubership.atp.itf.lite.backend.enums.ProjectEventType;
import org.qubership.atp.itf.lite.backend.enums.RequestExportStatus;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.enums.ValueType;
import org.qubership.atp.itf.lite.backend.enums.auth.OAuth2GrantType;
import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.qubership.atp.itf.lite.backend.enums.http.RequestBodyType;
import org.qubership.atp.itf.lite.backend.feign.dto.FileInfoDto;
import org.qubership.atp.itf.lite.backend.feign.dto.GetAccessCodeParametersDto;
import org.qubership.atp.itf.lite.backend.model.api.kafka.ItfExportResponseEvent;
import org.qubership.atp.itf.lite.backend.model.api.kafka.ItfLiteExecutionFinishEvent;
import org.qubership.atp.itf.lite.backend.model.api.kafka.MiaExportResponseEvent;
import org.qubership.atp.itf.lite.backend.model.api.kafka.ProjectEvent;
import org.qubership.atp.itf.lite.backend.model.api.request.ExecutionCollectionRequestExecuteRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderUpsetRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.HttpRequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.IdWithModifiedWhen;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntitiesBulkDelete;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntitiesCopyRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntitiesMoveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntityCopyRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntityCreateRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntityEditRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntityMoveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestItfExportRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestMiaExportRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.InheritFromParentAuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.OAuth2AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpHeaderSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpParamSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExecutionHeaderResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExecutionResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExportResultResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.environments.Connection;
import org.qubership.atp.itf.lite.backend.model.api.response.environments.System;
import org.qubership.atp.itf.lite.backend.model.entities.Cookie;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.model.entities.RequestBody;
import org.qubership.atp.itf.lite.backend.model.entities.RequestExportEntity;
import org.qubership.atp.itf.lite.backend.model.entities.auth.BasicRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.BearerRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.GetAuthorizationCode;
import org.qubership.atp.itf.lite.backend.model.entities.auth.InheritFromParentRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.OAuth2RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.collection.run.CollectionRunRequest;
import org.qubership.atp.itf.lite.backend.model.entities.collection.run.CollectionRunStackRequest;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileData;
import org.qubership.atp.itf.lite.backend.model.entities.history.RequestExecution;
import org.qubership.atp.itf.lite.backend.model.entities.http.FormDataPart;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestParam;
import org.qubership.atp.itf.lite.backend.model.entities.http.methods.HttpMethod;
import org.qubership.atp.itf.lite.backend.model.entities.user.UserSettings;
import org.qubership.atp.itf.lite.backend.service.MetricService;
import org.qubership.atp.itf.lite.backend.utils.AuthorizationUtils;
import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.qubership.atp.itf.lite.backend.utils.RequestUtils;

import org.qubership.atp.adapter.common.context.AtpCompaund;
import org.qubership.atp.itf.lite.backend.catalog.models.ActionEntity;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

public class EntitiesGenerator {

    public static Folder generateFolder(String name, UUID projectId) {
        return generateFolder(name, projectId, null);
    }

    public static Folder generateFolder(String name, UUID projectId, UUID parentId) {
        return generateFolder(name, projectId, parentId, 0);
    }

    public static Folder generateFolder(String name, UUID projectId, UUID parentId, int order) {
        return generateFolder(UUID.randomUUID(), name, projectId, parentId, order, "");
    }

    public static Folder generateFolder(UUID id, String name, UUID projectId, UUID parentId, int order, String doc) {
        Folder folder = new Folder();

        folder.setId(id);
        folder.setName(name);
        folder.setProjectId(projectId);
        folder.setParentId(parentId);
        folder.setOrder(order);
        folder.setDescription(doc);

        return folder;
    }

    public static HttpRequest generateHttpRequest(String name, UUID projectId) {
        return generateHttpRequest(UUID.randomUUID(), name, projectId, null, null, 0, "");
    }

    public static HttpRequest generateHttpRequest(String name, UUID projectId, UUID folderId) {
        return generateHttpRequest(UUID.randomUUID(), name, projectId, folderId, null, 0, "");
    }

    public static HttpRequest generateHttpRequest(String name, UUID projectId, int order) {
        return generateHttpRequest(UUID.randomUUID(), name, projectId, null, null, order, "");
    }

    public static HttpRequest generateHttpRequest(String name, UUID projectId, UUID folderId, Integer order) {
        return generateHttpRequest(UUID.randomUUID(), name, projectId, folderId, null, order, "");
    }

    public static HttpRequest generateHttpRequest(String name, UUID projectId, UUID folderId, TransportType type, Integer order) {
        return generateHttpRequest(UUID.randomUUID(), name, projectId, folderId, type, order, "");
    }

    public static HttpRequest generateHttpRequest(UUID id, String name, UUID projectId, UUID folderId,
                                                   TransportType type, Integer order, String doc) {
        HttpRequest request = new HttpRequest();

        request.setId(id);
        request.setName(name);
        request.setProjectId(projectId);
        request.setFolderId(folderId);
        request.setTransportType(type);
        request.setOrder(order);
        request.setDescription(doc);

        return request;
    }

    public static HttpRequest generateRandomHttpRequest() {
        HttpRequest request = new HttpRequest();

        request.setId(UUID.randomUUID());
        request.setName("Request");
        request.setProjectId(UUID.randomUUID());
        request.setTransportType(TransportType.REST);
        request.setFolderId(UUID.randomUUID());
        RequestHeader requestHeader1 = new RequestHeader(UUID.randomUUID(), "Content-Type", "application/json", "json type", false);
        requestHeader1.setCreatedWhen(DateUtil.now());
        requestHeader1.setModifiedWhen(DateUtil.now());
        RequestHeader requestHeader2 = new RequestHeader(UUID.randomUUID(), "My-Header", "my-value", "", false);
        requestHeader2.setCreatedWhen(DateUtil.now());
        requestHeader2.setModifiedWhen(DateUtil.now());
        RequestHeader requestHeader3 = new RequestHeader(UUID.randomUUID(), "My-Header", "my-value", "", false);
        requestHeader3.setCreatedWhen(DateUtil.now());
        requestHeader3.setModifiedWhen(DateUtil.now());
        request.setRequestHeaders(asList(requestHeader1, requestHeader2, requestHeader3));
        RequestParam requestParam1 = new RequestParam(UUID.randomUUID(), "name1", "name1", "name1", false);
        requestParam1.setCreatedWhen(DateUtil.now());
        requestParam1.setModifiedWhen(DateUtil.now());
        RequestParam requestParam2 = new RequestParam(UUID.randomUUID(), "name2", "name2", "name2", false);
        requestParam2.setCreatedWhen(DateUtil.now());
        requestParam2.setModifiedWhen(DateUtil.now());
        request.setRequestParams(asList(requestParam1, requestParam2));
        request.setHttpMethod(HttpMethod.POST);
        request.setBody(new RequestBody("{\"id\": \"123\"}", RequestBodyType.JSON));
        request.setUrl("http://test.test");
        request.setDisableSslCertificateVerification(false);
        request.setDisableSslClientCertificate(false);
        request.setDisableFollowingRedirect(false);

        return request;
    }

    public static HttpRequest generateRandomHttpRequestWithFormData() {
        HttpRequest httpRequest = generateRandomHttpRequest();
        httpRequest.setBody(new RequestBody(generateFormDataBody(), RequestBodyType.FORM_DATA));
        return httpRequest;
    }

    public static HttpRequestEntitySaveRequest generateRandomHttpRequestEntitySaveRequest() {
        HttpRequestEntitySaveRequest request = new HttpRequestEntitySaveRequest();

        request.setId(UUID.randomUUID());
        request.setName("Request");
        request.setProjectId(UUID.randomUUID());
        request.setTransportType(TransportType.REST);
        request.setFolderId(UUID.randomUUID());
        request.setRequestHeaders(new ArrayList<>(asList(
                new HttpHeaderSaveRequest("Content-Type", "application/json", "json type"))));
        request.setRequestParams(new ArrayList<>(asList(
                new HttpParamSaveRequest("name", "name", "name"))));
        request.setHttpMethod(HttpMethod.POST);
        request.setBody(new RequestBody("{\"id\": \"123\"}", RequestBodyType.JSON));
        request.setUrl("http://test.test");

        Cookie cookie1 = new Cookie();
        cookie1.setKey("Cookie_1");
        cookie1.setValue("Cookie_1=value; Path=/;");
        cookie1.setDomain("domain1");
        request.setCookies(Collections.singletonList(cookie1));

        return request;
    }

    public static HttpRequestEntitySaveRequest generateRandomHttpRequestEntitySaveRequestWithFormData() {
        HttpRequestEntitySaveRequest request = generateRandomHttpRequestEntitySaveRequest();
        request.setId(UUID.randomUUID());
        request.setName("Request");
        request.setProjectId(UUID.randomUUID());
        request.setTransportType(TransportType.REST);
        request.setFolderId(UUID.randomUUID());
        request.setRequestHeaders(new ArrayList<>(asList(
                new HttpHeaderSaveRequest("Content-Type", "multipart/formdata", null, false, true))));
        request.setRequestParams(new ArrayList<>(asList(
                new HttpParamSaveRequest("name", "name", "name"))));
        request.setHttpMethod(HttpMethod.POST);
        request.setBody(new RequestBody(generateFormDataBody(), RequestBodyType.FORM_DATA));
        request.setUrl("http://test.test");
        return request;
    }

    public static HttpRequestEntitySaveRequest generateRandomHttpRequestEntitySaveRequestWithFileData() {
        HttpRequestEntitySaveRequest request = new HttpRequestEntitySaveRequest();

        request.setId(UUID.randomUUID());
        request.setName("Request");
        request.setProjectId(UUID.randomUUID());
        request.setTransportType(TransportType.REST);
        request.setFolderId(UUID.randomUUID());
        request.setRequestHeaders(new ArrayList<>(asList(
                new HttpHeaderSaveRequest("Content-Type", "application/json", "json type"))));
        request.setRequestParams(new ArrayList<>(asList(
                new HttpParamSaveRequest("name", "name", "name"))));
        request.setHttpMethod(HttpMethod.POST);
        request.setBody(new RequestBody((String) null, RequestBodyType.Binary));
        request.setFile(new FileData("string".getBytes(), "name"));
        request.setUrl("http://test.test");

        return request;
    }

    public static List<FormDataPart> generateFormDataBody() {
        List<FormDataPart> result = new ArrayList<>();
        result.add(new FormDataPart("key1", ValueType.TEXT,
                "some text", null, null, "some description", false));
        result.add(new FormDataPart("key2", ValueType.FILE,
                "fileName.zip", UUID.randomUUID(), "application/zip", "some description", false));
        return result;
    }

    public static RequestExecutionResponse generateRequestExecutionResponse() {
        RequestExecutionResponse response = new RequestExecutionResponse();
        response.setId(UUID.randomUUID());
        response.setStatusCode("200");
        response.setStatusText("OK");
        response.setExecutedWhen(new Date());
        response.setDuration(new BigInteger("1225"));
        response.setResponseHeaders(asList(
                new RequestExecutionHeaderResponse("key1", "value1"),
                new RequestExecutionHeaderResponse("key1", "value2"),
                new RequestExecutionHeaderResponse("key2", "value")));
        String body = "{\"key\": \"value\"}";
        response.setBody(body);
        response.setAuthorizationToken("token");
        return response;
    }

    public static RequestExecutionResponse generateRequestExecutionResponseWithError() {
        return RequestExecutionResponse.builder()
                .id(UUID.randomUUID())
                .statusCode("500")
                .statusText("ERROR")
                .executedWhen(new Date())
                .duration(new BigInteger("1225"))
                .responseHeaders(asList(
                        new RequestExecutionHeaderResponse("key1", "value1"),
                        new RequestExecutionHeaderResponse("key1", "value2"),
                        new RequestExecutionHeaderResponse("key2", "value")))
                .authorizationToken("token")
                .error(RequestUtils.getErrorResponse(new Exception("Exception")))
                .build();
    }

    public static Optional<GetAuthorizationCode> generateGetAuthorizationCode(UUID sseId, String token) {
        GetAuthorizationCode getAuthorizationCode = new GetAuthorizationCode();
        getAuthorizationCode.setProjectId(UUID.randomUUID());
        getAuthorizationCode.setSseId(sseId);
        getAuthorizationCode.setAccessTokenUrl("URL");
        getAuthorizationCode.setState("state");
        getAuthorizationCode.setResponseState("state");
        getAuthorizationCode.setRedirectUri("redirectUri");
        getAuthorizationCode.setToken(token);
        return Optional.of(getAuthorizationCode);
    }

    public static GetAccessCodeParametersDto generateGetAccessCodeParametersDto(UUID sseId) {
        return new GetAccessCodeParametersDto()
                .projectId(UUID.randomUUID())
                .sseId(sseId)
                .accessTokenUrl("URL")
                .clientId("client ID")
                .clientSecret("client Secret encrypted")
                .scope("scope")
                .state("state")
                .redirectUri("redirectUri");
    }

    public static GetAccessCodeParametersDto generateEmptyGetAccessCodeParametersDto() {
        return new GetAccessCodeParametersDto().projectId(UUID.randomUUID());
    }

    public static RequestExecutionResponse generateRequestExecutionResponseWithHeaders(
            UUID requestId,
            List<RequestExecutionHeaderResponse> responseHeaders) {
        RequestExecutionResponse response = generateRequestExecutionResponse();
        response.setId(requestId);
        response.setResponseHeaders(responseHeaders);
        return response;
    }

    public static RequestExecution generateHttpRequestExecution(UUID projectId) {
        RequestExecution requestExecutionHttp = new RequestExecution();
        requestExecutionHttp.setId(UUID.randomUUID());
        requestExecutionHttp.setUrl("https://api.coindesk.com/v1/bpi/currentprice.json");
        requestExecutionHttp.setName("http_test");
        requestExecutionHttp.setProjectId(projectId);
        requestExecutionHttp.setTransportType(TransportType.REST);
        requestExecutionHttp.setExecutedWhen(new Date());
        requestExecutionHttp.setExecutor("Admin Adminovich");
        requestExecutionHttp.setStatusCode("5012");
        requestExecutionHttp.setStatusText("CER Recieved from Different Connection");
        requestExecutionHttp.setDuration(new BigInteger("11118"));
        return requestExecutionHttp;
    }

    public static UserSettings generateUserSettings() {
        UserSettings settings = new UserSettings();
        settings.setId(UUID.randomUUID());
        settings.setUserId(UUID.randomUUID());
        settings.setCreatedWhen(new Date());
        settings.setVisibleColumns(asList("Column_1", "Column_2"));
        return settings;
    }

    public static String getToken() {
        return "Bearer eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ4clRDZjR0Z0piUnVPX1lxRWdrVW03c1pvSzlvUER0"
                + "SUJyZElEMTRiNXBZIn0.eyJqdGkiOiI3OWFjMTVmYS1lODQ4LTQ2NmEtODA5ZC04YWE5OTljMmZkYmYiLCJleHAiOjE2NTk0Nz"
                + "MxNzYsIm5iZiI6MCwiaWF0IjoxNjU5NDY5NTc2LCJpc3MiOiJodHRwczovL2F0cC1rZXljbG9hay11YXQwMi5kZXYtYXRwLWNs"
                + "b3VkLm5ldGNyYWNrZXIuY29tL2F1dGgvcmVhbG1zL2F0cDIiLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiZGU0NDBmZTUtNzdjYi"
                + "00NTJjLWIwZDktMjYwODY0NGEzZTcwIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiZnJvbnRlbmQiLCJub25jZSI6IjMiLCJhdXRo"
                + "X3RpbWUiOjE2NTk0Njk1NzYsInNlc3Npb25fc3RhdGUiOiJhNzFhOTU0Ny1lMWQ4LTRjMmQtYTE5MS0yMGNkYjllOWVkZjQiLC"
                + "JhY3IiOiIxIiwiYWxsb3dlZC1vcmlnaW5zIjpbIioiXSwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbIm9mZmxpbmVfYWNjZXNz"
                + "IiwiQVRQX0FETUlOIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJhY2NvdW50Ijp7InJvbGVzIj"
                + "pbIm1hbmFnZS1hY2NvdW50IiwibWFuYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6ImVtYWls"
                + "IHByb2ZpbGUiLCJlbWFpbF92ZXJpZmllZCI6ZmFsc2UsIm5hbWUiOiJBZG1pbiBBZG1pbm92aWNoIiwicHJlZmVycmVkX3VzZX"
                + "JuYW1lIjoiYWRtaW4iLCJnaXZlbl9uYW1lIjoiQWRtaW4iLCJmYW1pbHlfbmFtZSI6IkFkbWlub3ZpY2giLCJlbWFpbCI6InRl"
                + "c3RAdGVzdCJ9.PJmyDq7Awpo-vr035n43tgtzoIpfiMxVO8gxdh5K2IjoBU1nN9x_HLRDsBcmwwn4ByVe4mVXdWU4fBf3LAdxf"
                + "P2fUrU8qM0yVKARnNukcZgdVBkpYQmbESb_RYhzjp1GuMASDS667y6MvbCfWdWBnrF5CBKyKxvO5JXF6a1p3BYntq0pJ4-FFq7"
                + "CDqH2ZcQzC3TddE8RvblkgoSDx_8s33mydwNe1K3SbOQ7omIDEpWFVfw4zur2voNMZAaTqtyNy7QIbrNRivWki8ZCx8jnhVwVS"
                + "HWSD4vtIhXYNpYaJDfRse_LD-uKp1tsFMy7AMPdNyGvEyKPFpWyeAn42J6peQ";
    }

    public static byte[] generateZipArchiveFromBytes(String filename, byte[] input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        ZipEntry entry = new ZipEntry(filename);
        entry.setSize(input.length);
        zos.putNextEntry(entry);
        zos.write(input);
        zos.closeEntry();
        zos.close();
        return baos.toByteArray();
    }

    public static HttpRequest generateHttpRequestWithName(String name) {
        HttpRequest request = generateRandomHttpRequest();
        request.setName(name);

        return request;
    }

    public static RequestEntityCreateRequest generateRequestCreateFromRequest(Request request) {
        return new RequestEntityCreateRequest(request.getName(), request.getProjectId(),
                request.getFolderId(), request.getTransportType());
    }

    public static HttpRequestEntitySaveRequest generateHttpRequestSaveFromHttpRequest(HttpRequest request) {
        List<HttpHeaderSaveRequest> headers = new ArrayList<>();
        List<HttpParamSaveRequest> parameters = new ArrayList<>();
        if (nonNull(request.getRequestHeaders())) {
            headers = request.getRequestHeaders().stream()
                    .map(header -> new HttpHeaderSaveRequest(header.getKey(), header.getValue(), header.getDescription()))
                    .collect(Collectors.toList());
        }
        if (nonNull(request.getRequestParams())) {
            parameters = request.getRequestParams().stream()
                    .map(parameter -> new HttpParamSaveRequest(parameter.getKey(), parameter.getValue(),
                            parameter.getDescription()))
                    .collect(Collectors.toList());
        }

        HttpRequestEntitySaveRequest saveRequest = new HttpRequestEntitySaveRequest(request.getHttpMethod(),
                request.getUrl(), parameters, headers, request.getBody(), null, null);
        saveRequest.setModifiedWhen(new Date());
        return (HttpRequestEntitySaveRequest) fillParentParameters(saveRequest, request);
    }

    private static RequestEntitySaveRequest fillParentParameters(RequestEntitySaveRequest saveRequest,
                                                                 Request request) {
        saveRequest.setId(request.getId());
        saveRequest.setName(request.getName());
        saveRequest.setProjectId(request.getProjectId());
        saveRequest.setFolderId(request.getFolderId());
        saveRequest.setTransportType(request.getTransportType());
        RequestAuthorization authorization = request.getAuthorization();
        if (nonNull(authorization)) {
            saveRequest.setAuthorization(AuthorizationUtils.castToAuthorizationSaveRequest(authorization));
        }
        return saveRequest;
    }

    public static RequestEntityEditRequest generateRequestEditFromRequest(Request request) {
        RequestEntityEditRequest editRequest = new RequestEntityEditRequest(request.getName(), request.getProjectId(),
                false, false, false, false, false);
        editRequest.setModifiedWhen(new Date());
        return editRequest;
    }

    public static RequestEntityCopyRequest generateRequestEntityCopyRequestFromRequest(Request request) {
        return new RequestEntityCopyRequest(request.getProjectId(), request.getFolderId());
    }

    public static RequestEntitiesCopyRequest generateRequestEntitiesCopyRequest() {
        UUID requestId1 = UUID.randomUUID();
        UUID requestId2 = UUID.randomUUID();
        HashSet<UUID> requestIds = new HashSet<>(asList(requestId1, requestId2));
        UUID projectId = UUID.randomUUID();
        UUID folderId = UUID.randomUUID();
        return new RequestEntitiesCopyRequest(projectId, requestIds, folderId);
    }

    public static RequestEntityMoveRequest generateRequestEntityMoveRequestFromRequest(Request request) {
        return new RequestEntityMoveRequest(request.getProjectId(), request.getFolderId());
    }

    public static RequestEntitiesMoveRequest generateRequestEntitiesMoveRequest() {
        UUID requestId1 = UUID.randomUUID();
        UUID requestId2 = UUID.randomUUID();
        HashSet<IdWithModifiedWhen> requestIds = new HashSet<>(asList(
                new IdWithModifiedWhen(requestId1, new Date()),
                new IdWithModifiedWhen(requestId2, new Date())));
        UUID projectId = UUID.randomUUID();
        UUID folderId = UUID.randomUUID();
        return new RequestEntitiesMoveRequest(projectId, requestIds, folderId);
    }

    public static RequestEntitiesBulkDelete generateRequestEntitiesDeleteRequest(Set<UUID> requestIds) {
        return new RequestEntitiesBulkDelete(requestIds, UUID.randomUUID());
    }

    public static RequestMiaExportRequest generateRequestMiaExportRequest() {
        RequestMiaExportRequest requestMiaExportRequest = new RequestMiaExportRequest();
        UUID projectId = UUID.randomUUID();
        requestMiaExportRequest.setProjectId(projectId);
        UUID requestId1 = UUID.randomUUID();
        UUID requestId2 = UUID.randomUUID();
        HashSet<UUID> requestIds = new HashSet<>(Arrays.asList(requestId1, requestId2));
        requestMiaExportRequest.setRequestIds(requestIds);
        String miaPath = "path/to/mia/request";
        requestMiaExportRequest.setMiaPath(miaPath);
        return requestMiaExportRequest;
    }

    public static RequestItfExportRequest generateRequestItfExportRequest() {
        RequestItfExportRequest requestItfExportRequest = new RequestItfExportRequest();
        requestItfExportRequest.setProjectId(UUID.randomUUID());
        UUID requestId1 = UUID.randomUUID();
        UUID requestId2 = UUID.randomUUID();
        HashSet<UUID> requestIds = new HashSet<>(Arrays.asList(requestId1, requestId2));
        requestItfExportRequest.setRequestIds(requestIds);
        requestItfExportRequest.setItfUrl("http://itf.url");
        requestItfExportRequest.setSystemId(BigInteger.ONE);
        requestItfExportRequest.setOperationId(BigInteger.TEN);
        Map<UUID, BigInteger> requestIdsReceiversMap = new HashMap<>();
        requestIdsReceiversMap.put(requestId1, BigInteger.ONE);
        requestIdsReceiversMap.put(requestId2, BigInteger.ONE.add(BigInteger.ONE));
        requestItfExportRequest.setRequestIdsReceiversMap(requestIdsReceiversMap);
        return requestItfExportRequest;
    }

    public static RequestItfExportRequest generateRequestItfExportRequest(Set<UUID> requestIds,
                                                                          Map<UUID, BigInteger> requestIdsReceiversMap) {
        RequestItfExportRequest requestItfExportRequest = new RequestItfExportRequest();
        requestItfExportRequest.setProjectId(UUID.randomUUID());
        requestItfExportRequest.setRequestIds(requestIds);
        requestItfExportRequest.setItfUrl("http://itf.url");
        requestItfExportRequest.setSystemId(BigInteger.ONE);
        requestItfExportRequest.setOperationId(BigInteger.TEN);
        requestItfExportRequest.setRequestIdsReceiversMap(requestIdsReceiversMap);
        return requestItfExportRequest;
    }

    public static RequestExportResultResponse generateSuccessfulExportResponse(Request request,
                                                                               String exportedRequestUrl) {
        return RequestExportResultResponse.builder()
                .requestId(request.getId())
                .requestUrl(exportedRequestUrl)
                .status(RequestExportStatus.DONE)
                .build();
    }

    public static MiaExportResponseEvent generateMiaExportSuccessResponseEvent(UUID requestExportId,
                                                                               HttpRequest httpRequest) {
        MiaExportResponseEvent miaExportResponse = new MiaExportResponseEvent();
        miaExportResponse.setId(requestExportId);
        miaExportResponse.setRequestId(httpRequest.getId());
        miaExportResponse.setStatus(RequestExportStatus.DONE.name());
        String miaRequestUrl = "http://mia.request.url";
        miaExportResponse.setMiaUrl(miaRequestUrl);
        return miaExportResponse;
    }

    public static MiaExportResponseEvent generateMiaExportFailedResponseEvent(UUID requestExportId,
                                                                              HttpRequest httpRequest) {
        MiaExportResponseEvent miaExportResponse = new MiaExportResponseEvent();
        miaExportResponse.setId(requestExportId);
        miaExportResponse.setRequestId(httpRequest.getId());
        miaExportResponse.setStatus(RequestExportStatus.ERROR.name());
        String errorMessage = "Failed to export";
        miaExportResponse.setErrorMessage(errorMessage);
        return miaExportResponse;
    }

    public static ItfExportResponseEvent generateItfExportSuccessResponseEvent(UUID requestExportId, Request request) {
        ItfExportResponseEvent itfExportResponse = new ItfExportResponseEvent();
        itfExportResponse.setId(requestExportId);
        itfExportResponse.setRequestId(request.getId());
        itfExportResponse.setStatus(RequestExportStatus.DONE.name());
        String itfRequestUrl = "http://itf.request.url";
        itfExportResponse.setItfRequestUrl(itfRequestUrl);
        return itfExportResponse;
    }

    public static ItfExportResponseEvent generateItfExportFailedResponseEvent(UUID requestExportId, Request request) {
        ItfExportResponseEvent itfExportResponse = new ItfExportResponseEvent();
        itfExportResponse.setId(requestExportId);
        itfExportResponse.setRequestId(request.getId());
        itfExportResponse.setStatus(RequestExportStatus.ERROR.name());
        String errorMessage = "Failed to export";
        itfExportResponse.setErrorMessage(errorMessage);
        return itfExportResponse;
    }

    public static RequestExportEntity generateRequestExportEntityBySseId(UUID sseId, UUID requestId) {
        Map<UUID, RequestExportStatus> statuses = new HashMap<>();
        statuses.put(requestId, RequestExportStatus.IN_PROGRESS);
        return generateRequestExportEntity(UUID.randomUUID(), sseId, UUID.randomUUID(), statuses);
    }

    public static RequestExportEntity generateRequestExportEntity(UUID requestExportId, UUID sseId, UUID userId,
                                                                  Map<UUID, RequestExportStatus> requestStatuses) {
        RequestExportEntity requestExportEntity = new RequestExportEntity();
        requestExportEntity.setRequestExportId(requestExportId);
        requestExportEntity.setSseId(sseId);
        requestExportEntity.setUserId(userId);
        requestExportEntity.setRequestStatuses(requestStatuses);
        return requestExportEntity;
    }

    public static RequestHeader generateRequestHeader(String key, String value) {
        return new RequestHeader(UUID.randomUUID(), key, value, "new header", false);
    }

    public static RequestParam generateRequestParameter(String key, String value) {
        return new RequestParam(UUID.randomUUID(), key, value, "new header", false);
    }

    public static OAuth2AuthorizationSaveRequest generateRandomOAuth2AuthorizationSaveRequest() {
        OAuth2AuthorizationSaveRequest request = new OAuth2AuthorizationSaveRequest();
        request.setUrl("http://test.test");
        request.setClientId("clientId");
        request.setClientSecret("clientSecret");
        request.setUsername("username");
        request.setPassword("password");
        request.setScope("scope");
        request.setType(RequestAuthorizationType.OAUTH2);
        return request;
    }

    public static ItfLiteExecutionFinishEvent generateRestItfLiteExecutionFinishEvent() {
        ItfLiteExecutionFinishEvent itfLiteExecutionFinishEvent = new ItfLiteExecutionFinishEvent();
        itfLiteExecutionFinishEvent.setSseId(UUID.randomUUID());
        itfLiteExecutionFinishEvent.setRequestId(UUID.randomUUID());
        itfLiteExecutionFinishEvent.setTransportType(TransportType.REST);
        return itfLiteExecutionFinishEvent;
    }

    public static CertificateDto generateRandomCertificate() {
        CertificateDto cert = new CertificateDto();
        cert.setProtocol("protocol");
        cert.setTrustStorePassphrase("phrase");
        FileInfoDto info = new FileInfoDto();
        info.setName("identity.jks");
        info.setId("1");
        cert.setTrustStoreFileInfo(info);
        return cert;
    }

    public static CertificateDto generateRandomCertificateByPhrase(String phrase) {
        CertificateDto cert = generateRandomCertificate();
        cert.setTrustStorePassphrase(phrase);
        return cert;
    }

    public static ProjectEvent generateRandomProjectEvent(UUID projectId) {
        ProjectEvent event = new ProjectEvent();
        event.setProjectId(projectId);
        event.setProjectName("test");
        event.setDatasetFormat(DatasetFormat.DEFAULT);
        event.setType(ProjectEventType.CREATE);
        return event;
    }

    public static System generateEnvironmentSystem(UUID systemId, String systemName) {
        return new System(systemId, systemName, new Connection());
    }

    public static void mockTimer(MetricService metricService) {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        Metrics.addRegistry(meterRegistry);
        when(metricService.timer(any(), any(), any(), any(), any())).thenReturn(meterRegistry.timer("mockTimer"));
    }

    public static Map<String, String> generateBearerAuthMap() {
        Map<String, String> authorizationParametersMap = new HashMap<>();
        authorizationParametersMap.put(Constants.TOKEN, "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ4MHlzOEJ4N2lncm1BSnRvMkdzWFpnaUlMTnJTQV9aMk12TENTX2RWZjZ3In0");

        return authorizationParametersMap;
    }

    public static Map<String, String> generateOAuth2AuthMap(OAuth2GrantType grantType) {
        Map<String, String> authorizationParametersMap = new HashMap<>();
        authorizationParametersMap.put(Constants.GRANT_TYPE, grantType.getKey());
        authorizationParametersMap.put(Constants.HEADER_PREFIX_CAMEL_CASE, "Bearer");
        authorizationParametersMap.put(Constants.SCOPE, "scope");
        authorizationParametersMap.put(Constants.CLIENT_SECRET, "clientSecret");
        authorizationParametersMap.put(Constants.CLIENT_ID, "12345");
        authorizationParametersMap.put(Constants.ACCESS_TOKEN_URL, "localhost");
        if (OAuth2GrantType.PASSWORD_CREDENTIALS.equals(grantType)) {
            authorizationParametersMap.put(Constants.PASSWORD, "password");
            authorizationParametersMap.put(Constants.USERNAME, "username");
        }
        return authorizationParametersMap;
    }

    public static OAuth2RequestAuthorization generateRandomAuthorization() {
        OAuth2RequestAuthorization oAuth2RequestAuthorization = new OAuth2RequestAuthorization();
        oAuth2RequestAuthorization.setId(UUID.randomUUID());
        oAuth2RequestAuthorization.setType(RequestAuthorizationType.OAUTH2);
        oAuth2RequestAuthorization.setClientId("clientId");
        oAuth2RequestAuthorization.setClientSecret("clientSecret");
        oAuth2RequestAuthorization.setUrl("url");
        oAuth2RequestAuthorization.setGrantType(OAuth2GrantType.PASSWORD_CREDENTIALS);
        oAuth2RequestAuthorization.setHeaderPrefix("headerPrefix");
        oAuth2RequestAuthorization.setUsername("username");
        oAuth2RequestAuthorization.setPassword("password");
        oAuth2RequestAuthorization.setScope("scope");
        return oAuth2RequestAuthorization;
    }

    public static BearerRequestAuthorization generateRandomBearerAuthorization() {
        BearerRequestAuthorization bearerRequestAuthorization = new BearerRequestAuthorization();
        bearerRequestAuthorization.setId(UUID.randomUUID());
        bearerRequestAuthorization.setType(RequestAuthorizationType.BEARER);
        bearerRequestAuthorization.setToken("token");
        return bearerRequestAuthorization;
    }

    public static ExecutionCollectionRequestExecuteRequest generateRequestExecuteRequest() {
        ExecutionCollectionRequestExecuteRequest requestExecuteRequest = new ExecutionCollectionRequestExecuteRequest();
        requestExecuteRequest.setExecutionRequestId(UUID.randomUUID());
        requestExecuteRequest.setTestPlanId(UUID.randomUUID());
        requestExecuteRequest.setTestRunId(UUID.randomUUID());
        requestExecuteRequest.setProjectId(UUID.randomUUID());
        requestExecuteRequest.setSection(new AtpCompaund());
        requestExecuteRequest.setActionEntity(new ActionEntity());
        return requestExecuteRequest;
    }

    public static InheritFromParentAuthorizationSaveRequest generateInheritFromParentAuthSaveRequest(UUID authFolderId) {
        InheritFromParentAuthorizationSaveRequest authorization = new InheritFromParentAuthorizationSaveRequest();
        authorization.setType(RequestAuthorizationType.INHERIT_FROM_PARENT);
        authorization.setAuthorizationFolderId(authFolderId);
        return authorization;
    }

    public static InheritFromParentRequestAuthorization generateInheritFromParentRequestAuth(UUID authFolderId) {
        InheritFromParentRequestAuthorization authorization = new InheritFromParentRequestAuthorization();
        authorization.setType(RequestAuthorizationType.INHERIT_FROM_PARENT);
        authorization.setAuthorizationFolderId(authFolderId);
        return authorization;
    }

    public static BasicRequestAuthorization generateBasicRequestAuthorization() {
        BasicRequestAuthorization authorization = new BasicRequestAuthorization();
        authorization.setType(RequestAuthorizationType.BASIC);
        authorization.setUsername("username");
        authorization.setPassword("password");
        return authorization;
    }

    public static BearerRequestAuthorization generateBearerRequestAuthorization(String token) {
        BearerRequestAuthorization authorization = new BearerRequestAuthorization();
        authorization.setType(RequestAuthorizationType.BEARER);
        authorization.setToken(token);
        return authorization;
    }

    public static OAuth2RequestAuthorization generateOAuth2RequestAuthorization(OAuth2GrantType grantType) {
        OAuth2RequestAuthorization request = new OAuth2RequestAuthorization();
        request.setUrl("http://test.test");
        request.setClientId("clientId");
        request.setClientSecret("clientSecret");
        request.setUsername("username");
        request.setPassword("password");
        request.setScope("scope");
        request.setToken("token");
        request.setType(RequestAuthorizationType.OAUTH2);
        request.setGrantType(grantType);
        return request;
    }

    public static CollectionRunRequest createCollRunRequest(UUID testRunId, UUID requestId,
                                                            String requestName, int order) {
        CollectionRunRequest req = new CollectionRunRequest();
        req.setCollectionRunId(testRunId);
        req.setRequestId(requestId);
        req.setRequestName(requestName);
        req.setOrder(order);
        return req;
    }

    public static CollectionRunStackRequest createCollRunStackRequest(UUID testRunId, UUID requestId,
                                                                      String requestName, int order) {
        CollectionRunStackRequest req = new CollectionRunStackRequest();
        req.setCollectionRunId(testRunId);
        req.setRequestId(requestId);
        req.setRequestName(requestName);
        req.setOrder(order);
        return req;
    }

    public static FolderUpsetRequest createFolderUpsetRequest(String folderName) {
        return new FolderUpsetRequest(folderName, UUID.randomUUID(), UUID.randomUUID(),
                null, false, false, false, false, false, "", null, new Date());
    }
}
