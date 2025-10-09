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

package org.qubership.atp.itf.lite.backend.feign.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.qubership.atp.auth.springbootstarter.feign.exception.FeignClientException;
import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.crypt.exception.AtpEncryptException;
import org.qubership.atp.itf.lite.backend.enums.ContextType;
import org.qubership.atp.itf.lite.backend.feign.clients.JsScriptEngineFeignClient;
import org.qubership.atp.itf.lite.backend.feign.dto.HeaderDto;
import org.qubership.atp.itf.lite.backend.feign.dto.HttpResponseExceptionTypeEnum;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanDto;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanExecuteScriptRequestDto;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanExecuteScriptResponseDto;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanExecuteScriptResponseTestResultsInnerDto;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanExecuteScriptResponseTestResultsInnerErrorDto;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanPostmanResponseDto;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.RequestExecutionResponse;
import org.qubership.atp.itf.lite.backend.model.context.SaveRequestResolvingContext;
import org.qubership.atp.itf.lite.backend.service.EncryptionService;
import org.qubership.atp.itf.lite.backend.utils.CookieUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import clover.org.apache.commons.lang3.StringUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import feign.FeignException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class JsScriptEngineService {

    private final JsScriptEngineFeignClient jsScriptEngineFeignClient;
    private final EncryptionService encryptionService;
    private final ThreadLocal<List<String>> globalsEncrypted = new ThreadLocal<>();
    private final ThreadLocal<List<String>> collectionVariablesEncrypted = new ThreadLocal<>();
    private final ThreadLocal<List<String>> environmentEncrypted = new ThreadLocal<>();
    private final ThreadLocal<List<String>> iterationDataEncrypted = new ThreadLocal<>();
    private final ThreadLocal<List<String>> variablesEncrypted = new ThreadLocal<>();

    /**
     * Execute post script script in js engine.
     *
     * @param request          request to script engine
     * @param response         response
     * @param resolvingContext context
     */
    public PostmanExecuteScriptResponseDto evaluateRequestPostScript(
            RequestEntitySaveRequest request,
            RequestExecutionResponse response,
            SaveRequestResolvingContext resolvingContext) {
        if (request == null || StringUtils.isEmpty(request.getPostScripts())) {
            // nothing to do
            return null;
        }
        log.info("Evaluate pre-script for request with id: {}", request.getId());
        return executePostmanScript(false, request, response, resolvingContext);
    }

    /**
     * Execute pre script script in js engine.
     *
     * @param request          request to script engine
     * @param resolvingContext context
     */
    public PostmanExecuteScriptResponseDto evaluateRequestPreScript(
            RequestEntitySaveRequest request, SaveRequestResolvingContext resolvingContext) {
        if (request == null || StringUtils.isEmpty(request.getPreScripts())) {
            // nothing to do
            return null;
        }
        log.info("Evaluate pre-script for request with id: {}", request.getId());
        return executePostmanScript(true, request, null, resolvingContext);
    }

    private PostmanExecuteScriptResponseDto executePostmanScript(boolean isPreScript,
                                                                 RequestEntitySaveRequest request,
                                                                 RequestExecutionResponse response,
                                                                 SaveRequestResolvingContext resolvingContext) {
        try {
            PostmanExecuteScriptRequestDto postmanExecuteScriptRequestDto =
                    generateRequestToJsEngine(isPreScript, request, response, resolvingContext);
            PostmanExecuteScriptResponseDto jsScriptEngineResponse =
                    jsScriptEngineFeignClient.executePostmanScript(postmanExecuteScriptRequestDto).getBody();

            // Update context
            assert jsScriptEngineResponse != null;
            PostmanDto postmanDto = jsScriptEngineResponse.getPostman();
            request.setCookies(CookieUtils.convertPostmanCookieDtoListToCookieList(
                    postmanDto.getCookies() != null ? postmanDto.getCookies() : new ArrayList<>()));
            request.updateFromPostmanRequest(postmanDto.getPostmanRequest());
            if (!isPreScript) {
                response.updateFromPostmanResponse(postmanDto.getPostmanResponse());
            }
            resolvingContext.setGlobals(updateContext(ContextType.GLOBALS, postmanDto.getGlobals()));
            resolvingContext.setCollectionVariables(updateContext(ContextType.COLLECTION_VARIABLES,
                    postmanDto.getCollectionVariables()));
            resolvingContext.setEnvironment(updateContext(ContextType.ENVIRONMENT, postmanDto.getEnvironment()));
            resolvingContext.setIterationData(updateContext(ContextType.ITERATION_DATA, postmanDto.getIterationData()));
            resolvingContext.setVariables(updateContext(ContextType.VARIABLES, postmanDto.getVariables()));
            log.debug("All results context {}, isPreScript? {}", resolvingContext, isPreScript);
            return jsScriptEngineResponse;
        } catch (AtpDecryptException decryptEx) {
            return generateExecuteScriptErrorResponse("DECRYPT CONTEXT BEFORE EXECUTION",
                    decryptEx.getMessage(), decryptEx, HttpResponseExceptionTypeEnum.DECRYPT_EXCEPTION);
        } catch (AtpEncryptException encryptEx) {
            return generateExecuteScriptErrorResponse("ENCRYPT CONTEXT AFTER EXECUTION",
                    encryptEx.getMessage(), encryptEx, HttpResponseExceptionTypeEnum.ENCRYPT_EXCEPTION);
        } catch (FeignClientException feignClientEx) {
            String errorMessage = feignClientEx.getMessage();
            HttpResponseExceptionTypeEnum exceptionType = HttpResponseExceptionTypeEnum.EXECUTION_EXCEPTION;
            if (feignClientEx.getStatus().equals(HttpStatus.SERVICE_UNAVAILABLE.value())) {
                exceptionType = HttpResponseExceptionTypeEnum.UNAVAILABLE_EXCEPTION;
            }

            try {
                JsonObject feignClientExceptionAsJson = JsonParser.parseString(
                        feignClientEx.getErrorMessage()).getAsJsonObject();
                if (feignClientExceptionAsJson.has("reason")) {
                    String scriptEngineReason = feignClientExceptionAsJson.get("reason").getAsString();
                    String failToCreatePostmanSandboxContextReasonCode = "ITFLSE-0001";
                    if (failToCreatePostmanSandboxContextReasonCode.equals(scriptEngineReason)) {
                        exceptionType = HttpResponseExceptionTypeEnum.POSTMAN_SANDBOX_CONTEXT_EXCEPTION;
                    }
                }
                if (feignClientExceptionAsJson.has("message")) {
                    JsonElement messageElement = feignClientExceptionAsJson.get("message");
                    if (messageElement.isJsonObject()) {
                        JsonObject messageObj = messageElement.getAsJsonObject();
                        if (messageObj.has("message") && messageObj.get("message").isJsonPrimitive()) {
                            errorMessage = messageObj.get("message").getAsString();
                        }
                        if (messageObj.has("details") && messageObj.get("details").isJsonObject()) {
                            String detailsMessage = extractMessageFromDetails(messageObj);
                            if (!StringUtils.isEmpty(detailsMessage)) {
                                errorMessage = detailsMessage;
                            }
                        }
                    } else if (messageElement.isJsonNull()) {
                        errorMessage = "Null error message is provided";
                    } else {
                        errorMessage = messageElement.getAsString();
                    }
                }

                if (feignClientExceptionAsJson.has("details")
                        && feignClientExceptionAsJson.get("details").isJsonObject()) {
                    String errorMessageFromJson = extractMessageFromDetails(feignClientExceptionAsJson);
                    if (!StringUtils.isEmpty(errorMessageFromJson)) {
                        errorMessage = errorMessageFromJson;
                    }
                }
            } catch (JsonSyntaxException | IllegalStateException exception) {
                log.warn("Can't parse feign client exception message into json object.", exception);
            }

            return generateExecuteScriptErrorResponse("[FEIGN] EXECUTE JS SCRIPT", errorMessage,
                    feignClientEx, exceptionType);
        } catch (FeignException feignEx) {
            return generateExecuteScriptErrorResponse("JS SCRIPT NOT AVAILABLE", feignEx.getMessage(), feignEx,
                    HttpResponseExceptionTypeEnum.EXECUTION_EXCEPTION);
        } catch (Exception e) {
            return generateExecuteScriptErrorResponse("[OTHER] EXECUTE JS SCRIPT", e.getMessage(), e,
                    HttpResponseExceptionTypeEnum.EXECUTION_EXCEPTION);
        }
    }

    private String extractMessageFromDetails(JsonObject jsonObject) {
        String errorMessageFromJson = "";
        if (jsonObject.get("details").getAsJsonObject().has("name")) {
            errorMessageFromJson = jsonObject.get("details").getAsJsonObject()
                    .get("name").getAsString();
        }
        if (jsonObject.get("details").getAsJsonObject().has("message")) {
            errorMessageFromJson += StringUtils.isEmpty(errorMessageFromJson)
                    ? jsonObject.get("details").getAsJsonObject()
                    .get("message").getAsString()
                    : (": " + jsonObject.get("details").getAsJsonObject()
                    .get("message").getAsString());
        }
        return errorMessageFromJson;
    }

    private Map<String, Object> generateContext(ContextType type, Map<String, Object> originContext)
            throws AtpDecryptException {
        Map<String, Object> targetContext = new HashMap<>();
        for (Map.Entry<String, Object> c: originContext.entrySet()) {
            if (Objects.nonNull(c.getValue()) && encryptionService.isEncrypted(c.getValue().toString())) {
                switch (type) {
                    case GLOBALS:
                        if (globalsEncrypted.get() == null) {
                            this.globalsEncrypted.set(new ArrayList<>());
                        }
                        globalsEncrypted.get().add(c.getKey());
                        break;
                    case COLLECTION_VARIABLES:
                        if (collectionVariablesEncrypted.get() == null) {
                            this.collectionVariablesEncrypted.set(new ArrayList<>());
                        }
                        collectionVariablesEncrypted.get().add(c.getKey());
                        break;
                    case ENVIRONMENT:
                        if (environmentEncrypted.get() == null) {
                            this.environmentEncrypted.set(new ArrayList<>());
                        }
                        environmentEncrypted.get().add(c.getKey());
                        break;
                    case ITERATION_DATA:
                        if (iterationDataEncrypted.get() == null) {
                            this.iterationDataEncrypted.set(new ArrayList<>());
                        }
                        iterationDataEncrypted.get().add(c.getKey());
                        break;
                    case VARIABLES:
                        if (variablesEncrypted.get() == null) {
                            this.variablesEncrypted.set(new ArrayList<>());
                        }
                        variablesEncrypted.get().add(c.getKey());
                        break;
                    default:
                }
                targetContext.put(c.getKey(), encryptionService.decrypt(c.getValue().toString()));
            } else {
                targetContext.put(c.getKey(), c.getValue());
            }
        }
        return targetContext;
    }

    private Map<String, Object> updateContext(ContextType type, Map<String, Object> sourceContext)
            throws AtpEncryptException {
        log.debug("Update context type: {} and sourceContext {}", type, sourceContext);
        Map<String, Object> targetContext = new HashMap<>();
        List<String> keysEncrypted = new ArrayList<>();
        switch (type) {
            case GLOBALS:
                keysEncrypted = globalsEncrypted.get();
                globalsEncrypted.remove();
                break;
            case COLLECTION_VARIABLES:
                keysEncrypted = collectionVariablesEncrypted.get();
                collectionVariablesEncrypted.remove();
                break;
            case ENVIRONMENT:
                keysEncrypted = environmentEncrypted.get();
                environmentEncrypted.remove();
                break;
            case ITERATION_DATA:
                keysEncrypted = iterationDataEncrypted.get();
                iterationDataEncrypted.remove();
                break;
            case VARIABLES:
                keysEncrypted = variablesEncrypted.get();
                variablesEncrypted.remove();
                break;
            default:
        }
        keysEncrypted = keysEncrypted == null ? new ArrayList<>() : keysEncrypted;
        for (Map.Entry<String, Object> c: sourceContext.entrySet()) {
            if (Objects.nonNull(c.getValue()) && keysEncrypted.contains(c.getKey())) {
                targetContext.put(c.getKey(), encryptionService.encrypt(c.getValue().toString()));
            } else {
                targetContext.put(c.getKey(), c.getValue());
            }
        }
        return targetContext;
    }

    private PostmanPostmanResponseDto generatePostmanResponseDto(RequestExecutionResponse itfLiteResponse) {
        return new PostmanPostmanResponseDto()
                .status(itfLiteResponse.getStatusText())
                .code(Integer.parseInt(itfLiteResponse.getStatusCode()))
                .header(itfLiteResponse.getResponseHeaders()
                        .stream()
                        .map(h -> new HeaderDto().key(h.getKey()).value(h.getValue()))
                        .collect(Collectors.toList()))
                .body(itfLiteResponse.getBody())
                .responseTime(itfLiteResponse.getDuration().intValue());
    }

    private PostmanExecuteScriptRequestDto generateRequestToJsEngine(boolean isPreScript,
                                                                     RequestEntitySaveRequest request,
                                                                     @Nullable RequestExecutionResponse response,
                                                                     SaveRequestResolvingContext resolvingContext)
            throws AtpDecryptException {
        log.debug("Generate request to JS engine for request(id: {})", request.getId());
        PostmanDto postman = new PostmanDto()
                .postmanRequest(request.getPostmanRequest())
                .globals(generateContext(ContextType.GLOBALS, resolvingContext.getGlobals()))
                .collectionVariables(generateContext(ContextType.COLLECTION_VARIABLES,
                        resolvingContext.getCollectionVariables()))
                .environment(generateContext(ContextType.ENVIRONMENT, resolvingContext.getEnvironment()))
                .iterationData(generateContext(ContextType.ITERATION_DATA, resolvingContext.getIterationData()))
                .variables(generateContext(ContextType.VARIABLES, resolvingContext.getVariables()))
                .cookies(CookieUtils.convertCookieListToPostmanCookieDtoList(
                        request.getCookies() != null ? request.getCookies() : new ArrayList<>()));
        if (Objects.nonNull(response)) {
            postman.setPostmanResponse(generatePostmanResponseDto(response));
        }
        return new PostmanExecuteScriptRequestDto()
                .projectId(request.getProjectId())
                .postman(postman)
                .script(isPreScript ? request.getPreScripts() : request.getPostScripts());
    }

    private PostmanExecuteScriptResponseDto generateExecuteScriptErrorResponse(
            String step, String errorMessage, Exception ex, HttpResponseExceptionTypeEnum httpResponseExceptionType) {
        log.error(errorMessage, ex);
        return new PostmanExecuteScriptResponseDto()
                .testResults(Collections.singletonList(new PostmanExecuteScriptResponseTestResultsInnerDto()
                        .name(step)
                        .index(BigDecimal.valueOf(0))
                        .passed(false)
                        .error(new PostmanExecuteScriptResponseTestResultsInnerErrorDto()
                                .message(errorMessage)
                                .httpResponseExceptionType(httpResponseExceptionType))));
    }
}
