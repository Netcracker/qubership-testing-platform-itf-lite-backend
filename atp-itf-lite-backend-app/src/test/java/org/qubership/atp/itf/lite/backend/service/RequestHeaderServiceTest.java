package org.qubership.atp.itf.lite.backend.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestHeaderRepository;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;

@ExtendWith(MockitoExtension.class)
class RequestHeaderServiceTest {

    @Mock
    private RequestHeaderRepository repository;
    @InjectMocks
    private RequestHeaderService service;

    @Test
    public void disableArgumentSet_disableRequestHeader_headerShouldBeDisabled() {
        // given
        final UUID headerId = UUID.randomUUID();
        RequestHeader header = new RequestHeader(headerId, "Content-Type", "application/json", "Content type header", false);
        // when
        when(repository.findById(headerId)).thenReturn(Optional.of(header));
        service.disableRequestHeader(headerId);

        // then
        ArgumentCaptor<RequestHeader> headerCaptor = ArgumentCaptor.forClass(RequestHeader.class);
        verify(repository, times(1)).save(headerCaptor.capture());

        RequestHeader savedHeader = headerCaptor.getValue();

        assertNotNull(savedHeader, "Saved header shouldn't be null");
        assertTrue(savedHeader.isDisabled(), "Saved header should be disabled");
    }

    @Test
    public void enableArgumentSet_enableRequestHeader_headerShouldBeEnabled() {
        // given
        final UUID headerId = UUID.randomUUID();
        RequestHeader header = new RequestHeader(headerId, "Content-Type", "application/json", "Content type header", false);
        // when
        when(repository.findById(headerId)).thenReturn(Optional.of(header));
        service.enableRequestHeader(headerId);

        // then
        ArgumentCaptor<RequestHeader> headerCaptor = ArgumentCaptor.forClass(RequestHeader.class);
        verify(repository, times(1)).save(headerCaptor.capture());

        RequestHeader savedHeader = headerCaptor.getValue();

        assertNotNull(savedHeader, "Saved header shouldn't be null");
        assertFalse(savedHeader.isDisabled(), "Saved header shouldn't be disabled");
    }
}