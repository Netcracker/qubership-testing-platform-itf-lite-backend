package org.qubership.atp.itf.lite.backend.service;

import static com.google.common.collect.ImmutableMap.of;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRandomHttpRequestEntitySaveRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRandomOAuth2AuthorizationSaveRequest;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.camel.util.CaseInsensitiveMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteRequestEnvironmentVariableNotFoundException;
import org.qubership.atp.itf.lite.backend.feign.service.EnvironmentFeignService;
import org.qubership.atp.itf.lite.backend.model.api.request.HttpRequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.OAuth2AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpHeaderSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpParamSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.environments.Connection;
import org.qubership.atp.itf.lite.backend.model.api.response.environments.System;

@ExtendWith(MockitoExtension.class)
public class EnvironmentVariableServiceTest {

    private final ThreadLocal<EncryptionService> encryptionService = new ThreadLocal<>();
    private final ThreadLocal<EnvironmentFeignService> environmentFeignService = new ThreadLocal<>();
    private final ThreadLocal<EnvironmentVariableService> environmentVariableService = new ThreadLocal<>();

    @BeforeEach
    public void setUp() {
        EncryptionService encryptionServiceMock = mock(EncryptionService.class);
        EnvironmentFeignService environmentFeignServiceMock = mock(EnvironmentFeignService.class);
        encryptionService.set(encryptionServiceMock);
        environmentFeignService.set(environmentFeignServiceMock);
        environmentVariableService.set(new EnvironmentVariableService(environmentFeignServiceMock, encryptionServiceMock));
    }

    @Test
    public void resolveEnvironmentParametersTest_OAuth2AuthorizationSaveRequestConfiguredWithoutEnvironmentVariables_resolvedSuccessfully()
            throws AtpDecryptException {
        // given
        OAuth2AuthorizationSaveRequest oAuth2AuthorizationSaveRequest = generateRandomOAuth2AuthorizationSaveRequest();
        // when
        environmentVariableService.get().resolveEnvironmentParameters(oAuth2AuthorizationSaveRequest, UUID.randomUUID());
        // then
        verify(environmentFeignService.get(), times(0)).getEnvironmentSystems(any());
    }

    @Test
    public void resolveEnvironmentParametersTest_NotAllEnvironmentVariablesFound_resolvedSuccessfully() throws AtpDecryptException {
        // given
        OAuth2AuthorizationSaveRequest oAuth2AuthorizationSaveRequest = mock(OAuth2AuthorizationSaveRequest.class);

        // url with environment variable
        String myUrl1 = "my.url.1";
        String myUrl2 = "my.url.2";
        String url = "http://test.test/";
        // username with environment variable
        String environmentVariableLogin = "QA.http.login";
        // password with environment variable
        String password = "password";

        String expectedUrlPart1 = "part1";
        String expectedUrlPart2 = "part2";
        String expectedPassword = "myPassword";
        Connection connection = new Connection(randomUUID(), "HTTP",
                new CaseInsensitiveMap(of(
                        myUrl1, expectedUrlPart1,
                        myUrl2, expectedUrlPart2,
                        password, expectedPassword)));

        List<System> systems = singletonList(new System(randomUUID(), "QA", connection));
        UUID environmentId = randomUUID();

        // when
        when(oAuth2AuthorizationSaveRequest.getUrl()).thenReturn(
                url + "${ENV.QA.http.\"" + myUrl1 + "\"}/${ENV.QA.http.\"" + myUrl2 + "\"}");
        when(oAuth2AuthorizationSaveRequest.getUsername()).thenReturn("${ENV." + environmentVariableLogin + "}");
        when(oAuth2AuthorizationSaveRequest.getPassword()).thenReturn("${ENV.QA.http." + password + "}");
        when(environmentFeignService.get().getEnvironmentSystems(any())).thenReturn(systems);
        when(encryptionService.get().isEncrypted(any())).thenReturn(false).thenReturn(false).thenReturn(true);
        when(encryptionService.get().decryptIfEncrypted(any())).thenAnswer(answer -> answer.getArgument(0));
        ItfLiteRequestEnvironmentVariableNotFoundException exception = assertThrows(
                ItfLiteRequestEnvironmentVariableNotFoundException.class,
                () -> environmentVariableService.get().resolveEnvironmentParameters(
                        oAuth2AuthorizationSaveRequest, environmentId));
        // then
        ArgumentCaptor<String> actualUrlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> actualPasswordCaptor = ArgumentCaptor.forClass(String.class);
        verify(oAuth2AuthorizationSaveRequest, times(1)).setUrl(actualUrlCaptor.capture());
        verify(oAuth2AuthorizationSaveRequest, times(1)).setPassword(actualPasswordCaptor.capture());
        assertEquals(url + expectedUrlPart1 + "/" + expectedUrlPart2, actualUrlCaptor.getValue());
        assertEquals(expectedPassword, actualPasswordCaptor.getValue());

        String expectedErrorMessage =
                format(ItfLiteRequestEnvironmentVariableNotFoundException.DEFAULT_MESSAGE, environmentVariableLogin);
        assertEquals(expectedErrorMessage, exception.getMessage());
    }

    @Test
    public void encodeParameterExceptEnv() throws UnsupportedEncodingException {
        String parameterValue = "[{\"${ENV.SYSTEM.CONN.key}\": ${ENV.SYSTEM.CONN.value}}]";
        String encodedParameterValue = environmentVariableService.get().encodeParameterExceptEnv(parameterValue);
        assertEquals("%5B%7B%22${ENV.SYSTEM.CONN.key}%22%3A+${ENV.SYSTEM.CONN.value}%7D%5D", encodedParameterValue);
    }

    @Test
    public void resolveEnvironmentParametersTest_httpRequestSpecified_allVariablesReplaced()
            throws AtpDecryptException {
        // given
        HttpRequestEntitySaveRequest request = generateRandomHttpRequestEntitySaveRequest();
        request.setUrl("${ENV.QA.http.url}");
        request.setRequestHeaders(
                Collections.singletonList(new HttpHeaderSaveRequest("header_1", "${ENV.QA.http.headerValue}", "")));
        request.setRequestParams(
                Collections.singletonList(new HttpParamSaveRequest("query_1", "${ENV.QA.http.queryValue}", "")));
        UUID environmentId = randomUUID();

        String expectedUrl = "http://localhost:8080";
        String expectedHeader = "header_value";
        String expectedQuery = "query_value";

        Connection connection = new Connection(randomUUID(), "http",
                new CaseInsensitiveMap(of(
                        "url", expectedUrl,
                        "headerValue", expectedHeader,
                        "queryValue", expectedQuery)));

        List<System> systems = singletonList(new System(randomUUID(), "QA", connection));

        // when
        when(environmentFeignService.get().getEnvironmentSystems(any())).thenReturn(systems);
        when(encryptionService.get().isEncrypted(any())).thenReturn(false);
        environmentVariableService.get().resolveEnvironmentParameters(request, false, environmentId);

        // then
        assertEquals(expectedUrl, request.getUrl());
        assertEquals(1, request.getRequestHeaders().size());
        assertEquals(expectedHeader, request.getRequestHeaders().get(0).getValue());
        assertEquals(1, request.getRequestParams().size());
        assertEquals(expectedQuery, request.getRequestParams().get(0).getValue());
    }
}