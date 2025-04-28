package org.qubership.atp.itf.lite.backend.model.entities.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.qubership.atp.itf.lite.backend.feign.dto.ConsoleLogDto;

class ListConsoleLogConverterTest {

    @Test
    void convertToDatabaseColumnAndBack() {
        String consoleLogs = "[{\"level\":\"info\",\"message\":\"somemessage\",\"timestamp\":123456789}]";
        ListConsoleLogConverter listConsoleLogConverter = new ListConsoleLogConverter();
        assertEquals(consoleLogs,
                listConsoleLogConverter.convertToDatabaseColumn(
                        listConsoleLogConverter.convertToEntityAttribute(consoleLogs)));
    }

    @Test
    void convertToStringAndBack() {
        List<ConsoleLogDto> consoleLogs = Arrays.asList(new ConsoleLogDto().level("info").message("somemessage").timestamp(123456789L));
        ListConsoleLogConverter listConsoleLogConverter = new ListConsoleLogConverter();
        assertEquals(consoleLogs,
                listConsoleLogConverter.convertToEntityAttribute(
                        listConsoleLogConverter.convertToDatabaseColumn(consoleLogs)));
    }
}