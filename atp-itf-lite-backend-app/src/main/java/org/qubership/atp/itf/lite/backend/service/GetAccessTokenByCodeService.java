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

import static java.util.Objects.nonNull;
import static org.qubership.atp.auth.springbootstarter.utils.ExceptionUtils.throwWithLog;
import static org.qubership.atp.itf.lite.backend.components.auth.OAuth2RequestAuthorizationStrategy.TO_ENCRYPT_FLAG;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.qubership.atp.integration.configuration.mdc.MdcUtils;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.GetAuthorizationCodeRepository;
import org.qubership.atp.itf.lite.backend.exceptions.auth.AuthActionDuplicateSseException;
import org.qubership.atp.itf.lite.backend.exceptions.auth.AuthActionGetTokenByCodeException;
import org.qubership.atp.itf.lite.backend.exceptions.auth.AuthActionInvalidSseException;
import org.qubership.atp.itf.lite.backend.exceptions.auth.AuthActionMandatoryFieldException;
import org.qubership.atp.itf.lite.backend.feign.dto.GetAccessCodeParametersDto;
import org.qubership.atp.itf.lite.backend.mdc.ItfLiteMdcField;
import org.qubership.atp.itf.lite.backend.model.entities.auth.GetAuthorizationCode;
import org.qubership.atp.itf.lite.backend.service.kafka.KafkaGetAccessTokenService;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetAccessTokenByCodeService {

    private final GetAuthorizationCodeRepository getAuthorizationCodeRepository;
    private final Provider<UserInfo> userInfoProvider;
    private final EncryptionService encryptionService;
    private final KafkaGetAccessTokenService kafkaGetAccessTokenService;

    /**
     * Save authorizationCode.
     */
    public void saveCode(UUID sseId, String authorizationCode, String state) {
        if (Strings.isNullOrEmpty(authorizationCode)) {
            throwWithLog(log, new AuthActionMandatoryFieldException("'" + authorizationCode + "'"));
        }
        Optional<GetAuthorizationCode> getAuthorizationCodeOptional = getAuthorizationCodeRepository.findById(sseId);
        if (!getAuthorizationCodeOptional.isPresent()) {
            throwWithLog(log, new AuthActionInvalidSseException(sseId));
        }
        MdcUtils.put(ItfLiteMdcField.SSE_ID.toString(), sseId);
        GetAuthorizationCode getAuthorizationCode = getAuthorizationCodeOptional.get();
        try {
            getAuthorizationCode.setAuthorizationCode(encryptionService.encrypt(authorizationCode));
            getAuthorizationCode.setResponseState(state);
            getAuthorizationCodeRepository.save(getAuthorizationCode);
            kafkaGetAccessTokenService.getAccessTokenFinishEventSend(sseId);
        } catch (Exception e) {
            throwWithLog(log, new AuthActionGetTokenByCodeException(e.getMessage()));
        }
    }

    /**
     * Save parameters for get access token by authorization code.
     *
     * @param getAccessCodeParametersDto {@link GetAccessCodeParametersDto} instance
     */
    public void saveParamsForGetAccessCode(GetAccessCodeParametersDto getAccessCodeParametersDto) {
        //Check mandatory fields
        List<String> looseMandatoryFields = new ArrayList<>();
        if (getAccessCodeParametersDto.getSseId() == null) {
            looseMandatoryFields.add("sseId");
        }
        if (Strings.isNullOrEmpty(getAccessCodeParametersDto.getAccessTokenUrl())) {
            looseMandatoryFields.add("accessTokenUrl");
        }
        if (Strings.isNullOrEmpty(getAccessCodeParametersDto.getClientId())) {
            looseMandatoryFields.add("clientId");
        }
        if (Strings.isNullOrEmpty(getAccessCodeParametersDto.getClientSecret())) {
            looseMandatoryFields.add("clientSecret");
        }
        if (Strings.isNullOrEmpty(getAccessCodeParametersDto.getRedirectUri())) {
            looseMandatoryFields.add("redirectUri");
        }
        //Exception if any mandatory field is not present
        if (looseMandatoryFields.size() > 0) {
            throwWithLog(log, new AuthActionMandatoryFieldException(looseMandatoryFields.toString()));
        }
        //Check that no another SSE
        if (getAuthorizationCodeRepository.findById(getAccessCodeParametersDto.getSseId()).isPresent()) {
            throwWithLog(log, new AuthActionDuplicateSseException(getAccessCodeParametersDto.getSseId()));
        }
        try {
            MdcUtils.put(ItfLiteMdcField.SSE_ID.toString(), getAccessCodeParametersDto.getSseId());
            //Get ATP user from token
            String user = (userInfoProvider != null && userInfoProvider.get() != null)
                    ? userInfoProvider.get().getUsername() : "UserNotDefined";
            //Save to DB
            log.info("save authorization parameters into DB for sseID {}", getAccessCodeParametersDto.getSseId());
            String clientId = getAccessCodeParametersDto.getClientId();
            String clientSecret =  decodeParameter(getAccessCodeParametersDto.getClientSecret());
            getAuthorizationCodeRepository.save(new GetAuthorizationCode(
                    getAccessCodeParametersDto.getSseId(),
                    getAccessCodeParametersDto.getProjectId(),
                    Timestamp.from(Instant.now()),
                    getAccessCodeParametersDto.getAccessTokenUrl(),
                    clientId,
                    encryptionService.isEncrypted(clientSecret) ? clientSecret
                            : encryptionService.encrypt(clientSecret),
                    getAccessCodeParametersDto.getScope(),
                    getAccessCodeParametersDto.getState(),
                    getAccessCodeParametersDto.getRedirectUri(),
                    null,
                    user,
                    null,
                    null
            ));
        } catch (Exception e) {
            throwWithLog(log, new AuthActionGetTokenByCodeException(e.getMessage()));
        }
    }

    private String decodeParameter(String paramValue) {
        if (nonNull(paramValue) && paramValue.startsWith(TO_ENCRYPT_FLAG)) {
            paramValue = paramValue.replace(TO_ENCRYPT_FLAG, "");
            paramValue = encryptionService.decodeBase64(paramValue);
        }
        return paramValue;
    }
}
