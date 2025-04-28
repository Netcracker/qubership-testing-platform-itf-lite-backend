package org.qubership.atp.itf.lite.backend.service.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.itf.lite.backend.enums.RequestExportStatus;
import org.qubership.atp.itf.lite.backend.model.api.kafka.ItfExportResponseEvent;
import org.qubership.atp.itf.lite.backend.model.api.kafka.MiaExportResponseEvent;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
public class KafkaExportEventExceptionResponseSendingServiceTest {

    private final ThreadLocal<KafkaExportEventExceptionResponseSendingService> kafkaExportEventExceptionResponseSendingService = new ThreadLocal<>();
    private final ThreadLocal<KafkaTemplate<UUID, MiaExportResponseEvent>> miaFinishExportKafkaTemplate = new ThreadLocal<>();
    private final ThreadLocal<KafkaTemplate<UUID, ItfExportResponseEvent>> itfFinishExportKafkaTemplate = new ThreadLocal<>();

    private static final String miaFinishTopicName = "miaTopic";
    private static final String itfFinishTopicName = "itfTopic";
    private static final String exportException = "Exception";

    @BeforeEach
    public void setUp() {
        KafkaTemplate miaFinishExportKafkaTemplateMock = mock(KafkaTemplate.class);
        KafkaTemplate itfFinishExportKafkaTemplateMock = mock(KafkaTemplate.class);
        miaFinishExportKafkaTemplate.set(miaFinishExportKafkaTemplateMock);
        itfFinishExportKafkaTemplate.set(itfFinishExportKafkaTemplateMock);
        kafkaExportEventExceptionResponseSendingService.set(new KafkaExportEventExceptionResponseSendingService(
                miaFinishTopicName, miaFinishExportKafkaTemplateMock, itfFinishTopicName, itfFinishExportKafkaTemplateMock));

    }

    @Test
    public void sendMiaExportRequestTest_shouldSuccessfullySend() {
        // given
        final UUID requestExportId = UUID.randomUUID();
        final UUID requestId = UUID.randomUUID();
        MiaExportResponseEvent expectedMiaExportResponseEvent = new MiaExportResponseEvent();
        expectedMiaExportResponseEvent.setId(requestExportId);
        expectedMiaExportResponseEvent.setRequestId(requestId);
        expectedMiaExportResponseEvent.setStatus(RequestExportStatus.ERROR.name());
        expectedMiaExportResponseEvent.setErrorMessage(exportException);
        // when
        kafkaExportEventExceptionResponseSendingService.get().miaFinishExportResponseEventSend(expectedMiaExportResponseEvent);
        // then
        ArgumentCaptor<String> topicNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<UUID> requestExportIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<MiaExportResponseEvent> miaExportResponseEventCaptor =
                ArgumentCaptor.forClass(MiaExportResponseEvent.class);
        verify(miaFinishExportKafkaTemplate.get()).send(
                topicNameCaptor.capture(), requestExportIdCaptor.capture(), miaExportResponseEventCaptor.capture());
        assertEquals(miaFinishTopicName, topicNameCaptor.getValue());
        assertEquals(requestExportId, requestExportIdCaptor.getValue());
        assertEquals(expectedMiaExportResponseEvent, miaExportResponseEventCaptor.getValue());
    }

    @Test
    public void sendItfExportRequestTest_shouldSuccessfullySend() {
        // given
        final UUID requestExportId = UUID.randomUUID();
        final UUID requestId = UUID.randomUUID();
        ItfExportResponseEvent expectedItfExportResponseEvent = new ItfExportResponseEvent();
        expectedItfExportResponseEvent.setId(requestExportId);
        expectedItfExportResponseEvent.setRequestId(requestId);
        expectedItfExportResponseEvent.setStatus(RequestExportStatus.ERROR.name());
        expectedItfExportResponseEvent.setErrorMessage(exportException);
        // when
        kafkaExportEventExceptionResponseSendingService.get().itfFinishExportResponseEventSend(expectedItfExportResponseEvent);
        // then
        ArgumentCaptor<String> topicNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<UUID> requestExportIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<ItfExportResponseEvent> itfExportResponseEventCaptor =
                ArgumentCaptor.forClass(ItfExportResponseEvent.class);
        verify(itfFinishExportKafkaTemplate.get()).send(
                topicNameCaptor.capture(), requestExportIdCaptor.capture(), itfExportResponseEventCaptor.capture());
        assertEquals(itfFinishTopicName, topicNameCaptor.getValue());
        assertEquals(requestExportId, requestExportIdCaptor.getValue());
        assertEquals(expectedItfExportResponseEvent, itfExportResponseEventCaptor.getValue());
    }
}
