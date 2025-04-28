package org.qubership.atp.itf.lite.backend.service.kafka;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.itf.lite.backend.model.api.kafka.ItfLiteExecutionFinishEvent;
import org.qubership.atp.itf.lite.backend.service.SseEmitterService;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class KafkaExecutionFinishResponseServiceTest {

    private final ThreadLocal<KafkaExecutionFinishResponseService> kafkaExecutionFinishResponseService = new ThreadLocal<>();
    private final ThreadLocal<SseEmitterService> sseEmitterService = new ThreadLocal<>();
    private final ThreadLocal<SseEmitter> sseEmitter = new ThreadLocal<>();

    @BeforeEach
    void setUp() {
        SseEmitterService sseEmitterServiceMock = mock(SseEmitterService.class);
        kafkaExecutionFinishResponseService.set(
                new KafkaExecutionFinishResponseService(sseEmitterServiceMock));
        SseEmitter sseEmitterSpy = spy(SseEmitter.class);
        sseEmitterService.set(sseEmitterServiceMock);
        sseEmitter.set(sseEmitterSpy);
    }

    @Test
    public void listenItfLiteExecutionFinishEvent_emitterNotFound_shouldReturn() {
        // given
        final UUID sseId = UUID.randomUUID();
        ItfLiteExecutionFinishEvent itfLiteExecutionFinishEvent = new ItfLiteExecutionFinishEvent();
        itfLiteExecutionFinishEvent.setSseId(sseId);
        // when
        when(sseEmitterService.get().getEmitter(any())).thenReturn(null);

        kafkaExecutionFinishResponseService.get().listenItfLiteExecutionFinishEvent(itfLiteExecutionFinishEvent);
        // then
        verify(sseEmitterService.get(), never()).generateResponseAndSendToEmitter(any(), any());
    }

    @Test
    public void listenItfLiteExecutionFinishEvent_emitterNotFound_shouldExecuteSendFinishExecutionRespons() {
        // given
        final UUID sseId = UUID.randomUUID();
        ItfLiteExecutionFinishEvent itfLiteExecutionFinishEvent = new ItfLiteExecutionFinishEvent();
        itfLiteExecutionFinishEvent.setSseId(sseId);
        // when
        when(sseEmitterService.get().getEmitter(any())).thenReturn(sseEmitter.get());

        kafkaExecutionFinishResponseService.get().listenItfLiteExecutionFinishEvent(itfLiteExecutionFinishEvent);
        // then
        verify(sseEmitterService.get(), times(1)).generateResponseAndSendToEmitter(eq(sseEmitter.get()), eq(itfLiteExecutionFinishEvent));
    }
}
