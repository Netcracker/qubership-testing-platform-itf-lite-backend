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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.codehaus.commons.compiler.util.Producer;
import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteRequestEnvironmentNotSpecifiedException;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteRequestEnvironmentVariableNotFoundException;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteRequestHeaderDecryptException;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteRequestParamDecryptException;
import org.qubership.atp.itf.lite.backend.feign.service.EnvironmentFeignService;
import org.qubership.atp.itf.lite.backend.model.api.request.HttpRequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.BearerAuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.OAuth2AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpHeaderSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpParamSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.environments.Connection;
import org.qubership.atp.itf.lite.backend.model.api.response.environments.System;
import org.qubership.atp.itf.lite.backend.model.entities.RequestBody;
import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EnvironmentVariableService {

    // TODO: a lot of if-conditions related with encrypted/unencrypted environment variables. Can be refactored

    // named group for searching variable names without "ENV."
    private final String envVariableWithoutSpecialChars = "envVariableWithoutSpecialChars";
    private final Pattern envVariablesSearchPattern =
            Pattern.compile("(\\$\\{(?i)ENV\\.(?<" + envVariableWithoutSpecialChars + ">[^}]+)\\})");

    private final String encryptedEnvParameterPrefix = "EncryptedParameterENV";
    private final Pattern envEncryptedVariablesSearchPattern =
            Pattern.compile("(\\{(?i)" + encryptedEnvParameterPrefix
                    + "\\.(?<" + envVariableWithoutSpecialChars + ">[^}]+)\\})");

    private final String envName = "envName";
    private final Pattern encodedEnvVariableSearchPattern = Pattern.compile(
            "(%24%7B(?<" + envName + ">[^%7D]+)%7D)");

    private final EnvironmentFeignService environmentFeignService;
    private final EncryptionService encryptionService;

    /**
     * Resolve environment parameters values in OAuth2Authorization request.
     *
     * @param request OAuth2 authorization request
     */
    public void resolveEnvironmentParameters(OAuth2AuthorizationSaveRequest request, UUID environmentId)
            throws AtpDecryptException {
        log.debug("Resolve environment parameters in OAuth2 authorization request, environment id: {}", environmentId);

        Map<Producer<String>, Consumer<String>> resolveMap = new HashMap<>();
        resolveMap.put(request::getUrl, request::setUrl);
        resolveMap.put(request::getClientId, request::setClientId);
        resolveMap.put(request::getClientSecret, request::setClientSecret);
        resolveMap.put(request::getUsername, request::setUsername);
        resolveMap.put(request::getPassword, request::setPassword);
        resolveMap.put(request::getScope, request::setScope);

        List<String> resolvableFields = Stream.of(request.getUrl(), request.getClientId(), request.getClientSecret(),
                request.getUsername(), request.getPassword(), request.getScope())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        resolveEnvironmentParameters(resolvableFields, resolveMap, environmentId);
    }

    /**
     * Resolve environment parameters values in BearerAuthorizationSaveRequest request.
     *
     * @param request Bearer authorization request
     */
    public void resolveEnvironmentParameters(BearerAuthorizationSaveRequest request, UUID environmentId)
            throws AtpDecryptException {
        log.debug("Resolve environment parameters in OAuth2 authorization request, environment id: {}", environmentId);
        List<String> resolvableFields = Stream.of(request.getToken())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<Producer<String>, Consumer<String>> resolveMap = new HashMap<>();
        resolveMap.put(request::getToken, request::setToken);

        resolveEnvironmentParameters(resolvableFields, resolveMap, environmentId);
    }

    /**
     * Resolve environment parameters values in auth request.
     */
    public void resolveEnvironmentParameters(List<String> resolvableFields,
                                             Map<Producer<String>, Consumer<String>> resolveMap, UUID environmentId)
            throws AtpDecryptException {
        log.debug("Resolve environment parameters in OAuth2 authorization request, environment id: {}", environmentId);
        if (isEnvironmentVariablesPresent(resolvableFields)) {
            log.debug("Found environment parameters");
            if (isNull(environmentId)) {
                log.error("The environment has not been specified for the request");
                throw new ItfLiteRequestEnvironmentNotSpecifiedException();
            }
            List<System> systems = environmentFeignService.getEnvironmentSystems(environmentId);
            List<String> notFoundVariables = new ArrayList<>();

            for (Map.Entry<Producer<String>, Consumer<String>> entry : resolveMap.entrySet()) {
                resolveParameter(entry.getKey(), entry.getValue(), systems, notFoundVariables);
            }

            checkNotFoundVariables(notFoundVariables);
        }
    }

    /**
     * Resolve environment parameters values in http requests.
     *
     * @param httpRequest http request
     */
    public void resolveEnvironmentParameters(HttpRequestEntitySaveRequest httpRequest, boolean onlyEncrypted,
                                             UUID environmentId)
            throws AtpDecryptException {
        // endpoint url, parameters, headers, body
        final UUID requestId = httpRequest.getId();
        log.debug("Resolve values for request id = {}", requestId);
        if (isEnvironmentVariablesPresent(httpRequest) && !onlyEncrypted) {
            log.debug("Environment variables are not found in request");
            return;
        }
        if (isEncryptedEnvironmentVariablesPresent(httpRequest) && onlyEncrypted) {
            log.debug("Encrypted environment variables are not found in request");
            return;
        }
        if (environmentId == null) {
            log.error("The environment has not been specified for the request '{}'", requestId);
            throw new ItfLiteRequestEnvironmentNotSpecifiedException();
        }
        log.debug("Get list of systems for environment id = {}", environmentId);
        List<System> systems = environmentFeignService.getEnvironmentSystems(environmentId);
        List<String> notFoundVariables = new ArrayList<>();
        String url = httpRequest.getUrl();
        url = resolveParameter(url, systems, notFoundVariables, onlyEncrypted);
        httpRequest.setUrl(url);
        List<HttpParamSaveRequest> requestParameters = httpRequest.getRequestParams();
        if (!isEmpty(requestParameters)) {
            requestParameters.forEach(parameter -> {
                final String key = parameter.getKey();
                final String value = parameter.getValue();

                try {
                    String updatedParameterValue = resolveParameter(value, systems, notFoundVariables, onlyEncrypted);
                    parameter.setValue(updatedParameterValue);
                } catch (AtpDecryptException e) {
                    log.error("Failed to decrypt request parameter '{}' with value: '{}'", key, value, e);
                    throw new ItfLiteRequestParamDecryptException(key, value);
                }
            });
        }

        List<HttpHeaderSaveRequest> requestHeaders = httpRequest.getRequestHeaders();
        if (!isEmpty(requestHeaders)) {
            requestHeaders.forEach(header -> {
                final String key = header.getKey();
                final String value = header.getValue();

                try {
                    String updatedHeaderValue = resolveParameter(value, systems, notFoundVariables, onlyEncrypted);
                    header.setValue(updatedHeaderValue);
                } catch (AtpDecryptException e) {
                    log.error("Failed to decrypt request parameter '{}' with value: '{}'", key, value, e);
                    throw new ItfLiteRequestHeaderDecryptException(key, value);
                }
            });
        }
        RequestBody requestBody = httpRequest.getBody();
        if (requestBody != null) {
            String updatedContent = resolveParameter(
                    requestBody.getContent(), systems, notFoundVariables, onlyEncrypted);
            requestBody.setContent(updatedContent);
        }

        // TODO: make sure that it's ok to show only unencrypted not found variables
        checkNotFoundVariables(notFoundVariables);
    }

    public List<System> getEnvironmentSystemsById(UUID environmentId) {
        return environmentFeignService.getEnvironmentSystems(environmentId);
    }

    /**
     * Check if environment variables present in specified parameters.
     *
     * @param params parameters
     * @return check result
     */
    public boolean isEnvironmentVariablesPresent(Collection<String> params) {
        return params.stream()
                .anyMatch(field -> {
                    Matcher envMatcher = envVariablesSearchPattern.matcher(field);
                    return envMatcher.find();
                });
    }

    /**
     * Checks if http request contains at least one environment variable.
     *
     * @param httpRequest http request
     * @return true if http request contains at least one environment variable
     */
    public boolean isEnvironmentVariablesPresent(HttpRequestEntitySaveRequest httpRequest) {
        List<String> requestFields = prepareRequestFields(httpRequest);
        return requestFields.stream().noneMatch(field -> {
            Matcher envMatcher = envVariablesSearchPattern.matcher(field);
            return envMatcher.find();
        });
    }

    /**
     * Checks if http request contains at least one encrypted environment variable.
     *
     * @param httpRequest http request
     * @return true if http request contains at least one environment variable
     */
    public boolean isEncryptedEnvironmentVariablesPresent(HttpRequestEntitySaveRequest httpRequest) {
        List<String> requestFields = prepareRequestFields(httpRequest);
        return requestFields.stream().noneMatch(field -> {
            Matcher envMatcher = envEncryptedVariablesSearchPattern.matcher(field);
            return envMatcher.find();
        });
    }

    private List<String> prepareRequestFields(HttpRequestEntitySaveRequest httpRequest) {
        List<String> requestFields = new ArrayList<>();
        if (!StringUtils.isEmpty(httpRequest.getUrl())) {
            requestFields.add(httpRequest.getUrl());
        }
        List<HttpParamSaveRequest> requestParameters = httpRequest.getRequestParams();
        if (!isEmpty(requestParameters)) {
            requestParameters.forEach(parameter -> requestFields.add(parameter.getValue()));
        }
        List<HttpHeaderSaveRequest> requestHeaders = httpRequest.getRequestHeaders();
        if (!isEmpty(requestHeaders)) {
            requestHeaders.forEach(header -> requestFields.add(header.getValue()));
        }
        RequestBody requestBody = httpRequest.getBody();
        if (requestBody != null && Objects.nonNull(requestBody.getContent())) {
            requestFields.add(requestBody.getContent());
        }
        return requestFields;
    }

    /**
     * Resolve environment parameters values for string.
     *
     * @param paramValue        string
     * @param systems           environment systems with connections
     * @param notFoundVariables list for not found variables from environments
     */
    private String resolveParameter(String paramValue, List<System> systems, List<String> notFoundVariables,
                                    boolean onlyEncrypted) throws AtpDecryptException {
        Matcher envMatcher;
        if (onlyEncrypted) {
            envMatcher = envEncryptedVariablesSearchPattern.matcher(paramValue);
        } else {
            envMatcher = envVariablesSearchPattern.matcher(paramValue);
        }

        String stringWithValues = paramValue;
        while (envMatcher.find()) {
            String variableFullName = envMatcher.group(envVariableWithoutSpecialChars);
            log.debug("Found environment variable {}", variableFullName);
            String varEnvironmentValue = getEnvironmentValueByVariableFullName(variableFullName, systems);
            if (varEnvironmentValue != null) {
                if (onlyEncrypted) {
                    varEnvironmentValue = encryptionService.decryptIfEncrypted(varEnvironmentValue);
                    stringWithValues = envMatcher.replaceFirst(varEnvironmentValue);
                } else {
                    if (!encryptionService.isEncrypted(varEnvironmentValue)) {
                        stringWithValues = envMatcher.replaceFirst(varEnvironmentValue);
                    } else {
                        stringWithValues = envMatcher.replaceFirst(
                                "{" + encryptedEnvParameterPrefix + "." + variableFullName + "}");
                    }
                }
            } else {
                notFoundVariables.add(variableFullName);
                // replace variable by placeholder to exclude while-block loop if variable value is not found
                stringWithValues = envMatcher.replaceFirst("{valueForEnvironmentVariableNotFound}");
            }
            envMatcher.reset(stringWithValues);
        }
        log.debug("Environment variables resolve is finished");
        return stringWithValues;
    }

    private void resolveParameter(Producer<String> getter, Consumer<String> setter,
                                  List<System> systems, List<String> notFoundVariables) throws AtpDecryptException {
        String paramValue = getter.produce();
        if (nonNull(paramValue)) {
            paramValue = resolveParameter(paramValue, systems, notFoundVariables, false);
            paramValue = resolveParameter(paramValue, systems, notFoundVariables, true);
            setter.accept(paramValue);
        }
    }

    /**
     * Throws exception if environment variables not found.
     *
     * @param notFoundVariables list of not found environment variables
     */
    private void checkNotFoundVariables(List<String> notFoundVariables) {
        if (!isEmpty(notFoundVariables)) {
            StringJoiner stringJoiner = new StringJoiner("\n");
            notFoundVariables.forEach(stringJoiner::add);
            String variables = stringJoiner.toString();
            log.error("The environment variables aren't found: {}", variables);
            throw new ItfLiteRequestEnvironmentVariableNotFoundException(variables);
        }
    }

    /**
     * Get environment value by variable full name.
     *
     * @param variableFullName variable full name string
     * @return value
     */
    private String getEnvironmentValueByVariableFullName(String variableFullName, List<System> environmentSystems) {
        // lower case for case-insensitive comparison
        String tempVariable = String.copyValueOf(variableFullName.toCharArray()).toLowerCase();
        String systemName = getVariablePart(tempVariable);
        List<System> filteredSystems =
                environmentSystems.stream()
                        .filter(system -> system.getName().equalsIgnoreCase(systemName))
                        .collect(Collectors.toList());
        if (!filteredSystems.isEmpty()) {
            log.debug("Filtered systems: {}", filteredSystems);
            tempVariable = getVariableWithoutStartPart(tempVariable, systemName);
            String connectionName = getVariablePart(tempVariable);
            System system = filteredSystems.get(0);
            log.debug("Filter connections for system: {}", system.getId());
            List<Connection> filteredConnections =
                    system.getConnections().stream()
                            .filter(connection -> connection.getName().equalsIgnoreCase(connectionName))
                            .collect(Collectors.toList());
            if (!filteredConnections.isEmpty()) {
                log.debug("Filtered connections: {}", filteredConnections);
                // remove any dots and quotes
                tempVariable = StringUtils.strip(getVariableWithoutStartPart(tempVariable, connectionName),
                        Constants.DOT_CHARACTER + Constants.DOUBLE_QUOTE_CHARACTER);
                if (filteredConnections.get(0).getParameters().containsKey(tempVariable)) {
                    String value = (String) filteredConnections.get(0).getParameters().get(tempVariable);
                    log.debug("Found variable value: {} = {}", variableFullName, value);
                    return value;
                }
            }
        }
        log.error("Environment variable {} not found", variableFullName);
        return null;
    }

    private String getVariablePart(String variable) {
        if (variable.startsWith(Constants.DOUBLE_QUOTE_CHARACTER)) {
            return StringUtils.substringBetween(variable,
                    Constants.DOUBLE_QUOTE_CHARACTER, Constants.DOUBLE_QUOTE_CHARACTER);
        }
        return StringUtils.substringBefore(variable, Constants.DOT_CHARACTER);
    }

    private String getVariableWithoutStartPart(String variable, String part) {
        if (variable.startsWith(Constants.DOUBLE_QUOTE_CHARACTER)) {
            return StringUtils.stripStart(variable.replaceFirst(
                    Constants.DOUBLE_QUOTE_CHARACTER + part + Constants.DOUBLE_QUOTE_CHARACTER,
                    Constants.EMPTY_STRING),
                    Constants.DOT_CHARACTER);
        }
        return StringUtils.stripStart(variable.replaceFirst(part, Constants.EMPTY_STRING), Constants.DOT_CHARACTER);
    }

    /**
     * Encode parameter special characters except environments.
     *
     * @param parameter request parameter
     * @return encoded parameter
     * @throws UnsupportedEncodingException possible encoding exception
     */
    public String encodeParameterExceptEnv(String parameter) throws UnsupportedEncodingException {
        String encodedParameter = URLEncoder.encode(parameter, StandardCharsets.UTF_8.name());
        Matcher encodedEnvMatcher = encodedEnvVariableSearchPattern.matcher(encodedParameter);
        while (encodedEnvMatcher.find()) {
            String name = encodedEnvMatcher.group(envName);
            encodedParameter = encodedEnvMatcher.replaceFirst("\\$\\{" + name + "\\}");
            encodedEnvMatcher.reset(encodedParameter);
        }
        log.debug("parameter encoded {} -> {}", parameter, encodedParameter);
        return encodedParameter;
    }
}
