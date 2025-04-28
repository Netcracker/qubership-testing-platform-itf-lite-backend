package org.qubership.atp.itf.lite.backend.components.export.strategies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRandomHttpRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestMiaExportRequest;

import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.itf.lite.backend.components.export.strategies.request.MiaRestRequestExportStrategy;
import org.qubership.atp.itf.lite.backend.components.export.strategies.request.MiaSoapRequestExportStrategy;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.model.api.kafka.MiaExportRequestEvent;
import org.qubership.atp.itf.lite.backend.model.api.kafka.entities.HttpRequestExportEntity;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestMiaExportRequest;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.service.RequestService;
import org.qubership.atp.itf.lite.backend.service.kafka.KafkaExportEventSendingService;

@ExtendWith(MockitoExtension.class)
public class MiaRequestExportStrategyTest {

    private static MiaRestRequestExportStrategy miaRestRequestExportStrategy;
    private static MiaSoapRequestExportStrategy miaSoapRequestExportStrategy;
    private static KafkaExportEventSendingService kafkaExportEventSendingService;
    private static RequestService requestService;
    private static ModelMapper modelMapper;

    @BeforeEach
    public void setUp() {
        kafkaExportEventSendingService = mock(KafkaExportEventSendingService.class);
        requestService = mock(RequestService.class);
        modelMapper = mock(ModelMapper.class);
        miaRestRequestExportStrategy = new MiaRestRequestExportStrategy(kafkaExportEventSendingService,
                requestService, modelMapper);
        miaSoapRequestExportStrategy = new MiaSoapRequestExportStrategy(kafkaExportEventSendingService,
                requestService, modelMapper);
    }

    @Test
    public void sendExportRequestTest_shouldSuccessfullySend() throws URISyntaxException, AtpDecryptException {
        // given
        UUID exportRequestId = UUID.randomUUID();
        RequestMiaExportRequest requestMiaExportRequest = generateRequestMiaExportRequest();
        HttpRequest httpRestRequest = generateRandomHttpRequest();
        HttpRequestExportEntity miaRestRequestEntity = new HttpRequestExportEntity(httpRestRequest, true);
        HttpRequest httpSoapRequest = generateRandomHttpRequest();
        httpSoapRequest.setTransportType(TransportType.SOAP);
        HttpRequestExportEntity miaSoapRequestEntity = new HttpRequestExportEntity(httpSoapRequest);

        // when
        miaRestRequestExportStrategy.export(exportRequestId, requestMiaExportRequest, httpRestRequest, null, null);
        miaSoapRequestExportStrategy.export(exportRequestId, requestMiaExportRequest, httpSoapRequest, null, null);
        // then
        ArgumentCaptor<MiaExportRequestEvent> miaExportRequestEventArgumentCaptor =
                ArgumentCaptor.forClass(MiaExportRequestEvent.class);
        verify(kafkaExportEventSendingService, times(2))
                .miaExportRequestEventSend(any(), miaExportRequestEventArgumentCaptor.capture());
        List<MiaExportRequestEvent> actualMiaExportRequests = miaExportRequestEventArgumentCaptor.getAllValues();
        assertEquals(2, actualMiaExportRequests.size());

        MiaExportRequestEvent actualMiaRestExportRequest = actualMiaExportRequests.get(0);
        assertEquals(exportRequestId, actualMiaRestExportRequest.getId());
        assertEquals(requestMiaExportRequest.getProjectId(), actualMiaRestExportRequest.getProjectId());
        assertEquals(requestMiaExportRequest.getMiaPath(), actualMiaRestExportRequest.getMiaPath());
        assertEquals(httpRestRequest.getName(), actualMiaRestExportRequest.getMiaProcessName());
        assertEquals(miaRestRequestEntity, actualMiaRestExportRequest.getRequest());

        MiaExportRequestEvent actualMiaSoapExportRequest = actualMiaExportRequests.get(1);
        assertEquals(exportRequestId, actualMiaSoapExportRequest.getId());
        assertEquals(requestMiaExportRequest.getProjectId(), actualMiaSoapExportRequest.getProjectId());
        assertEquals(requestMiaExportRequest.getMiaPath(), actualMiaSoapExportRequest.getMiaPath());
        assertEquals(httpSoapRequest.getName(), actualMiaSoapExportRequest.getMiaProcessName());
        assertEquals(miaSoapRequestEntity, actualMiaSoapExportRequest.getRequest());
    }
}
