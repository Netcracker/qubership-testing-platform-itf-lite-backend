package org.qubership.atp.itf.lite.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.qubership.atp.itf.lite.backend.enums.ImportToolType;
import org.qubership.atp.itf.lite.backend.enums.RequestExportStatus;
import org.qubership.atp.itf.lite.backend.model.api.kafka.ItfExportResponseEvent;
import org.qubership.atp.itf.lite.backend.model.api.kafka.MiaExportResponseEvent;
import org.qubership.atp.itf.lite.backend.service.kafka.KafkaExportEventExceptionResponseSendingService;

public class RequestExportExceptionResponseServiceTest {

    private final ThreadLocal<RequestExportExceptionResponseService> requestExportExceptionResponseService = new ThreadLocal<>();
    private final ThreadLocal<KafkaExportEventExceptionResponseSendingService> kafkaExportEventExceptionResponseSendingService = new ThreadLocal<>();

    @BeforeEach
    public void setUp() {
        KafkaExportEventExceptionResponseSendingService kafkaExportEventExceptionResponseSendingServiceMock = mock(KafkaExportEventExceptionResponseSendingService.class);
        kafkaExportEventExceptionResponseSendingService.set(kafkaExportEventExceptionResponseSendingServiceMock);
        requestExportExceptionResponseService.set(new RequestExportExceptionResponseService(
                kafkaExportEventExceptionResponseSendingServiceMock));
    }

    @Test
    public void sendMiaExportExceptionResponseEventTest_shouldSuccessfullySend() {
        // given
        UUID exportRequestId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        String errorMessage = "Exception";
        MiaExportResponseEvent expectedMiaExportResponseEvent = new MiaExportResponseEvent();
        expectedMiaExportResponseEvent.setId(exportRequestId);
        expectedMiaExportResponseEvent.setRequestId(requestId);
        expectedMiaExportResponseEvent.setErrorMessage(errorMessage);
        expectedMiaExportResponseEvent.setStatus(RequestExportStatus.ERROR.name());
        // when
        requestExportExceptionResponseService.get().sendExceptionResponseEvent(
                ImportToolType.MIA, exportRequestId, requestId, errorMessage);
        // then
        ArgumentCaptor<MiaExportResponseEvent> miaExportResponseEventCaptor =
                ArgumentCaptor.forClass(MiaExportResponseEvent.class);
        verify(kafkaExportEventExceptionResponseSendingService.get())
                .miaFinishExportResponseEventSend(miaExportResponseEventCaptor.capture());
        assertThat(miaExportResponseEventCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(expectedMiaExportResponseEvent);
    }

    @Test
    public void sendItfExportExceptionResponseEventTest_shouldSuccessfullySend() {
        // given
        UUID exportRequestId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        String errorMessage = "Exception";
        ItfExportResponseEvent expectedItfExportResponseEvent = new ItfExportResponseEvent();
        expectedItfExportResponseEvent.setId(exportRequestId);
        expectedItfExportResponseEvent.setRequestId(requestId);
        expectedItfExportResponseEvent.setErrorMessage(errorMessage);
        expectedItfExportResponseEvent.setStatus(RequestExportStatus.ERROR.name());
        // when
        requestExportExceptionResponseService.get().sendExceptionResponseEvent(
                ImportToolType.ITF, exportRequestId, requestId, errorMessage);
        // then
        ArgumentCaptor<ItfExportResponseEvent> itfExportResponseEventCaptor =
                ArgumentCaptor.forClass(ItfExportResponseEvent.class);
        verify(kafkaExportEventExceptionResponseSendingService.get())
                .itfFinishExportResponseEventSend(itfExportResponseEventCaptor.capture());
        assertThat(itfExportResponseEventCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(expectedItfExportResponseEvent);
    }
}
