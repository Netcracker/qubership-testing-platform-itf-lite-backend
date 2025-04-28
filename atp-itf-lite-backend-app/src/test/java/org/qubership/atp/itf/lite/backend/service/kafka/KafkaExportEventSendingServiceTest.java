package org.qubership.atp.itf.lite.backend.service.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRandomHttpRequest;

import java.math.BigInteger;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.itf.lite.backend.model.api.kafka.ItfExportRequestEvent;
import org.qubership.atp.itf.lite.backend.model.api.kafka.MiaExportRequestEvent;
import org.qubership.atp.itf.lite.backend.model.api.kafka.entities.HttpRequestExportEntity;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
public class KafkaExportEventSendingServiceTest {

    private final ThreadLocal<KafkaExportEventSendingService> kafkaExportEventSendingService = new ThreadLocal<>();
    private final ThreadLocal<KafkaTemplate<UUID, MiaExportRequestEvent>> miaExportKafkaTemplate = new ThreadLocal<>();
    private final ThreadLocal<KafkaTemplate<UUID, ItfExportRequestEvent>> itfExportKafkaTemplate = new ThreadLocal<>();

    private static final String miaTopicName = "miaTopic";
    private static final String itfTopicName = "itfTopic";

    @BeforeEach
    public void setUp() {
        KafkaTemplate miaExportKafkaTemplateMock = mock(KafkaTemplate.class);
        KafkaTemplate itfExportKafkaTemplateMock = mock(KafkaTemplate.class);
        miaExportKafkaTemplate.set(miaExportKafkaTemplateMock);
        itfExportKafkaTemplate.set(itfExportKafkaTemplateMock);
        kafkaExportEventSendingService.set(new KafkaExportEventSendingService(
                miaTopicName, miaExportKafkaTemplateMock, itfTopicName, itfExportKafkaTemplateMock));
    }

    @Test
    public void sendMiaExportRequestTest_shouldSuccessfullySend() {
        // given
        final UUID requestExportId = UUID.randomUUID();
        HttpRequest httpRequest = generateRandomHttpRequest();
        String miaPath = "path/to/mia/request";
        HttpRequestExportEntity miaRequestEntity = new HttpRequestExportEntity(httpRequest, true);
        MiaExportRequestEvent expectedMiaExportRequest = new MiaExportRequestEvent(
                requestExportId, httpRequest.getProjectId(), miaPath, httpRequest.getName(), miaRequestEntity);
        // when
        kafkaExportEventSendingService.get().miaExportRequestEventSend(requestExportId, expectedMiaExportRequest);
        // then
        ArgumentCaptor<String> topicNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<UUID> requestExportIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<MiaExportRequestEvent> miaExportRequestEventCaptor = ArgumentCaptor.forClass(MiaExportRequestEvent.class);
        verify(miaExportKafkaTemplate.get()).send(
                topicNameCaptor.capture(), requestExportIdCaptor.capture(), miaExportRequestEventCaptor.capture());
        assertEquals(miaTopicName, topicNameCaptor.getValue());
        assertEquals(requestExportId, requestExportIdCaptor.getValue());
        assertEquals(expectedMiaExportRequest, miaExportRequestEventCaptor.getValue());
    }

    @Test
    public void sendItfExportRequestTest_shouldSuccessfullySend() {
        // given
        final UUID requestExportId = UUID.randomUUID();
        String itfUrl = "http://itf";
        String systemId = BigInteger.ONE.toString();
        String operationId = BigInteger.ONE.toString();
        HttpRequest httpRequest = generateRandomHttpRequest();
        HttpRequestExportEntity itfHttpRequestEntity = new HttpRequestExportEntity(httpRequest, true);
        ItfExportRequestEvent expectedItfHttpExportRequest = new ItfExportRequestEvent(
                requestExportId, httpRequest.getProjectId(), itfUrl, systemId, operationId,
                systemId, itfHttpRequestEntity);
        // when
        kafkaExportEventSendingService.get().itfExportRequestEventSend(requestExportId, expectedItfHttpExportRequest);
        // then
        ArgumentCaptor<String> topicNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<UUID> requestExportIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<ItfExportRequestEvent> itfExportRequestEventCaptor = ArgumentCaptor.forClass(ItfExportRequestEvent.class);
        verify(itfExportKafkaTemplate.get(), times(1)).send(
                topicNameCaptor.capture(), requestExportIdCaptor.capture(), itfExportRequestEventCaptor.capture());
        assertEquals(1, topicNameCaptor.getAllValues().size());
        assertEquals(itfTopicName, topicNameCaptor.getAllValues().get(0));
        assertEquals(1, requestExportIdCaptor.getAllValues().size());
        assertEquals(requestExportId, requestExportIdCaptor.getAllValues().get(0));
        assertEquals(1, itfExportRequestEventCaptor.getAllValues().size());
        assertEquals(expectedItfHttpExportRequest, itfExportRequestEventCaptor.getAllValues().get(0));
    }
}
