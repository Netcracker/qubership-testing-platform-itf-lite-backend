package org.qubership.atp.itf.lite.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.qubership.atp.itf.lite.backend.components.auth.OAuth2RequestAuthorizationStrategy.TO_ENCRYPT_FLAG;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateEmptyGetAccessCodeParametersDto;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateGetAccessCodeParametersDto;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateGetAuthorizationCode;

import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.SerializationUtils;
import org.hibernate.HibernateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.qubership.atp.crypt.exception.AtpEncryptException;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.GetAuthorizationCodeRepository;
import org.qubership.atp.itf.lite.backend.exceptions.auth.AuthActionDuplicateSseException;
import org.qubership.atp.itf.lite.backend.exceptions.auth.AuthActionGetTokenByCodeException;
import org.qubership.atp.itf.lite.backend.exceptions.auth.AuthActionInvalidSseException;
import org.qubership.atp.itf.lite.backend.exceptions.auth.AuthActionMandatoryFieldException;
import org.qubership.atp.itf.lite.backend.feign.dto.GetAccessCodeParametersDto;
import org.qubership.atp.itf.lite.backend.model.entities.auth.GetAuthorizationCode;
import org.qubership.atp.itf.lite.backend.service.kafka.KafkaGetAccessTokenService;

class GetAccessTokenByCodeServiceTest {

    private GetAuthorizationCodeRepository getAuthorizationCodeRepository;
    private Provider<UserInfo> userInfoProvider;
    private EncryptionService encryptionService;
    private KafkaGetAccessTokenService kafkaGetAccessTokenService;
    private GetAccessTokenByCodeService getAccessTokenByCodeService;
    private static final String responseState = "state";
    private static final String authorizationCode = "authorizationCode";
    private static final String authorizationCodeEncoded = "EncodedAuthorizationCode";
    private static final String token = "token";

    @BeforeEach
    public void setUp() {
        getAuthorizationCodeRepository = mock(GetAuthorizationCodeRepository.class);
        userInfoProvider = mock(Provider.class);
        encryptionService = mock(EncryptionService.class);
        kafkaGetAccessTokenService = mock(KafkaGetAccessTokenService.class);
        getAccessTokenByCodeService = new GetAccessTokenByCodeService(getAuthorizationCodeRepository, userInfoProvider,
                encryptionService, kafkaGetAccessTokenService);
    }

    @Test
    void saveCode_whenAuthorizationCodeIsNull_thenException() {
        final UUID sseId = UUID.randomUUID();
        assertThrows(AuthActionMandatoryFieldException.class, () -> getAccessTokenByCodeService.saveCode(sseId, null,
                responseState));
    }

    @Test
    void saveCode_whenSseIdNotFoundInDb_thenException() {
        final UUID sseId = UUID.randomUUID();
        //mock
        when(getAuthorizationCodeRepository.findById(sseId)).thenReturn(Optional.empty());
        //check
        assertThrows(AuthActionInvalidSseException.class, () -> getAccessTokenByCodeService.saveCode(sseId,
                authorizationCode, responseState));
    }

    @Test
    void saveCode_whenAtpEncryptException_thenException() throws AtpEncryptException {
        final UUID sseId = UUID.randomUUID();
        //mock
        when(getAuthorizationCodeRepository.findById(sseId)).thenReturn(generateGetAuthorizationCode(sseId,
                authorizationCodeEncoded));
        when(encryptionService.isEncrypted(authorizationCode)).thenReturn(false);
        when(encryptionService.encrypt(authorizationCode)).thenThrow(new AtpEncryptException(""));
        //check
        assertThrows(AuthActionGetTokenByCodeException.class, () -> getAccessTokenByCodeService.saveCode(sseId,
                authorizationCode, responseState));
    }

    @Test
    void saveCode_whenSaveToDbException_thenException() throws AtpEncryptException {
        final UUID sseId = UUID.randomUUID();
        //mock
        Optional<GetAuthorizationCode> getAuthorizationCodeOptional = generateGetAuthorizationCode(sseId, authorizationCodeEncoded);
        when(getAuthorizationCodeRepository.findById(sseId)).thenReturn(getAuthorizationCodeOptional);
        when(encryptionService.isEncrypted(authorizationCode)).thenReturn(false);
        when(encryptionService.encrypt(authorizationCode)).thenReturn(authorizationCodeEncoded);
        GetAuthorizationCode getAuthorizationCode = getAuthorizationCodeOptional.get();
        getAuthorizationCode.setAuthorizationCode(authorizationCodeEncoded);
        when(getAuthorizationCodeRepository.save(getAuthorizationCode)).thenThrow(new HibernateException(""));
        //check
        assertThrows(AuthActionGetTokenByCodeException.class, () -> getAccessTokenByCodeService.saveCode(sseId,
                authorizationCode, responseState));
    }

    @Test
    void saveCode() throws AtpEncryptException {
        final UUID sseId = UUID.randomUUID();
        //mock
        Optional<GetAuthorizationCode> getAuthorizationCodeOptional = generateGetAuthorizationCode(sseId, authorizationCodeEncoded);
        when(getAuthorizationCodeRepository.findById(sseId)).thenReturn(getAuthorizationCodeOptional);
        when(encryptionService.isEncrypted(authorizationCode)).thenReturn(false);
        when(encryptionService.encrypt(authorizationCode)).thenReturn(authorizationCodeEncoded);
        GetAuthorizationCode getAuthorizationCodeClone1 = SerializationUtils.clone(getAuthorizationCodeOptional.get());
        getAuthorizationCodeClone1.setAuthorizationCode(authorizationCodeEncoded);
        when(getAuthorizationCodeRepository.save(eq(getAuthorizationCodeOptional.get()))).thenReturn(getAuthorizationCodeClone1);
        GetAuthorizationCode getAuthorizationCodeClone2 = SerializationUtils.clone(getAuthorizationCodeClone1);
        getAuthorizationCodeClone2.setToken(token);
        when(getAuthorizationCodeRepository.save(eq(getAuthorizationCodeClone2))).thenReturn(getAuthorizationCodeClone2);
        //action
        getAccessTokenByCodeService.saveCode(sseId, authorizationCode, responseState);
        //check
        verify(kafkaGetAccessTokenService, times(1)).getAccessTokenFinishEventSend(eq(sseId));
    }

    @Test
    void saveParamsForGetAccessCode_whenNotAllMandatoryParameters_thenException() {
        assertThrows(AuthActionMandatoryFieldException.class, () ->
                getAccessTokenByCodeService.saveParamsForGetAccessCode(generateEmptyGetAccessCodeParametersDto()));
    }

    @Test
    void saveParamsForGetAccessCode_whenSseIdAlreadyPresent_thenException() {
        final UUID sseId = UUID.randomUUID();
        //mock
        when(getAuthorizationCodeRepository.findById(sseId)).thenReturn(generateGetAuthorizationCode(sseId, authorizationCodeEncoded));
        //check
        assertThrows(AuthActionDuplicateSseException.class, () ->
                getAccessTokenByCodeService.saveParamsForGetAccessCode(generateGetAccessCodeParametersDto(sseId)));
    }

    @Test
    void saveParamsForGetAccessCode_whenSaveException_thenException() {
        final UUID sseId = UUID.randomUUID();
        //mock
        when(getAuthorizationCodeRepository.findById(sseId)).thenReturn(Optional.empty());
        when(getAuthorizationCodeRepository.save(any())).thenThrow(new HibernateException(""));
        //check
        assertThrows(AuthActionGetTokenByCodeException.class, () ->
                getAccessTokenByCodeService.saveParamsForGetAccessCode(generateGetAccessCodeParametersDto(sseId)));
    }

    @Test
    void saveParamsForGetAccessCode_whenUserNotDefinedAndEncrypted() {
        final UUID sseId = UUID.randomUUID();
        //mock
        GetAccessCodeParametersDto getAccessCodeParametersDto =  generateGetAccessCodeParametersDto(sseId);
        when(getAuthorizationCodeRepository.findById(sseId)).thenReturn(Optional.empty());
        when(userInfoProvider.get()).thenReturn(null);
        when(encryptionService.isEncrypted(getAccessCodeParametersDto.getClientSecret())).thenReturn(true);
        ArgumentCaptor<GetAuthorizationCode> getAuthorizationCodeCapture = ArgumentCaptor.forClass(GetAuthorizationCode.class);
        //action
        getAccessTokenByCodeService.saveParamsForGetAccessCode(getAccessCodeParametersDto);
        //check
        verify(getAuthorizationCodeRepository, times(1)).save(getAuthorizationCodeCapture.capture());
        GetAuthorizationCode getAuthorizationCode = getAuthorizationCodeCapture.getValue();
        assertEquals(getAccessCodeParametersDto.getProjectId(), getAuthorizationCode.getProjectId());
        assertEquals(getAccessCodeParametersDto.getSseId(), getAuthorizationCode.getSseId());
        assertNotNull(getAuthorizationCode.getStartedAt());
        assertEquals(getAccessCodeParametersDto.getClientId(), getAuthorizationCode.getClientId());
        assertEquals(getAccessCodeParametersDto.getClientSecret(), getAuthorizationCode.getClientSecret());
        assertEquals(getAccessCodeParametersDto.getScope(), getAuthorizationCode.getScope());
        assertEquals(getAccessCodeParametersDto.getState(), getAuthorizationCode.getState());
        assertNull(getAuthorizationCode.getAuthorizationCode());
        assertNull(getAuthorizationCode.getToken());
    }

    @Test
    void saveParamsForGetAccessCode() throws AtpEncryptException {
        final UUID sseId = UUID.randomUUID();
        //mock
        GetAccessCodeParametersDto getAccessCodeParametersDto =  generateGetAccessCodeParametersDto(sseId);
        when(getAuthorizationCodeRepository.findById(sseId)).thenReturn(Optional.empty());
        UserInfo userInfo = new UserInfo();
        userInfo.setUsername("Test user");
        when(userInfoProvider.get()).thenReturn(userInfo);
        when(encryptionService.isEncrypted(getAccessCodeParametersDto.getClientSecret())).thenReturn(true);
        ArgumentCaptor<GetAuthorizationCode> getAuthorizationCodeCapture = ArgumentCaptor.forClass(GetAuthorizationCode.class);
        //action
        getAccessTokenByCodeService.saveParamsForGetAccessCode(getAccessCodeParametersDto);
        //check
        verify(getAuthorizationCodeRepository, times(1)).save(getAuthorizationCodeCapture.capture());
        GetAuthorizationCode getAuthorizationCode = getAuthorizationCodeCapture.getValue();
        assertEquals(getAccessCodeParametersDto.getProjectId(), getAuthorizationCode.getProjectId());
        assertEquals(getAccessCodeParametersDto.getSseId(), getAuthorizationCode.getSseId());
        assertNotNull(getAuthorizationCode.getStartedAt());
        assertEquals(getAccessCodeParametersDto.getClientId(), getAuthorizationCode.getClientId());
        assertEquals(getAccessCodeParametersDto.getClientSecret(), getAuthorizationCode.getClientSecret());
        assertEquals(getAccessCodeParametersDto.getScope(), getAuthorizationCode.getScope());
        assertEquals(getAccessCodeParametersDto.getState(), getAuthorizationCode.getState());
        assertNull(getAuthorizationCode.getAuthorizationCode());
        assertNull(getAuthorizationCode.getToken());
    }

    @Test
    void saveParamsForGetAccessCode_whenClientSecretNeedToEncrypt() throws AtpEncryptException {
        final UUID sseId = UUID.randomUUID();
        //mock
        when(encryptionService.encodeBase64(anyString())).thenCallRealMethod();
        when(encryptionService.decodeBase64(anyString())).thenCallRealMethod();
        GetAccessCodeParametersDto getAccessCodeParametersDto =  generateGetAccessCodeParametersDto(sseId);
        String initClientSecret = getAccessCodeParametersDto.getClientSecret();
        getAccessCodeParametersDto.setClientSecret(TO_ENCRYPT_FLAG + encryptionService.encodeBase64(initClientSecret));
        when(getAuthorizationCodeRepository.findById(sseId)).thenReturn(Optional.empty());
        UserInfo userInfo = new UserInfo();
        userInfo.setUsername("Test user");
        when(userInfoProvider.get()).thenReturn(userInfo);
        when(encryptionService.isEncrypted(getAccessCodeParametersDto.getClientSecret())).thenReturn(false);
        when(encryptionService.encrypt(initClientSecret)).thenReturn(getAccessCodeParametersDto.getClientSecret() + " encrypted");
        ArgumentCaptor<GetAuthorizationCode> getAuthorizationCodeCapture = ArgumentCaptor.forClass(GetAuthorizationCode.class);
        //action
        getAccessTokenByCodeService.saveParamsForGetAccessCode(getAccessCodeParametersDto);
        //check
        verify(getAuthorizationCodeRepository, times(1)).save(getAuthorizationCodeCapture.capture());
        GetAuthorizationCode getAuthorizationCode = getAuthorizationCodeCapture.getValue();
        assertEquals(getAccessCodeParametersDto.getProjectId(), getAuthorizationCode.getProjectId());
        assertEquals(getAccessCodeParametersDto.getSseId(), getAuthorizationCode.getSseId());
        assertNotNull(getAuthorizationCode.getStartedAt());
        assertEquals(getAccessCodeParametersDto.getClientId(), getAuthorizationCode.getClientId());
        assertEquals(getAccessCodeParametersDto.getClientSecret() + " encrypted", getAuthorizationCode.getClientSecret());
        assertEquals(getAccessCodeParametersDto.getScope(), getAuthorizationCode.getScope());
        assertEquals(getAccessCodeParametersDto.getState(), getAuthorizationCode.getState());
        assertNull(getAuthorizationCode.getAuthorizationCode());
        assertNull(getAuthorizationCode.getToken());
    }
}