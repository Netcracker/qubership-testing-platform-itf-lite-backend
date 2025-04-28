package org.qubership.atp.itf.lite.backend.service.rest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
public class RestTemplateServiceTest {

    private static HttpClientService clientService;
    private static RestTemplateService templateService;

    @BeforeEach
    public void setUp() {
        clientService = mock(HttpClientService.class);
        templateService = new RestTemplateService(clientService);
    }

    @Test
    public void restTemplate() {
        //given
        UUID projectID = UUID.randomUUID();
        CloseableHttpClient httpClient = HttpClients.createDefault();
        //when
        when(clientService.getHttpClient(any())).thenReturn(httpClient);

        RestTemplate result = templateService.restTemplate(projectID);

        // then
        assertNotNull(result);
    }
}
