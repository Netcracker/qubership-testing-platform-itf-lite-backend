package org.qubership.atp.itf.lite.backend.feign.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class LogRecordDtoDeserializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    public void deserialize_typeTransportProtocolTypeRest_returnsRestLogRecordDto() throws Exception {
        String json = "["
                + "{"
                + "\"uuid\":\"554e5cce-2701-495b-8a56-f03147cf3450\","
                + "\"name\":\"atp-versions\","
                + "\"type\":\"TRANSPORT\","
                + "\"protocolType\":\"REST\","
                + "\"request\":{"
                + "\"endpoint\":\"https://example.com/\","
                + "\"method\":\"GET\","
                + "\"headersList\":[]"
                + "},"
                + "\"response\":{"
                + "\"code\":\"200\","
                + "\"headersList\":["
                + "{\"name\":\"Content-Type\",\"value\":\"text/html\"}"
                + "]"
                + "}"
                + "}"
                + "]";

        List<LogRecordDto> logRecords = objectMapper.readValue(json, new TypeReference<List<LogRecordDto>>() {});

        assertEquals(1, logRecords.size());
        LogRecordDto logRecord = logRecords.get(0);
        assertInstanceOf(RestLogRecordDto.class, logRecord);
        assertEquals(TypeActionDto.TRANSPORT, logRecord.getType());
        assertEquals("REST", logRecord.getProtocolType());

        RestLogRecordDto restLogRecord = (RestLogRecordDto) logRecord;
        assertNotNull(restLogRecord.getRequest());
        assertEquals("https://example.com/", restLogRecord.getRequest().getEndpoint());
        assertNotNull(restLogRecord.getResponse());
        assertEquals("200", restLogRecord.getResponse().getCode());
        assertFalse(restLogRecord.getResponse().getHeadersList().isEmpty());
    }

    @Test
    public void deserialize_nonRestProtocolType_returnsBaseLogRecordDto() throws Exception {
        String json = "["
                + "{"
                + "\"uuid\":\"554e5cce-2701-495b-8a56-f03147cf3450\","
                + "\"name\":\"sql-step\","
                + "\"type\":\"TRANSPORT\","
                + "\"protocolType\":\"SQL\""
                + "}"
                + "]";

        List<LogRecordDto> logRecords = objectMapper.readValue(json, new TypeReference<List<LogRecordDto>>() {});

        assertEquals(1, logRecords.size());
        LogRecordDto logRecord = logRecords.get(0);
        assertTrue(logRecord.getClass().equals(LogRecordDto.class));
        assertFalse(logRecord instanceof RestLogRecordDto);
        assertEquals(TypeActionDto.TRANSPORT, logRecord.getType());
        assertEquals("SQL", logRecord.getProtocolType());
    }
}
