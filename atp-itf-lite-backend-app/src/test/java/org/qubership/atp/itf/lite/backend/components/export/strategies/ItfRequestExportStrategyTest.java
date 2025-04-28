package org.qubership.atp.itf.lite.backend.components.export.strategies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRandomHttpRequest;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRequestItfExportRequest;

import java.math.BigInteger;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.itf.lite.backend.components.export.strategies.request.ItfRestRequestExportStrategy;
import org.qubership.atp.itf.lite.backend.components.export.strategies.request.ItfSoapRequestExportStrategy;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.model.api.kafka.ItfExportRequestEvent;
import org.qubership.atp.itf.lite.backend.model.api.kafka.entities.HttpRequestExportEntity;
import org.qubership.atp.itf.lite.backend.model.api.request.RequestItfExportRequest;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.service.RequestService;
import org.qubership.atp.itf.lite.backend.service.kafka.KafkaExportEventSendingService;

import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class ItfRequestExportStrategyTest {

    private static ItfRestRequestExportStrategy itfRestRequestExportStrategy;
    private static ItfSoapRequestExportStrategy itfSoapRequestExportStrategy;
    private static KafkaExportEventSendingService kafkaExportEventSendingService;
    private static RequestService requestService;

    @BeforeEach
    public void setUp() {
        kafkaExportEventSendingService = mock(KafkaExportEventSendingService.class);
        requestService = mock(RequestService.class);
        itfRestRequestExportStrategy = new ItfRestRequestExportStrategy(kafkaExportEventSendingService, requestService);
        itfSoapRequestExportStrategy = new ItfSoapRequestExportStrategy(kafkaExportEventSendingService, requestService);
    }

    @Test
    public void sendExportRequestEventTest_shouldSuccessfullySend() throws URISyntaxException, AtpDecryptException {
        // given
        UUID exportRequestId = UUID.randomUUID();
        HttpRequest httpRestRequest = generateRandomHttpRequest();
        BigInteger restSystemId = BigInteger.ONE;
        HttpRequest httpSoapRequest = generateRandomHttpRequest();
        httpSoapRequest.setTransportType(TransportType.SOAP);
        BigInteger soapSystemId = BigInteger.ONE.add(BigInteger.ONE);
        HashSet<UUID> requestIds = Sets.newHashSet(httpRestRequest.getId(), httpSoapRequest.getId());
        Map<UUID, BigInteger> requestIdsReceiversMap = new HashMap<>();
        requestIdsReceiversMap.put(httpRestRequest.getId(), restSystemId);
        requestIdsReceiversMap.put(httpSoapRequest.getId(), soapSystemId);
        RequestItfExportRequest requestItfExportRequest = generateRequestItfExportRequest(
                requestIds, requestIdsReceiversMap);
        // when
        when(requestService.resolveAllVariables(any(), any(), any(), any())).thenAnswer(i -> i.getArguments()[0]);
        itfRestRequestExportStrategy.export(exportRequestId, requestItfExportRequest, httpRestRequest, null, null);
        itfSoapRequestExportStrategy.export(exportRequestId, requestItfExportRequest, httpSoapRequest, null, null);
        // then
        ArgumentCaptor<ItfExportRequestEvent> itfExportRequestEventArgumentCaptor =
                ArgumentCaptor.forClass(ItfExportRequestEvent.class);
        verify(kafkaExportEventSendingService, times(2))
                .itfExportRequestEventSend(any(), itfExportRequestEventArgumentCaptor.capture());
        List<ItfExportRequestEvent> actualItfExportRequests = itfExportRequestEventArgumentCaptor.getAllValues();
        assertEquals(2, actualItfExportRequests.size());
        ItfExportRequestEvent actualItfRestExportRequest = actualItfExportRequests.get(0);
        assertEquals(exportRequestId, actualItfRestExportRequest.getId());
        assertEquals(requestItfExportRequest.getProjectId(), actualItfRestExportRequest.getProjectId());
        assertEquals(requestItfExportRequest.getItfUrl(), actualItfRestExportRequest.getItfUrl());
        assertEquals(requestItfExportRequest.getSystemId().toString(), actualItfRestExportRequest.getSystemId());
        assertEquals(requestItfExportRequest.getOperationId().toString(), actualItfRestExportRequest.getOperationId());
        assertEquals(restSystemId.toString(), actualItfRestExportRequest.getReceiver());
        HttpRequestExportEntity expectedRestEntity = new HttpRequestExportEntity(httpRestRequest, true);
        assertEquals(expectedRestEntity, actualItfRestExportRequest.getRequest());
        ItfExportRequestEvent actualItfSoapExportRequest = actualItfExportRequests.get(1);
        assertEquals(exportRequestId, actualItfSoapExportRequest.getId());
        assertEquals(requestItfExportRequest.getProjectId(), actualItfSoapExportRequest.getProjectId());
        assertEquals(requestItfExportRequest.getItfUrl(), actualItfSoapExportRequest.getItfUrl());
        assertEquals(requestItfExportRequest.getSystemId().toString(), actualItfSoapExportRequest.getSystemId());
        assertEquals(requestItfExportRequest.getOperationId().toString(), actualItfSoapExportRequest.getOperationId());
        assertEquals(soapSystemId.toString(), actualItfSoapExportRequest.getReceiver());
        HttpRequestExportEntity expectedSoapEntity = new HttpRequestExportEntity(httpSoapRequest);
        assertEquals(expectedSoapEntity, actualItfSoapExportRequest.getRequest());
    }
}
