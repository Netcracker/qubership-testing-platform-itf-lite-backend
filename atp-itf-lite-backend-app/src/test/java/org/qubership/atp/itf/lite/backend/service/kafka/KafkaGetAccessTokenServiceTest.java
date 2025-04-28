package org.qubership.atp.itf.lite.backend.service.kafka;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateGetAuthorizationCode;

import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.itf.lite.backend.components.auth.OAuth2RequestAuthorizationStrategy;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.GetAuthorizationCodeRepository;
import org.qubership.atp.itf.lite.backend.model.api.kafka.GetAccessTokenFinish;
import org.qubership.atp.itf.lite.backend.model.api.response.auth.OAuth2AuthrizationResponse;
import org.qubership.atp.itf.lite.backend.model.api.sse.GetAccessTokenData;
import org.qubership.atp.itf.lite.backend.model.entities.auth.GetAuthorizationCode;
import org.qubership.atp.itf.lite.backend.service.EncryptionService;
import org.qubership.atp.itf.lite.backend.service.SseEmitterService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class KafkaGetAccessTokenServiceTest {

    private KafkaGetAccessTokenService kafkaGetAccessTokenService;
    private KafkaTemplate<UUID, GetAccessTokenFinish> kafkaTemplate;
    private SseEmitterService sseEmitterService;
    private GetAuthorizationCodeRepository getAuthorizationCodeRepository;
    private OAuth2RequestAuthorizationStrategy oAuth2Strategy;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        sseEmitterService = mock(SseEmitterService.class);
        getAuthorizationCodeRepository = mock(GetAuthorizationCodeRepository.class);
        oAuth2Strategy = mock(OAuth2RequestAuthorizationStrategy.class);
        EncryptionService encryptionService = mock(EncryptionService.class);
        kafkaGetAccessTokenService = new KafkaGetAccessTokenService(kafkaTemplate, sseEmitterService,
                getAuthorizationCodeRepository, encryptionService, oAuth2Strategy);
    }

    @Test
    void getAccessTokenFinishEventSend() {
        final UUID sseId = UUID.randomUUID();
        //mock
        ArgumentCaptor<String> topicNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<UUID> sseIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<GetAccessTokenFinish> itfLiteGetAccessTokenFinishEventCaptor = ArgumentCaptor.forClass(GetAccessTokenFinish.class);
        //action
        kafkaGetAccessTokenService.getAccessTokenFinishEventSend(sseId);
        //check
        verify(kafkaTemplate, times(1)).send(topicNameCaptor.capture(), sseIdCaptor.capture(),
                itfLiteGetAccessTokenFinishEventCaptor.capture());
        assertEquals(kafkaGetAccessTokenService.finishTopic, topicNameCaptor.getAllValues().get(0));
    }

    @Test
    public void listenItfLiteGetAccessTokenFinishEvent_emitterNotFound_shouldReturn() {
        final UUID sseId = UUID.randomUUID();
        //mock
        when(sseEmitterService.getEmitter(any())).thenReturn(null);
        //action
        kafkaGetAccessTokenService.listenItfLiteGetAccessTokenFinishEvent(new GetAccessTokenFinish(sseId));
        //check
        verify(getAuthorizationCodeRepository, never()).findById(any());
    }

    @Test
    public void listenItfLiteGetAccessTokenFinishEvent_recordInDbNotFound_shouldReturnException() {
        final UUID sseId = UUID.randomUUID();
        //mock
        SseEmitter sseEmitter = mock(SseEmitter.class);
        when(sseEmitterService.getEmitter(eq(sseId))).thenReturn(sseEmitter);
        when(getAuthorizationCodeRepository.findById(eq(sseId))).thenReturn(Optional.empty());
        ArgumentCaptor<GetAccessTokenData> captor = ArgumentCaptor.forClass(GetAccessTokenData.class);
        //action
        kafkaGetAccessTokenService.listenItfLiteGetAccessTokenFinishEvent(new GetAccessTokenFinish(sseId));
        //check
        verify(sseEmitterService).sendGetAccessTokenResult(eq(sseId), eq(sseEmitter), captor.capture());
        assertEquals("ITFL-1031", captor.getValue().getError().getReason());
        verify(getAuthorizationCodeRepository, never()).delete(any());
    }

    @Test
    public void listenItfLiteGetAccessTokenFinishEvent_stateIsIncorrect_shouldReturnException() {
        final UUID sseId = UUID.randomUUID();
        //mock
        String token = "token";
        SseEmitter sseEmitter = mock(SseEmitter.class);
        when(sseEmitterService.getEmitter(eq(sseId))).thenReturn(sseEmitter);
        Optional<GetAuthorizationCode> getAuthorizationCodeOpt = generateGetAuthorizationCode(sseId, token);
        getAuthorizationCodeOpt.get().setResponseState("");
        when(getAuthorizationCodeRepository.findById(eq(sseId))).thenReturn(getAuthorizationCodeOpt);
        ArgumentCaptor<GetAccessTokenData> captor = ArgumentCaptor.forClass(GetAccessTokenData.class);
        //action
        kafkaGetAccessTokenService.listenItfLiteGetAccessTokenFinishEvent(new GetAccessTokenFinish(sseId));
        //check
        verify(sseEmitterService, times(1)).sendGetAccessTokenResult(eq(sseId), eq(sseEmitter), captor.capture());
        assertEquals("ITFL-1034", captor.getValue().getError().getReason());
    }

    @Test
    void listenItfLiteGetAccessTokenFinishEvent_whenAuth2StrategyException_shouldReturnException() throws IOException {
        final UUID sseId = UUID.randomUUID();
        //mock
        String token = "token";
        SseEmitter sseEmitter = mock(SseEmitter.class);
        when(sseEmitterService.getEmitter(eq(sseId))).thenReturn(sseEmitter);
        Optional<GetAuthorizationCode> getAuthorizationCodeOpt = generateGetAuthorizationCode(sseId, token);
        when(getAuthorizationCodeRepository.findById(eq(sseId))).thenReturn(getAuthorizationCodeOpt);
        when(oAuth2Strategy.performAuthorization(
                eq(getAuthorizationCodeOpt.get().getProjectId()),
                eq(getAuthorizationCodeOpt.get().getAccessTokenUrl()),
                any()))
                .thenThrow(new RuntimeException("Incorrect URL"));
        ArgumentCaptor<SseEmitter.SseEventBuilder> captor = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        doCallRealMethod().when(sseEmitterService).sendGetAccessTokenResult(eq(sseId), eq(sseEmitter), any(GetAccessTokenData.class));

        //action
        kafkaGetAccessTokenService.listenItfLiteGetAccessTokenFinishEvent(new GetAccessTokenFinish(sseId));

        //check
        verify(sseEmitter, times(1)).send(captor.capture());
        Iterator<ResponseBodyEmitter.DataWithMediaType> iterator = captor.getValue().build().iterator();
        assertEquals("event:GET_ACCESS_TOKEN_FINISHED\ndata:", iterator.next().getData());
        assertTrue(new ObjectMapper().writeValueAsString(iterator.next().getData()).
                matches("\\{\"data\":null,\"error\":\\{"
                                + "\"status\":500,"
                                + "\"path\":null,"
                                + "\"timestamp\":\\d+,"
                                + "\"trace\":null,"
                                + "\"message\":\"Get access token by code failed due to Incorrect URL\","
                                + "\"reason\":\"ITFL-1033\","
                                + "\"details\":null\\}"
                                + "\\}"));
    }

    @Test
    void listenItfLiteGetAccessTokenFinishEvent() throws IOException {
        final UUID sseId = UUID.randomUUID();
        //mock
        String token = "token";
        SseEmitter sseEmitter = mock(SseEmitter.class);
        when(sseEmitterService.getEmitter(eq(sseId))).thenReturn(sseEmitter);
        Optional<GetAuthorizationCode> getAuthorizationCodeOpt = generateGetAuthorizationCode(sseId, token);
        when(getAuthorizationCodeRepository.findById(eq(sseId))).thenReturn(getAuthorizationCodeOpt);
        OAuth2AuthrizationResponse oAuth2AuthrizationResponse = new OAuth2AuthrizationResponse();
        oAuth2AuthrizationResponse.setAccessToken(token);
        when(oAuth2Strategy.performAuthorization(
                eq(getAuthorizationCodeOpt.get().getProjectId()),
                eq(getAuthorizationCodeOpt.get().getAccessTokenUrl()),
                any()))
                .thenReturn(oAuth2AuthrizationResponse);
        GetAccessTokenData getAccessTokenData = new GetAccessTokenData(oAuth2AuthrizationResponse);
        doCallRealMethod().when(sseEmitterService).sendGetAccessTokenResult(eq(sseId), eq(sseEmitter), eq(getAccessTokenData));
        ArgumentCaptor<SseEmitter.SseEventBuilder> captor = ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);
        //action
        kafkaGetAccessTokenService.listenItfLiteGetAccessTokenFinishEvent(new GetAccessTokenFinish(sseId));
        //check
        verify(sseEmitter, times(1)).send(captor.capture());
        Iterator<ResponseBodyEmitter.DataWithMediaType> iterator = captor.getValue().build().iterator();
        assertEquals("event:GET_ACCESS_TOKEN_FINISHED\ndata:", iterator.next().getData());
        assertEquals("{\"data\":{\"access_token\":\"" + oAuth2AuthrizationResponse.getAccessToken() + "\"},\"error\":null}",
                new ObjectMapper().writeValueAsString(iterator.next().getData()));
        verify(sseEmitter, times(1)).complete();
        verify(getAuthorizationCodeRepository, times(1)).delete(getAuthorizationCodeOpt.get());
    }
}