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
import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestParamRepository;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestParam;

@ExtendWith(MockitoExtension.class)
class RequestParamServiceTest {

    @Mock
    private RequestParamRepository repository;
    @InjectMocks
    private RequestParamService service;

    @Test
    public void disableArgumentSet_disableRequestParam_paramShouldBeDisabled() {
        // given
        final UUID paramId = UUID.randomUUID();
        RequestParam param = new RequestParam(paramId, "sorted", "true", "Sor param", false);

        // when
        when(repository.findById(paramId)).thenReturn(Optional.of(param));
        service.disableRequestParam(paramId);

        // then
        ArgumentCaptor<RequestParam> paramCaptor = ArgumentCaptor.forClass(RequestParam.class);
        verify(repository, times(1)).save(paramCaptor.capture());

        RequestParam savedParam = paramCaptor.getValue();

        assertNotNull(savedParam, "Saved param shouldn't be null");
        assertTrue(savedParam.isDisabled(), "Saved param should be disabled");
    }

    @Test
    public void enableArgumentSet_enableRequestParam_paramShouldBeEnabled() {
        // given
        final UUID paramId = UUID.randomUUID();
        RequestParam param = new RequestParam(paramId, "sorted", "true", "Sor param", false);

        // when
        when(repository.findById(paramId)).thenReturn(Optional.of(param));
        service.enableRequestParam(paramId);

        // then
        ArgumentCaptor<RequestParam> paramCaptor = ArgumentCaptor.forClass(RequestParam.class);
        verify(repository, times(1)).save(paramCaptor.capture());

        RequestParam savedParam = paramCaptor.getValue();

        assertNotNull(savedParam, "Saved param shouldn't be null");
        assertFalse(savedParam.isDisabled(), "Saved param shouldn't be disabled");
    }
}