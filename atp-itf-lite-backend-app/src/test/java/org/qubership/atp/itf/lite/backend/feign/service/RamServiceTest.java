package org.qubership.atp.itf.lite.backend.feign.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteIllegalTestRunsCountInExecutionRequestException;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteIncorrectImportRequest;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteTestRunsNotFoundException;
import org.qubership.atp.itf.lite.backend.feign.clients.RamExecutionRequestFeignClient;
import org.qubership.atp.itf.lite.backend.feign.clients.RamTestRunsFeignClient;
import org.qubership.atp.itf.lite.backend.feign.dto.LogRecordFilteringRequestDto;
import org.qubership.atp.itf.lite.backend.feign.dto.RequestDto;
import org.qubership.atp.itf.lite.backend.feign.dto.RequestHeaderDto;
import org.qubership.atp.itf.lite.backend.feign.dto.ResponseDto;
import org.qubership.atp.itf.lite.backend.feign.dto.RestLogRecordDto;
import org.qubership.atp.itf.lite.backend.model.api.request.ImportFromRamRequest;
import org.qubership.atp.itf.lite.backend.model.entities.Cookie;
import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class RamServiceTest {

    @Mock
    private RamTestRunsFeignClient ramTestRunsFeignClient;
    @Mock
    private RamExecutionRequestFeignClient ramExecutionRequestFeignClient;
    @InjectMocks
    private RamService ramService;

    @Test
    public void importCookiesTest_executionRequestIdSpecified_OneTestRunIdInER_cookiesImported() {
        // given
        ImportFromRamRequest importRequest = new ImportFromRamRequest();
        UUID erId = UUID.randomUUID();
        importRequest.setExecutionRequestId(erId);
        UUID testRunId = UUID.randomUUID();
        RestLogRecordDto lr1 = new RestLogRecordDto()
                .request(new RequestDto().endpoint("http://localhost.test/path").headersList(new ArrayList<>()));
        lr1.uuid(UUID.randomUUID());
        lr1.getRequest().getHeadersList().add(
                new RequestHeaderDto().name(Constants.COOKIE_HEADER_KEY).value("Cookie_1=value"));

        RestLogRecordDto lr2 = new RestLogRecordDto()
                .request(new RequestDto().endpoint("http://anotherhost.com/path").headersList(new ArrayList<>()))
                .response(new ResponseDto().headersList(new ArrayList<>()));
        lr2.uuid(UUID.randomUUID());
        lr2.getResponse().getHeadersList().add(
                new RequestHeaderDto().name(Constants.COOKIE_RESP_HEADER_KEY).value("Cookie_2=value"));
        lr2.getResponse().getHeadersList().add(
                new RequestHeaderDto().name(Constants.COOKIE_RESP_HEADER_KEY).value("Cookie_3=value; Domain=test"));

        // when
        when(ramExecutionRequestFeignClient.getAllTestRunIds(eq(erId)))
                .thenReturn(ResponseEntity.ok(Collections.singletonList(testRunId)));
        when(ramTestRunsFeignClient.getAllFilteredLogRecords(eq(testRunId), any(LogRecordFilteringRequestDto.class)))
                .thenReturn(ResponseEntity.ok(Arrays.asList(lr1, lr2)));

        // then
        List<Cookie> importedCookies = ramService.importCookies(importRequest);
        assertEquals(3, importedCookies.size());
        Cookie c1 = importedCookies.get(0);
        assertEquals("localhost.test", c1.getDomain());
        assertEquals("Cookie_1", c1.getKey());
        assertEquals("Cookie_1=value", c1.getValue());

        Cookie c2 = importedCookies.get(1);
        assertEquals("anotherhost.com", c2.getDomain());
        assertEquals("Cookie_2", c2.getKey());
        assertEquals("Cookie_2=value", c2.getValue());

        Cookie c3 = importedCookies.get(2);
        assertEquals("test", c3.getDomain());
        assertEquals("Cookie_3", c3.getKey());
        assertEquals("Cookie_3=value; Domain=test", c3.getValue());
    }

    @Test
    public void importCookiesTest_executionRequestIdSpecified_testRunIdSpecified_cookiesImported() {
        // given
        ImportFromRamRequest importRequest = new ImportFromRamRequest();
        UUID erId = UUID.randomUUID();
        UUID testRunId = UUID.randomUUID();
        importRequest.setExecutionRequestId(erId);
        importRequest.setTestRunId(testRunId);

        RestLogRecordDto lr1 = new RestLogRecordDto()
                .request(new RequestDto().endpoint("http://localhost.test/path").headersList(new ArrayList<>()));
        lr1.uuid(UUID.randomUUID());
        lr1.getRequest().getHeadersList().add(
                new RequestHeaderDto().name(Constants.COOKIE_HEADER_KEY).value("Cookie_1=value"));

        RestLogRecordDto lr2 = new RestLogRecordDto()
                .request(new RequestDto().endpoint("http://anotherhost.com/path").headersList(new ArrayList<>()))
                .response(new ResponseDto().headersList(new ArrayList<>()));
        lr2.uuid(UUID.randomUUID());
        lr2.getResponse().getHeadersList().add(
                new RequestHeaderDto().name(Constants.COOKIE_RESP_HEADER_KEY).value("Cookie_2=value"));
        lr2.getResponse().getHeadersList().add(
                new RequestHeaderDto().name(Constants.COOKIE_RESP_HEADER_KEY).value("Cookie_3=value; Domain=test"));

        // when
        when(ramTestRunsFeignClient.getAllFilteredLogRecords(eq(testRunId), any(LogRecordFilteringRequestDto.class)))
                .thenReturn(ResponseEntity.ok(Arrays.asList(lr1, lr2)));

        // then
        List<Cookie> importedCookies = ramService.importCookies(importRequest);
        assertEquals(3, importedCookies.size());
        Cookie c1 = importedCookies.get(0);
        assertEquals("localhost.test", c1.getDomain());
        assertEquals("Cookie_1", c1.getKey());
        assertEquals("Cookie_1=value", c1.getValue());

        Cookie c2 = importedCookies.get(1);
        assertEquals("anotherhost.com", c2.getDomain());
        assertEquals("Cookie_2", c2.getKey());
        assertEquals("Cookie_2=value", c2.getValue());

        Cookie c3 = importedCookies.get(2);
        assertEquals("test", c3.getDomain());
        assertEquals("Cookie_3", c3.getKey());
        assertEquals("Cookie_3=value; Domain=test", c3.getValue());
    }

    @Test
    public void importCookiesTest_executionRequestIdSpecified_testRunIdSpecified_logRecordIdSpecified_cookiesImported() {
        // given
        ImportFromRamRequest importRequest = new ImportFromRamRequest();
        UUID erId = UUID.randomUUID();
        UUID testRunId = UUID.randomUUID();
        importRequest.setExecutionRequestId(erId);
        importRequest.setTestRunId(testRunId);

        RestLogRecordDto lr1 = new RestLogRecordDto()
                .request(new RequestDto().endpoint("http://localhost.test/path").headersList(new ArrayList<>()));
        lr1.uuid(UUID.randomUUID());
        lr1.getRequest().getHeadersList().add(
                new RequestHeaderDto().name(Constants.COOKIE_HEADER_KEY).value("Cookie_1=value"));

        RestLogRecordDto lr2 = new RestLogRecordDto()
                .request(new RequestDto().endpoint("http://anotherhost.com/path").headersList(new ArrayList<>()))
                .response(new ResponseDto().headersList(new ArrayList<>()));
        lr2.uuid(UUID.randomUUID());
        lr2.getResponse().getHeadersList().add(
                new RequestHeaderDto().name(Constants.COOKIE_RESP_HEADER_KEY).value("Cookie_2=value"));
        lr2.getResponse().getHeadersList().add(
                new RequestHeaderDto().name(Constants.COOKIE_RESP_HEADER_KEY).value("Cookie_3=value; Domain=test"));

        RestLogRecordDto lr3 = new RestLogRecordDto()
                .request(new RequestDto().endpoint("http://localhost.test/path").headersList(new ArrayList<>()));
        lr3.uuid(UUID.randomUUID());
        lr3.getRequest().getHeadersList().add(
                new RequestHeaderDto().name(Constants.COOKIE_HEADER_KEY).value("Cookie_3=value"));

        importRequest.setLogRecordId(lr2.getUuid());

        // when
        when(ramTestRunsFeignClient.getAllFilteredLogRecords(eq(testRunId), any(LogRecordFilteringRequestDto.class)))
                .thenReturn(ResponseEntity.ok(Arrays.asList(lr1, lr2, lr3)));

        // then
        List<Cookie> importedCookies = ramService.importCookies(importRequest);
        assertEquals(3, importedCookies.size());
        Cookie c1 = importedCookies.get(0);
        assertEquals("localhost.test", c1.getDomain());
        assertEquals("Cookie_1", c1.getKey());
        assertEquals("Cookie_1=value", c1.getValue());

        Cookie c2 = importedCookies.get(1);
        assertEquals("anotherhost.com", c2.getDomain());
        assertEquals("Cookie_2", c2.getKey());
        assertEquals("Cookie_2=value", c2.getValue());

        Cookie c3 = importedCookies.get(2);
        assertEquals("test", c3.getDomain());
        assertEquals("Cookie_3", c3.getKey());
        assertEquals("Cookie_3=value; Domain=test", c3.getValue());
    }

    @Test
    public void importCookiesTest_noIds_throwException() {
        // given
        ImportFromRamRequest importRequest = new ImportFromRamRequest();
        // then
        assertThrows(ItfLiteIncorrectImportRequest.class, () -> ramService.importCookies(importRequest));
    }

    @Test
    public void importCookiesTest_executionRequestIdSpecified_TwoTestRunIdInER_throwException() {
        // given
        ImportFromRamRequest importRequest = new ImportFromRamRequest();
        UUID erId = UUID.randomUUID();
        importRequest.setExecutionRequestId(erId);
        UUID testRunId1 = UUID.randomUUID();
        UUID testRunId2 = UUID.randomUUID();

        // when
        when(ramExecutionRequestFeignClient.getAllTestRunIds(eq(erId)))
                .thenReturn(ResponseEntity.ok(Arrays.asList(testRunId1, testRunId2)));

        // then
        assertThrows(ItfLiteIllegalTestRunsCountInExecutionRequestException.class, () ->
                ramService.importCookies(importRequest));
    }


    @Test
    public void importCookiesTest_executionRequestIdSpecified_NoTestRunsInER_throwException() {
        // given
        ImportFromRamRequest importRequest = new ImportFromRamRequest();
        UUID erId = UUID.randomUUID();
        importRequest.setExecutionRequestId(erId);
        // when
        when(ramExecutionRequestFeignClient.getAllTestRunIds(eq(erId)))
                .thenReturn(ResponseEntity.ok(new ArrayList<>()));

        // then
        assertThrows(ItfLiteTestRunsNotFoundException.class, () -> ramService.importCookies(importRequest));
    }

}
