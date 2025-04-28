package org.qubership.atp.itf.lite.backend.exceptions;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.springframework.web.bind.annotation.ResponseStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CheckDuplicatedReasonTest {

    private static final String MODELS_PACKAGE_SCAN = "org/qubership/atp/itf/lite/backend/exceptions";

    @Test
    public void ExceptionsHaveNoOverlappingReasonsTest() {
        Set<Class<?>> customExceptions = lookupCustomExceptionsClasses();
        Map<String, List<Class<?>>> groupedByReasonExceptions = customExceptions
                .stream()
                .collect(Collectors.groupingBy(clazz -> clazz.getAnnotation(ResponseStatus.class).reason()));
        for (Map.Entry<String, List<Class<?>>> entry : groupedByReasonExceptions.entrySet()) {
            Assertions.assertEquals(1, entry.getValue().size(),
                    String.format("Found duplicated reason for %s in classes %s", entry.getKey(), entry.getValue()));
        }
    }

    private Set<Class<?>> lookupCustomExceptionsClasses() {
        Reflections reflections = new Reflections(MODELS_PACKAGE_SCAN);
        return reflections.getTypesAnnotatedWith(ResponseStatus.class);
    }
}
