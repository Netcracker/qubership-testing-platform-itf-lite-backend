package org.qubership.atp.itf.lite.backend.model.entities.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class HashMapConverterTest {

    private static final HashMapConverter hashMapConverter = new HashMapConverter();

    @Test
    void convertToDatabaseColumn() {
        Map<String, List<String>> requestParams = new HashMap<>();
        List<String> test = new ArrayList<>();
        test.add("test1");
        test.add("test2");
        requestParams.put("test",test);
        String expected = "{\"test\":[\"test1\",\"test2\"]}";
        String actual = hashMapConverter.convertToDatabaseColumn(requestParams);
        assertEquals(expected, actual);
    }

    @Test
    void convertToEntityAttribute() {
        Map<String, List<String>> requestParams = new HashMap<>();
        List<String> test = new ArrayList<>();
        test.add("test1");
        test.add("test2");
        requestParams.put("test",test);
        Map<String, List<String>> actual = hashMapConverter
                .convertToEntityAttribute("{\"test\":[\"test1\",\"test2\"]}");
        assertEquals(requestParams, actual);
    }
}