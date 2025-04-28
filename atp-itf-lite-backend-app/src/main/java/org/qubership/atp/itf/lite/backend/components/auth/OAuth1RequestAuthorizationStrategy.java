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

package org.qubership.atp.itf.lite.backend.components.auth;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType.OAUTH1;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.modelmapper.ModelMapper;
import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.itf.lite.backend.enums.auth.OAuth1AddDataType;
import org.qubership.atp.itf.lite.backend.enums.auth.OAuth1SignatureMethod;
import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.qubership.atp.itf.lite.backend.model.AuthorizationStrategyRequest;
import org.qubership.atp.itf.lite.backend.model.AuthorizationStrategyResponse;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.AuthorizationResolvingContext;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.OAuth1AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.auth.OAuth2AuthrizationResponse;
import org.qubership.atp.itf.lite.backend.model.entities.auth.OAuth1RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestParam;
import org.qubership.atp.itf.lite.backend.service.EncryptionService;
import org.qubership.atp.itf.lite.backend.utils.AuthorizationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OAuth1RequestAuthorizationStrategy extends AbstractAuthorizationStrategy
        implements RequestAuthorizationStrategy {

    private static final String OAUTH_VERSION = "1.0";
    private static final String CALCULATED_VALUE = "<calculated when request is sent>";
    private static final String MASKED_VALUE = "******";

    private ModelMapper modelMapper;

    public OAuth1RequestAuthorizationStrategy(EncryptionService encryptionService,
                                              ModelMapper modelMapper) {
        super(encryptionService);
        this.modelMapper = modelMapper;
    }

    @Override
    public AuthorizationStrategyResponse getAuthorizationToken(AuthorizationStrategyRequest request)
            throws AtpDecryptException {
        final OAuth1AuthorizationSaveRequest authorization =
                (OAuth1AuthorizationSaveRequest) request.getUnsafeAuthorizationRequest();

        AuthorizationResolvingContext authResolvingContext = request.getAuthResolvingContext();
        final Map<String, String> oauthParams = getOauthParams(authorization, authResolvingContext);

        final OAuth1AddDataType addDataType = authorization.getAddDataType();
        switch (addDataType) {
            case REQUEST_HEADERS:
                final String authHeader = generateAuthorizationHeader(oauthParams);
                return new AuthorizationStrategyResponse(authHeader, authHeader);
            case REQUEST_URL:
                return new AuthorizationStrategyResponse(oauthParams);
            default:
                throw new IllegalArgumentException("Unsupported add data type: " + addDataType);
        }
    }

    private Map<String, String> getOauthParams(OAuth1AuthorizationSaveRequest authorization,
                                               AuthorizationResolvingContext authResolvingContext) {
        decryptParameters(authorization);

        String url = authorization.getUrl();
        String httpMethod = authorization.getHttpMethod();

        // if comes from Inherit From Parent auth type
        if (isNull(url) && isNull(httpMethod)) {
            url = authResolvingContext.getUrl();
            httpMethod = authResolvingContext.getHttpMethod().toString();
        }

        final OAuth1SignatureMethod signatureMethod = authorization.getSignatureMethod();

        final String consumerKey = authorization.getConsumerKey();
        final String consumerSecret = authorization.getConsumerSecret();
        final String token = authorization.getAccessToken();
        final String tokenSecret = authorization.getTokenSecret();

        final Map<String, String> oauthParams = new TreeMap<>();
        oauthParams.put("oauth_consumer_key", consumerKey);
        oauthParams.put("oauth_nonce", getNonce());
        oauthParams.put("oauth_signature_method", signatureMethod.getKey());
        oauthParams.put("oauth_timestamp", getTimestamp());
        oauthParams.put("oauth_version", OAUTH_VERSION);

        if (nonNull(token) && nonNull(tokenSecret)) {
            oauthParams.put("oauth_token", token);
        }

        final String baseString = getSignatureBaseString(oauthParams, url, httpMethod);
        final String signingKey = percentEncode(consumerSecret) + "&"
                + (tokenSecret != null ? percentEncode(tokenSecret) : "");
        final String signature = generateSignature(baseString, signingKey, signatureMethod.getKey());

        oauthParams.put("oauth_signature", signature);

        return oauthParams;
    }

    private String getSignatureBaseString(Map<String, String> oauthParams, String url, String httpMethod) {
        StringBuilder baseString = new StringBuilder();
        baseString.append(httpMethod).append("&");
        baseString.append(percentEncode(url)).append("&");

        StringBuilder paramString = new StringBuilder();
        for (Map.Entry<String, String> entry : oauthParams.entrySet()) {
            paramString.append(percentEncode(entry.getKey())).append("=")
                    .append(percentEncode(entry.getValue())).append("&");
        }

        paramString.deleteCharAt(paramString.length() - 1);

        baseString.append(percentEncode(paramString.toString()));
        return baseString.toString();
    }

    private String generateSignature(String data, String key, String algorithm) {
        try {
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm);
            Mac mac = Mac.getInstance(algorithm);
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Failed to generate HMAC", e);
        }
    }

    private String percentEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
                    .replace("+", "%20")
                    .replace("*", "%2A")
                    .replace("%7E", "~");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private String getNonce() {
        return Long.toString(System.currentTimeMillis());
    }

    private String getTimestamp() {
        return Long.toString(System.currentTimeMillis() / 1000L);
    }

    @Override
    public void encryptParameters(AuthorizationSaveRequest requestAuthorization) {
        OAuth1AuthorizationSaveRequest authorization = (OAuth1AuthorizationSaveRequest) requestAuthorization;
        encryptParameter(authorization::getConsumerKey, authorization::setConsumerKey);
        encryptParameter(authorization::getConsumerSecret, authorization::setConsumerSecret);
        encryptParameter(authorization::getAccessToken, authorization::setAccessToken);
        encryptParameter(authorization::getTokenSecret, authorization::setTokenSecret);
    }

    @Override
    public void decryptParameters(AuthorizationSaveRequest requestAuthorization) {
        OAuth1AuthorizationSaveRequest authorization = (OAuth1AuthorizationSaveRequest) requestAuthorization;
        decryptParameter(authorization::getConsumerKey, authorization::setConsumerKey);
        decryptParameter(authorization::getConsumerSecret, authorization::setConsumerSecret);
        decryptParameter(authorization::getAccessToken, authorization::setAccessToken);
        decryptParameter(authorization::getTokenSecret, authorization::setTokenSecret);
    }

    @Override
    public OAuth2AuthrizationResponse performAuthorization(UUID projectId, String url,
                                                           MultiValueMap<String, String> map) {
        // OAuth 1.0a does not require this method
        return null;
    }

    @Override
    public RequestAuthorizationType getAuthorizationType() {
        return OAUTH1;
    }

    @Override
    public RequestAuthorization parseAuthorizationFromMap(Map<String, String> authorizationInfo) {
        OAuth1AuthorizationSaveRequest authorizationSaveRequest = new OAuth1AuthorizationSaveRequest();
        authorizationSaveRequest.setType(OAUTH1);

        final String signatureMethodKey = authorizationInfo.getOrDefault("signatureMethod", "");
        authorizationSaveRequest.setSignatureMethod(OAuth1SignatureMethod.fromKey(signatureMethodKey));
        authorizationSaveRequest.setConsumerKey(authorizationInfo.getOrDefault("consumerKey", ""));
        authorizationSaveRequest.setConsumerSecret(authorizationInfo.getOrDefault("consumerSecret", ""));
        authorizationSaveRequest.setAccessToken(authorizationInfo.getOrDefault("token", ""));
        authorizationSaveRequest.setTokenSecret(authorizationInfo.getOrDefault("tokenSecret", ""));

        encryptParameters(authorizationSaveRequest);

        return modelMapper.map(authorizationSaveRequest, OAuth1RequestAuthorization.class);
    }

    @Nullable
    @Override
    public RequestHeader generateAuthorizationHeader(RequestAuthorization authorization) {
        OAuth1AuthorizationSaveRequest oauthAuthorization =
                (OAuth1AuthorizationSaveRequest) AuthorizationUtils.castToAuthorizationSaveRequest(authorization);
        OAuth1AddDataType addDataType = oauthAuthorization.getAddDataType();

        if (addDataType == OAuth1AddDataType.REQUEST_HEADERS) {
            return new RequestHeader(null, "Authorization", CALCULATED_VALUE, "", false, true);
        }

        return null;
    }

    private String generateAuthorizationHeader(Map<String, String> oauthParams) {
        StringBuilder header = new StringBuilder("OAuth ");
        for (Map.Entry<String, String> entry : oauthParams.entrySet()) {
            header.append(entry.getKey()).append("=\"").append(percentEncode(entry.getValue())).append("\", ");
        }

        header.deleteCharAt(header.length() - 2);
        return header.toString();
    }

    @Override
    public List<RequestParam> generateAuthorizationParams(RequestAuthorization authorization) {
        OAuth1AuthorizationSaveRequest oauthAuthorization =
                (OAuth1AuthorizationSaveRequest) AuthorizationUtils.castToAuthorizationSaveRequest(authorization);
        OAuth1AddDataType addDataType = oauthAuthorization.getAddDataType();
        String accessToken = oauthAuthorization.getAccessToken();
        String tokenSecret = oauthAuthorization.getTokenSecret();

        List<String> baseParams = new ArrayList<>(Arrays.asList(
                "oauth_consumer_key",
                "oauth_nonce",
                "oauth_signature",
                "oauth_signature_method",
                "oauth_timestamp",
                "oauth_version"
        ));

        if (addDataType == OAuth1AddDataType.REQUEST_URL) {
            if (!StringUtils.isAllEmpty(accessToken, tokenSecret)) {
                baseParams.add("oauth_token");
            }

            return baseParams.stream()
                    .map(param -> new RequestParam(param, MASKED_VALUE, "", false, true))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}