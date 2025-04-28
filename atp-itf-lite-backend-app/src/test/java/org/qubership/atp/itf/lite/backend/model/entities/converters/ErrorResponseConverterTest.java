package org.qubership.atp.itf.lite.backend.model.entities.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpException;
import org.qubership.atp.itf.lite.backend.exceptions.jsengine.ItfLiteScriptEnginePreScriptExecutionException;
import org.qubership.atp.itf.lite.backend.model.api.response.ErrorResponseSerializable;
import org.qubership.atp.itf.lite.backend.utils.RequestUtils;

class ErrorResponseConverterTest {

    private static final ErrorResponseSerializableConverter errorResponseConverter = new ErrorResponseSerializableConverter();
    private static final Long timestamp = 123456789L + (long) (Math.random() * (1234567890L - 123456789L));

    @Test
    void convertToDatabaseColumn_null() {
        assertNull(errorResponseConverter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToDatabaseColumn_Exception() {
        String toMatch = "\\{" +
                "\"status\":500," +
                "\"path\":null," +
                "\"timestamp\":\\d+," +
                "\"trace\":null," +
                "\"message\":null," +
                "\"reason\":\"ITFL-0000\"," +
                "\"details\":null\\}";
        assertTrue(errorResponseConverter.convertToDatabaseColumn(RequestUtils.getErrorResponse(new Exception())).matches(toMatch));
    }

    @Test
    void convertToDatabaseColumn_AtpException() {
        String toMatch = "\\{" +
                "\"status\":500," +
                "\"path\":null," +
                "\"timestamp\":\\d+," +
                "\"trace\":null," +
                "\"message\":\"Atp exception\"," +
                "\"reason\":\"ATP-0000\"," +
                "\"details\":null\\}";
        assertTrue(errorResponseConverter.convertToDatabaseColumn(RequestUtils.getErrorResponse(new AtpException("Atp exception"))).matches(toMatch));
    }

    @Test
    void convertToDatabaseColumn_AtpItfLiteException() {
        String toMatch = "\\{" +
                "\"status\":400,\"" +
                "path\":null,\"" +
                "timestamp\":\\d+,\"" +
                "trace\":null," +
                "\"message\":\"Failed to execute pre-script in JS engine: Atp script exception\"," +
                "\"reason\":\"ITFL-1020\"," +
                "\"details\":null\\}";
        assertTrue(errorResponseConverter.convertToDatabaseColumn(RequestUtils.getErrorResponse(new ItfLiteScriptEnginePreScriptExecutionException("Atp script exception"))).matches(toMatch));
    }

    @Test
    void convertToEntityAttribute_null() {
        assertNull(errorResponseConverter.convertToEntityAttribute(null));
    }

    @Test
    void convertToEntityAttribute_Exception() {
        String exception = "{" +
                "\"status\":500," +
                "\"path\":null," +
                "\"timestamp\":" + timestamp + "," +
                "\"trace\":null," +
                "\"message\":null," +
                "\"reason\":\"ITFL-0000\"," +
                "\"details\":null}";
        ErrorResponseSerializable errorResponse = RequestUtils.getErrorResponse(new Exception());
        errorResponse.setTimestamp(new Date(timestamp));
        assertEquals(errorResponse, errorResponseConverter.convertToEntityAttribute(exception));
    }

    @Test
    void convertToEntityAttribute_AtpException() {
        String exception = "{" +
                "\"status\":500," +
                "\"path\":null," +
                "\"timestamp\":" + timestamp + "," +
                "\"trace\":null," +
                "\"message\":\"Atp exception\"," +
                "\"reason\":\"ATP-0000\"," +
                "\"details\":null}";
        ErrorResponseSerializable errorResponse = RequestUtils.getErrorResponse(new AtpException("Atp exception"));
        errorResponse.setTimestamp(new Date(timestamp));
        assertEquals(errorResponse, errorResponseConverter.convertToEntityAttribute(exception));
    }

    @Test
    void convertToEntityAttribute_ItfLiteException() {
        String exception = "{" +
                "\"status\":400," +
                "\"path\":null," +
                "\"timestamp\":" + timestamp + "," +
                "\"trace\":null," +
                "\"message\":\"Failed to execute pre-script in JS engine: Atp script exception\"," +
                "\"reason\":\"ITFL-1020\"," +
                "\"details\":null}";
        ErrorResponseSerializable errorResponse = RequestUtils.getErrorResponse(new ItfLiteScriptEnginePreScriptExecutionException("Atp script exception"));
        errorResponse.setTimestamp(new Date(timestamp));
        errorResponse.setStatus(400);
        assertEquals(errorResponse, errorResponseConverter.convertToEntityAttribute(exception));
    }

    @Test
    void convertToEntityAttribute_String() {
        String exception = "Failed to execute in JS engine: Atp script exception";
        ErrorResponseSerializable errorResponse = errorResponseConverter.convertToEntityAttribute(exception);
        assertEquals(500, errorResponse.getStatus());
        assertEquals("Failed to execute in JS engine: Atp script exception", errorResponse.getMessage());
        assertEquals("ITFL-0000", errorResponse.getReason());
    }

}
