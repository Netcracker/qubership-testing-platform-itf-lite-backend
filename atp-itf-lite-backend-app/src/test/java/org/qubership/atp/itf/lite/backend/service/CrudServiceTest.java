package org.qubership.atp.itf.lite.backend.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.itf.lite.backend.model.entities.AbstractNamedEntity;
import org.springframework.data.jpa.repository.JpaRepository;

@ExtendWith(MockitoExtension.class)
public class CrudServiceTest {

    private final ThreadLocal<CrudService<AbstractNamedEntity>> crudService = new ThreadLocal<>();
    private final ThreadLocal<JpaRepository<AbstractNamedEntity, UUID>> repository = new ThreadLocal<>();

    @BeforeEach
    public void setUp() {
        crudService.set(mock(CrudService.class));
        repository.set(mock(JpaRepository.class));
    }

    @Test
    public void isEntityExists_whenEntityExists_shouldReturnTrue() {
        // given
        // when
        when(crudService.get().repository()).thenReturn(repository.get());
        when(crudService.get().repository().existsById(any())).thenReturn(true);
        when(crudService.get().isEntityExists(any())).thenCallRealMethod();
        boolean isExists = crudService.get().isEntityExists(UUID.randomUUID());
        // then
        assertTrue(isExists);
    }

    @Test
    public void isEntityExists_whenEntityDoesNotExist_shouldReturnFalse() {
        // given
        // when
        when(crudService.get().repository()).thenReturn(repository.get());
        when(crudService.get().repository().existsById(any())).thenReturn(false);
        when(crudService.get().isEntityExists(any())).thenCallRealMethod();
        boolean isExists = crudService.get().isEntityExists(UUID.randomUUID());
        // then
        assertFalse(isExists);
    }
}
