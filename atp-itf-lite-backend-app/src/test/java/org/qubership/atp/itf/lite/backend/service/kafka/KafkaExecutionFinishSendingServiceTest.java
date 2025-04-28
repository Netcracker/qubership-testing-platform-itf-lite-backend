package org.qubership.atp.itf.lite.backend.service.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.model.api.kafka.ItfLiteExecutionFinishEvent;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class KafkaExecutionFinishSendingServiceTest {

    private static KafkaExecutionFinishSendingService kafkaExecutionFinishSendingService;

    private static String itfLiteExecutionFinishTopicName;
    private static KafkaTemplate<UUID, ItfLiteExecutionFinishEvent> itfLiteExecutionFinishKafkaTemplate;

    @BeforeEach
    public void setUp() {
        itfLiteExecutionFinishTopicName = "itfLiteExecutionFinishTopic";
        itfLiteExecutionFinishKafkaTemplate = mock(KafkaTemplate.class);
        kafkaExecutionFinishSendingService = new KafkaExecutionFinishSendingService(
                itfLiteExecutionFinishTopicName, itfLiteExecutionFinishKafkaTemplate);
    }

    @Test
    void executionFinishKafkaEventSendTest_shouldSuccessfullySend() {
        // given
        UUID sseId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        ItfLiteExecutionFinishEvent expectedRestExecutionFinishEvent = new ItfLiteExecutionFinishEvent(
                sseId, requestId, TransportType.REST);
        ItfLiteExecutionFinishEvent expectedSoapExecutionFinishEvent = new ItfLiteExecutionFinishEvent(
                sseId, requestId, TransportType.SOAP);
        // when
        kafkaExecutionFinishSendingService.executionFinishEventSend(expectedRestExecutionFinishEvent);
        kafkaExecutionFinishSendingService.executionFinishEventSend(expectedSoapExecutionFinishEvent);
        // then
        ArgumentCaptor<String> topicNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<UUID> sseIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<ItfLiteExecutionFinishEvent> itfLiteExecutionFinishEventCaptor =
                ArgumentCaptor.forClass(ItfLiteExecutionFinishEvent.class);
        verify(itfLiteExecutionFinishKafkaTemplate, times(2)).send(
                topicNameCaptor.capture(), sseIdCaptor.capture(), itfLiteExecutionFinishEventCaptor.capture());
        assertEquals(itfLiteExecutionFinishTopicName, topicNameCaptor.getAllValues().get(0));
        assertEquals(itfLiteExecutionFinishTopicName, topicNameCaptor.getAllValues().get(1));
        assertEquals(sseId, sseIdCaptor.getAllValues().get(0));
        assertEquals(sseId, sseIdCaptor.getAllValues().get(1));
        assertEquals(expectedRestExecutionFinishEvent, itfLiteExecutionFinishEventCaptor.getAllValues().get(0));
        assertEquals(expectedSoapExecutionFinishEvent, itfLiteExecutionFinishEventCaptor.getAllValues().get(1));
    }
}