package org.qubership.atp.itf.lite.backend.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CompareEntitiesUtils {
    public static void compareEvents(SseEmitter.SseEventBuilder expectedEvent, SseEmitter.SseEventBuilder actualEvent) {
        List<ResponseBodyEmitter.DataWithMediaType> expectedEventData =
                new ArrayList<>(expectedEvent.build());
        List<ResponseBodyEmitter.DataWithMediaType> actualEventData =
                new ArrayList<>(actualEvent.build());
        for (int i = 0; i < actualEventData.size(); i++) {
            assertThat(actualEventData.get(i).getData())
                    .usingRecursiveComparison()
                    .isEqualTo(expectedEventData.get(i).getData());
        }
    }
}
