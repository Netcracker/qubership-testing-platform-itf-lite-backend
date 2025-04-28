package org.qubership.atp.itf.lite.backend.model.entities.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ListConverterTest {

    private static final ListConverter listConverter = new ListConverter();

    @Test
    void convertToDatabaseColumn() {
        List<String> requestParams = Arrays.asList("test1", "test2");
        String requestParamsJsonString = "[\"test1\",\"test2\"]";
        String actual = listConverter.convertToDatabaseColumn(requestParams);
        assertEquals(requestParamsJsonString, actual);
    }

    @Test
    void convertToEntityAttribute() {
        List<String> requestParams = Arrays.asList("test1", "test2");
        String requestParamsJsonString = "[\"test1\",\"test2\"]";
        List<String> actual = listConverter.convertToEntityAttribute(requestParamsJsonString);
        assertEquals(requestParams, actual);
    }
}
