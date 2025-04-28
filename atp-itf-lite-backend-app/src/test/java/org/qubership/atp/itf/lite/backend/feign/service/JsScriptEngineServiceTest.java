package org.qubership.atp.itf.lite.backend.feign.service;

import static java.nio.charset.Charset.defaultCharset;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.auth.springbootstarter.feign.exception.FeignClientException;
import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.crypt.exception.AtpEncryptException;
import org.qubership.atp.itf.lite.backend.enums.http.RequestBodyType;
import org.qubership.atp.itf.lite.backend.feign.clients.JsScriptEngineFeignClient;
import org.qubership.atp.itf.lite.backend.feign.dto.HttpResponseExceptionTypeEnum;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanExecuteScriptRequestDto;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanExecuteScriptResponseDto;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanExecuteScriptResponseTestResultsInnerDto;
import org.qubership.atp.itf.lite.backend.model.api.request.HttpRequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpHeaderSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpParamSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExecutionHeaderResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExecutionResponse;
import org.qubership.atp.itf.lite.backend.model.context.SaveRequestResolvingContext;
import org.qubership.atp.itf.lite.backend.model.entities.RequestBody;
import org.qubership.atp.itf.lite.backend.model.entities.http.methods.HttpMethod;
import org.qubership.atp.itf.lite.backend.service.EncryptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;

import com.google.gson.Gson;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;

@ExtendWith(MockitoExtension.class)
public class JsScriptEngineServiceTest {

    @Mock
    JsScriptEngineFeignClient jsScriptEngineFeignClient;
    @Mock
    EncryptionService encryptionService;
    @InjectMocks
    JsScriptEngineService scriptService;

    @Test
    public void evaluatePreScriptTest() throws IOException, AtpDecryptException, AtpEncryptException {
        setupMockBooksResponse("JsScriptEngine/preResponse.json");
        when(encryptionService.isEncrypted(eq("globals"))).thenReturn(true);
        when(encryptionService.decrypt(eq("globals"))).thenReturn("globalsEncrypted");
        when(encryptionService.encrypt(eq("globalsEncryptedUpdated"))).thenReturn("globalsUpdated");

        when(encryptionService.isEncrypted(eq("collection"))).thenReturn(true);
        when(encryptionService.decrypt(eq("collection"))).thenReturn("collectionEncrypted");
        when(encryptionService.encrypt(eq("collectionEncryptedUpdated"))).thenReturn("collectionUpdated");

        when(encryptionService.isEncrypted(eq("environment"))).thenReturn(true);
        when(encryptionService.decrypt(eq("environment"))).thenReturn("environmentEncrypted");
        when(encryptionService.encrypt(eq("environmentEncryptedUpdated"))).thenReturn("environmentUpdated");

        when(encryptionService.isEncrypted(eq("data"))).thenReturn(true);
        when(encryptionService.decrypt(eq("data"))).thenReturn("dataEncrypted");
        when(encryptionService.encrypt(eq("dataEncryptedUpdated"))).thenReturn("dataUpdated");

        when(encryptionService.isEncrypted(eq("local"))).thenReturn(true);
        when(encryptionService.decrypt(eq("local"))).thenReturn("localEncrypted");
        when(encryptionService.encrypt(eq("localEncryptedUpdated"))).thenReturn("localUpdated");

        HttpRequestEntitySaveRequest request = new HttpRequestEntitySaveRequest();
        request.setId(UUID.randomUUID());
        request.setName("test");
        request.setHttpMethod(HttpMethod.GET);
        request.setBody(new RequestBody("test", RequestBodyType.JSON));
        request.setUrl("http://localohst:8080/path?query_1=q_value_1");
        request.setRequestHeaders(Arrays.asList(new HttpHeaderSaveRequest("header1", "value1", "", false)));
        request.setRequestParams(Collections.singletonList(new HttpParamSaveRequest("query_2", "q_value_2", "")));
        request.setRequestHeaders(new ArrayList<HttpHeaderSaveRequest>(){{
            add(new HttpHeaderSaveRequest("header_1", "h_value_1", ""));
        }});

        SaveRequestResolvingContext resolvingContext = SaveRequestResolvingContext.builder()
                .globals(new HashMap<String, Object>(){{
                    put("ITF_LITE_GLOBALS_TEST", "globals");
                }})
                .collectionVariables(new HashMap<String, Object>(){{
                    put("ITF_LITE_COLLECTIONVARIABLES_TEST", "collection");
                }})
                .environment(new HashMap<String, Object>(){{
                    put("ITF_LITE_ENVIRONMENT_TEST", "environment");
                }})
                .iterationData(new HashMap<String, Object>(){{
                    put("ITF_LITE_ITERATIONDATA_TEST", "data");
                }})
                .variables(new HashMap<String, Object>(){{
                    put("TEST", "local");
                }}).build();


        request.setPreScripts(""
                // change method
                + "pm.request.method=\"POST\";\n"
                // change request body
                + "pm.request.body.raw=\"new body\";\n"
                // add header
                + "pm.request.headers.add({\"key\":\"h_value_2\", \"value\":\"header_2\"});\n"
                // add query parameter
                + "pm.request.addQueryParams([{key: \"query_3\", value: \"q_value_3\"}]);\n"
                // change global variables
                + "pm.globals.set(\"g1\", \"2\");\n"
                // change collection variables
                + "pm.collectionVariables.set(\"c1\", \"2\");\n"
                // change env variables
                + "pm.environment.set(\"e1\", \"2\");\n"
                // change data variables
                + "pm.iterationData.set(\"d1\", \"2\");\n"
                + "postman.setEnvironmentVariable(\"e2\", \"val2\");\n");

        scriptService.evaluateRequestPreScript(request, resolvingContext);
        Assertions.assertEquals(HttpMethod.POST, request.getHttpMethod());
        Assertions.assertEquals("new body", request.getBody().getContent());
        Assertions.assertEquals(2, request.getRequestHeaders().size());
        Assertions.assertEquals(3, request.getRequestParams().size());
        Assertions.assertTrue(resolvingContext.getGlobals().containsKey("g1"));
        Assertions.assertTrue(resolvingContext.getGlobals().containsKey("ITF_LITE_GLOBALS_TEST"));
        Assertions.assertEquals("globalsUpdated", resolvingContext.getGlobals().get("ITF_LITE_GLOBALS_TEST"));
        Assertions.assertTrue(resolvingContext.getCollectionVariables().containsKey("c1"));
        Assertions.assertTrue(resolvingContext.getCollectionVariables().containsKey("ITF_LITE_COLLECTIONVARIABLES_TEST"));
        Assertions.assertEquals("collectionUpdated", resolvingContext.getCollectionVariables().get("ITF_LITE_COLLECTIONVARIABLES_TEST"));
        Assertions.assertTrue(resolvingContext.getEnvironment().containsKey("e1"));
        Assertions.assertTrue(resolvingContext.getEnvironment().containsKey("e2"));
        Assertions.assertEquals("val2", resolvingContext.getEnvironment().get("e2"));
        Assertions.assertTrue(resolvingContext.getEnvironment().containsKey("ITF_LITE_ENVIRONMENT_TEST"));
        Assertions.assertEquals("environmentUpdated",resolvingContext.getEnvironment().get("ITF_LITE_ENVIRONMENT_TEST"));
        Assertions.assertTrue(resolvingContext.getIterationData().containsKey("ITF_LITE_ITERATIONDATA_TEST"));
        Assertions.assertEquals("dataUpdated", resolvingContext.getIterationData().get("ITF_LITE_ITERATIONDATA_TEST"));
        Assertions.assertTrue(resolvingContext.getVariables().containsKey("TEST"));
        Assertions.assertEquals("localUpdated", resolvingContext.getVariables().get("TEST"));
        // No propagation from IterationData JS engine
        //Assertions.assertTrue(resolvingContext.getIterationData().containsKey("d1"));
    }

    @Test
    public void evaluatePostScriptTest() throws IOException {
        setupMockBooksResponse("JsScriptEngine/postResponse.json");
        HttpRequestEntitySaveRequest request = new HttpRequestEntitySaveRequest();
        request.setId(UUID.randomUUID());
        request.setName("test");
        request.setHttpMethod(HttpMethod.GET);
        request.setUrl("http://localohst:8080/path");
        request.setRequestHeaders(Arrays.asList(new HttpHeaderSaveRequest("header1", "value1", "", false)));
        request.setPostScripts("pm.response.headers.append({key: \"key2\", value: \"value2\"})\n");

        RequestExecutionResponse response = new RequestExecutionResponse();
        response.setBody("test");
        response.setStatusCode("200");
        response.setStatusText("OK");
        response.setDuration(new BigInteger(String.valueOf(0)));
        response.setResponseHeaders(new ArrayList<RequestExecutionHeaderResponse>(){{
            add(new RequestExecutionHeaderResponse("key1", "val1"));
        }});

        SaveRequestResolvingContext resolvingContext = SaveRequestResolvingContext.builder().build();

        PostmanExecuteScriptResponseDto executionResults =
                scriptService.evaluateRequestPostScript(request, response, resolvingContext);
        List<PostmanExecuteScriptResponseTestResultsInnerDto> testResults = executionResults.getTestResults();
        Assertions.assertEquals(2, response.getResponseHeaders().size());
        Assertions.assertEquals(2, testResults.size());
        Assertions.assertEquals("expected 2 to deeply equal 1", testResults.get(1).getError().getMessage());
    }

    @Test
    public void jsScript_ifNoPreScript_thenReturnNull() {
        HttpRequestEntitySaveRequest request = new HttpRequestEntitySaveRequest();
        request.setId(UUID.randomUUID());
        request.setName("test");
        request.setHttpMethod(HttpMethod.GET);
        request.setUrl("http://localohst:8080/path");
        request.setRequestHeaders(Arrays.asList(new HttpHeaderSaveRequest("header1", "value1", "", false)));

        SaveRequestResolvingContext resolvingContext = SaveRequestResolvingContext.builder().build();

        Assertions.assertNull(scriptService.evaluateRequestPreScript(request, resolvingContext));
    }

    @Test
    public void jsScript_ifNoPostScript_thenReturnNull() {
        HttpRequestEntitySaveRequest request = new HttpRequestEntitySaveRequest();
        request.setId(UUID.randomUUID());
        request.setName("test");
        request.setHttpMethod(HttpMethod.GET);
        request.setUrl("http://localohst:8080/path");
        request.setRequestHeaders(Arrays.asList(new HttpHeaderSaveRequest("header1", "value1", "", false)));

        SaveRequestResolvingContext resolvingContext = SaveRequestResolvingContext.builder().build();

        Assertions.assertNull(scriptService.evaluateRequestPostScript(request, null, resolvingContext));
    }

    @Test
    public void jsScriptNotAvailable_thenReturnPostmanExecuteScriptResponseTestResultsDto() {
        Request requestException = Request.create(Request.HttpMethod.GET, "url",
                new HashMap<>(), null, new RequestTemplate());
        when(jsScriptEngineFeignClient.executePostmanScript(any(PostmanExecuteScriptRequestDto.class)))
                .thenThrow(new FeignException.NotFound("JS NOT FOUND", requestException, null, null));
        HttpRequestEntitySaveRequest request = new HttpRequestEntitySaveRequest();
        request.setId(UUID.randomUUID());
        request.setName("test");
        request.setHttpMethod(HttpMethod.GET);
        request.setUrl("http://localohst:8080/path");
        request.setRequestHeaders(Arrays.asList(new HttpHeaderSaveRequest("header1", "value1", "", false)));
        request.setPreScripts("pm.response.headers.append({key: \"key2\", value: \"value2\"})\n");

        SaveRequestResolvingContext resolvingContext = SaveRequestResolvingContext.builder().build();

        PostmanExecuteScriptResponseDto executionResults =
                scriptService.evaluateRequestPreScript(request, resolvingContext);
        List<PostmanExecuteScriptResponseTestResultsInnerDto> testResults = executionResults.getTestResults();
        Assertions.assertEquals(1, testResults.size());
        Assertions.assertEquals("JS SCRIPT NOT AVAILABLE", testResults.get(0).getName());
        Assertions.assertEquals("JS NOT FOUND", testResults.get(0).getError().getMessage());
    }

    @Test
    public void jsScript_ifExecutionFailed_thenReturnPostmanExecuteScriptResponseTestResultsDto() {
        Request requestException = Request.create(Request.HttpMethod.POST, "url",
                new HashMap<>(), null, new RequestTemplate());
        when(jsScriptEngineFeignClient.executePostmanScript(any(PostmanExecuteScriptRequestDto.class)))
                .thenThrow(new FeignClientException(
                        500,
                        "pm.request.addHeaders is not a function",
                        Request.HttpMethod.POST,
                        new HashMap<>(),
                        requestException));
        HttpRequestEntitySaveRequest request = new HttpRequestEntitySaveRequest();
        request.setId(UUID.randomUUID());
        request.setName("test");
        request.setHttpMethod(HttpMethod.GET);
        request.setUrl("http://localohst:8080/path");
        request.setRequestHeaders(Arrays.asList(new HttpHeaderSaveRequest("header1", "value1", "", false)));
        request.setPreScripts("pm.response.headers.append({key: \"key2\", value: \"value2\"})\n");

        SaveRequestResolvingContext resolvingContext = SaveRequestResolvingContext.builder().build();

        PostmanExecuteScriptResponseDto executionResults =
                scriptService.evaluateRequestPreScript(request, resolvingContext);
        List<PostmanExecuteScriptResponseTestResultsInnerDto> testResults = executionResults.getTestResults();
        Assertions.assertEquals(1, testResults.size());
        Assertions.assertEquals("[FEIGN] EXECUTE JS SCRIPT", testResults.get(0).getName());
        Assertions.assertEquals("500 pm.request.addHeaders is not a function",
                testResults.get(0).getError().getMessage());
    }

    @Test
    public void jsScriptTest_serviceUnavailableResponse_thenReturnPostmanExecuteScriptResponseTestResultsDto() {
        Request requestException = Request.create(Request.HttpMethod.POST, "url",
                new HashMap<>(), null, new RequestTemplate());
        when(jsScriptEngineFeignClient.executePostmanScript(any(PostmanExecuteScriptRequestDto.class)))
                .thenThrow(new FeignClientException(
                        503,
                        "unavailable",
                        Request.HttpMethod.POST,
                        new HashMap<>(),
                        requestException));
        HttpRequestEntitySaveRequest request = new HttpRequestEntitySaveRequest();
        request.setId(UUID.randomUUID());
        request.setName("test");
        request.setHttpMethod(HttpMethod.GET);
        request.setUrl("http://localohst:8080/path");
        request.setRequestHeaders(Arrays.asList(new HttpHeaderSaveRequest("header1", "value1", "", false)));
        request.setPreScripts("pm.response.headers.append({key: \"key2\", value: \"value2\"})\n");

        SaveRequestResolvingContext resolvingContext = SaveRequestResolvingContext.builder().build();

        PostmanExecuteScriptResponseDto executionResults =
                scriptService.evaluateRequestPreScript(request, resolvingContext);
        List<PostmanExecuteScriptResponseTestResultsInnerDto> testResults = executionResults.getTestResults();
        Assertions.assertEquals(1, testResults.size());
        Assertions.assertEquals("[FEIGN] EXECUTE JS SCRIPT", testResults.get(0).getName());
        Assertions.assertEquals("503 unavailable",
                testResults.get(0).getError().getMessage());
        Assertions.assertEquals(HttpResponseExceptionTypeEnum.UNAVAILABLE_EXCEPTION,
                testResults.get(0).getError().getHttpResponseExceptionType());
    }

    @Test
    public void jsScriptTest_cannotCreatePostmanContext_thenReturnPostmanExecuteScriptResponseTestResultsDto() {
        Request requestException = Request.create(Request.HttpMethod.POST, "url",
                new HashMap<>(), null, new RequestTemplate());
        when(jsScriptEngineFeignClient.executePostmanScript(any(PostmanExecuteScriptRequestDto.class)))
                .thenThrow(new FeignClientException(
                        500,
                        "{\"reason\": \"ITFLSE-0001\", \"message\": \"Failed to create postman sandbox context\"}",
                        Request.HttpMethod.POST,
                        new HashMap<>(),
                        requestException));
        HttpRequestEntitySaveRequest request = new HttpRequestEntitySaveRequest();
        request.setId(UUID.randomUUID());
        request.setName("test");
        request.setHttpMethod(HttpMethod.GET);
        request.setUrl("http://localohst:8080/path");
        request.setRequestHeaders(Arrays.asList(new HttpHeaderSaveRequest("header1", "value1", "", false)));
        request.setPreScripts("pm.response.headers.append({key: \"key2\", value: \"value2\"})\n");

        SaveRequestResolvingContext resolvingContext = SaveRequestResolvingContext.builder().build();

        PostmanExecuteScriptResponseDto executionResults =
                scriptService.evaluateRequestPreScript(request, resolvingContext);
        List<PostmanExecuteScriptResponseTestResultsInnerDto> testResults = executionResults.getTestResults();
        Assertions.assertEquals(1, testResults.size());
        Assertions.assertEquals("[FEIGN] EXECUTE JS SCRIPT", testResults.get(0).getName());
        Assertions.assertEquals("Failed to create postman sandbox context",
                testResults.get(0).getError().getMessage());
        Assertions.assertEquals(HttpResponseExceptionTypeEnum.POSTMAN_SANDBOX_CONTEXT_EXCEPTION,
                testResults.get(0).getError().getHttpResponseExceptionType());
    }

    @Test
    public void jsScript_ifUnexpectedException_thenReturnPostmanExecuteScriptResponseTestResultsDto() {

        when(jsScriptEngineFeignClient.executePostmanScript(any(PostmanExecuteScriptRequestDto.class)))
                .thenThrow(new RuntimeException("request entity too large"));
        HttpRequestEntitySaveRequest request = new HttpRequestEntitySaveRequest();
        request.setId(UUID.randomUUID());
        request.setName("test");
        request.setHttpMethod(HttpMethod.GET);
        request.setUrl("http://localohst:8080/path");
        request.setRequestHeaders(Arrays.asList(new HttpHeaderSaveRequest("header1", "value1", "", false)));
        request.setPreScripts("pm.response.headers.append({key: \"key2\", value: \"value2\"})\n");

        SaveRequestResolvingContext resolvingContext = SaveRequestResolvingContext.builder().build();

        PostmanExecuteScriptResponseDto executionResults =
                scriptService.evaluateRequestPreScript(request, resolvingContext);
        List<PostmanExecuteScriptResponseTestResultsInnerDto> testResults = executionResults.getTestResults();
        Assertions.assertEquals(1, testResults.size());
        Assertions.assertEquals("[OTHER] EXECUTE JS SCRIPT", testResults.get(0).getName());
        Assertions.assertEquals("request entity too large", testResults.get(0).getError().getMessage());
    }

    private void setupMockBooksResponse(String fileName) throws IOException {
        when(jsScriptEngineFeignClient.executePostmanScript(any(PostmanExecuteScriptRequestDto.class)))
                .thenReturn(
                        ResponseEntity.ok(new Gson()
                                .fromJson(StreamUtils.copyToString(
                                                JsScriptEngineServiceTest.class.getClassLoader().getResourceAsStream(fileName),
                                                defaultCharset()),
                                        PostmanExecuteScriptResponseDto.class)));
    }
}
