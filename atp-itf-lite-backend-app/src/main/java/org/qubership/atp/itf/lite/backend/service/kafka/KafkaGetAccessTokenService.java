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

package org.qubership.atp.itf.lite.backend.service.kafka;

import static org.qubership.atp.auth.springbootstarter.utils.ExceptionUtils.throwWithLog;
import static org.qubership.atp.itf.lite.backend.components.auth.OAuth2RequestAuthorizationStrategy.CLIENT_ID;
import static org.qubership.atp.itf.lite.backend.components.auth.OAuth2RequestAuthorizationStrategy.CLIENT_SECRET;
import static org.qubership.atp.itf.lite.backend.components.auth.OAuth2RequestAuthorizationStrategy.GRANT_TYPE;
import static org.qubership.atp.itf.lite.backend.configuration.KafkaConfiguration.GET_ACCESS_TOKEN_KAFKA_TEMPLATE_BEAN_NAME;

import java.util.Optional;
import java.util.UUID;

import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.itf.lite.backend.components.auth.OAuth2RequestAuthorizationStrategy;
import org.qubership.atp.itf.lite.backend.configuration.KafkaConfiguration;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.GetAuthorizationCodeRepository;
import org.qubership.atp.itf.lite.backend.exceptions.auth.AuthActionGetTokenByCodeException;
import org.qubership.atp.itf.lite.backend.exceptions.auth.AuthActionInvalidSseException;
import org.qubership.atp.itf.lite.backend.exceptions.auth.AuthActionInvalidStateException;
import org.qubership.atp.itf.lite.backend.mdc.ItfLiteMdcField;
import org.qubership.atp.itf.lite.backend.model.api.kafka.GetAccessTokenFinish;
import org.qubership.atp.itf.lite.backend.model.api.response.auth.OAuth2AuthrizationResponse;
import org.qubership.atp.itf.lite.backend.model.api.sse.GetAccessTokenData;
import org.qubership.atp.itf.lite.backend.model.entities.auth.GetAuthorizationCode;
import org.qubership.atp.itf.lite.backend.service.EncryptionService;
import org.qubership.atp.itf.lite.backend.service.SseEmitterService;
import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaGetAccessTokenService {

    private static final String KAFKA_ITF_LITE_GET_ACCESS_TOKEN_LISTENER_ID =
            "kafkaItfLiteGetAccessTokenListenerId";

    @Qualifier(GET_ACCESS_TOKEN_KAFKA_TEMPLATE_BEAN_NAME)
    private final KafkaTemplate<UUID, GetAccessTokenFinish> kafkaTemplate;
    private final SseEmitterService sseEmitterService;
    private final GetAuthorizationCodeRepository getAuthorizationCodeRepository;
    private final EncryptionService encryptionService;
    private final OAuth2RequestAuthorizationStrategy oauth2Strategy;

    @Value("${kafka.itflite.getaccesstoken.finish.topic}")
    public String finishTopic;

    /**
     * Sends execution finish event to kafka.
     *
     * @param sseId finished sseId
     */
    public void getAccessTokenFinishEventSend(UUID sseId) {
        log.info("Send 'get access token' finish event for sseId #{}", sseId);
        kafkaTemplate.send(finishTopic, sseId, new GetAccessTokenFinish(sseId));
    }

    /**
     * Listen start execution kafka topic.
     */
    @KafkaListener(groupId = KAFKA_ITF_LITE_GET_ACCESS_TOKEN_LISTENER_ID
            + "_#{T(org.qubership.atp.itf.lite.backend.utils.PodNameUtils).getServicePodName()}",
            topics = "${kafka.itflite.getaccesstoken.finish.topic}",
            containerFactory = KafkaConfiguration.GET_ACCESS_TOKEN_KAFKA_CONTAINER_FACTORY_BEAN_NAME
    )
    public void listenItfLiteGetAccessTokenFinishEvent(@Payload GetAccessTokenFinish getAccessTokenFinish) {
        UUID sseId = getAccessTokenFinish.getSseId();
        MDC.clear();
        MdcUtils.put(ItfLiteMdcField.SSE_ID.toString(), sseId);
        log.debug("Start 'get access token' processing by event from kafka #{}", sseId);
        // check if current itf-lite sseEmitters map has sseEmitter with key = sseId
        SseEmitter sseEmitter = sseEmitterService.getEmitter(sseId);
        if (sseEmitter == null) {
            log.debug(Constants.SSE_EMITTER_WITH_SSE_ID_NOT_FOUND, sseId);
            return;
        }
        GetAuthorizationCode getAuthorizationCode = null;
        try {
            try {
                log.info("Start SSE ({}) processing by event from kafka", sseId);
                Optional<GetAuthorizationCode> getAuthorizationCodeOpt = getAuthorizationCodeRepository.findById(sseId);
                if (!getAuthorizationCodeOpt.isPresent()) {
                    throwWithLog(log, new AuthActionInvalidSseException(sseId));
                }
                getAuthorizationCode = getAuthorizationCodeOpt.get();
                if (getAuthorizationCode.getState() != null
                        && !getAuthorizationCode.getState().equals(getAuthorizationCode.getResponseState())) {
                    throwWithLog(log, new AuthActionInvalidStateException(getAuthorizationCode.getState()));
                }
                GetAccessTokenData tokenResponse = new GetAccessTokenData(getAuthorizationToken(getAuthorizationCode));
                log.info("Received token ({}) from server. Send it into SSE", tokenResponse);
                sseEmitterService.sendGetAccessTokenResult(sseId, sseEmitter, tokenResponse);
            } catch (Exception e) {
                log.error("Failed processing by event from kafka", e);
                sseEmitterService.sendGetAccessTokenResult(sseId, sseEmitter,
                        new GetAccessTokenData(e));
            }
        } finally {
            if (getAuthorizationCode != null) {
                getAuthorizationCodeRepository.delete(getAuthorizationCode);
            }
        }
    }

    private OAuth2AuthrizationResponse getAuthorizationToken(GetAuthorizationCode getAuthorizationCode) {
        log.info("Get authorization token  by code for sseID {}", getAuthorizationCode.getSseId());
        try {
            final MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add(GRANT_TYPE, AuthorizationGrantType.AUTHORIZATION_CODE.getValue());
            params.add("code", encryptionService.decryptIfEncrypted(getAuthorizationCode.getAuthorizationCode()));
            params.add(CLIENT_ID, getAuthorizationCode.getClientId());
            params.add(CLIENT_SECRET, encryptionService.decryptIfEncrypted(getAuthorizationCode.getClientSecret()));
            params.add("response_type", "token");
            params.add("redirect_uri", getAuthorizationCode.getRedirectUri());
            return oauth2Strategy.performAuthorization(
                            getAuthorizationCode.getProjectId(), getAuthorizationCode.getAccessTokenUrl(), params);
        } catch (Exception e) {
            throwWithLog(log, new AuthActionGetTokenByCodeException(e.getMessage()));
            return null;
        }
    }
}
