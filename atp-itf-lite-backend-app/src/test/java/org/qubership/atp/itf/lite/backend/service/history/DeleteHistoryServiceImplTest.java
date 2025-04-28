package org.qubership.atp.itf.lite.backend.service.history;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.javers.core.metamodel.object.SnapshotType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.history.JaversCommitPropertyRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.history.JaversCommitRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.history.JaversGlobalIdRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.history.JaversSnapshotRepository;
import org.qubership.atp.itf.lite.backend.model.api.response.history.JaversCountResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.history.JaversCountResponseImpl;
import org.qubership.atp.itf.lite.backend.model.entities.javers.history.JvGlobalIdEntity;
import org.qubership.atp.itf.lite.backend.model.entities.javers.history.JvSnapshotEntity;
import org.qubership.atp.itf.lite.backend.service.history.impl.DeleteHistoryServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class DeleteHistoryServiceImplTest {
    @Mock
    private JaversSnapshotRepository snapshotRepository;
    @Mock
    private JaversGlobalIdRepository globalIdRepository;
    @Mock
    private JaversCommitRepository commitRepository;
    @Mock
    private JaversCommitPropertyRepository commitPropertyRepository;
    @InjectMocks
    private DeleteHistoryServiceImpl deleteHistoryService;

    @Test
    public void deleteTerminatedSnapshots_allEntitiesShouldBeDeleted() {
        // given
        Integer pageSize = 1;
        JvSnapshotEntity snapshotEntity = new JvSnapshotEntity();
        snapshotEntity.setGlobalId(1L);
        snapshotEntity.setCommitId(2L);
        Page<JvSnapshotEntity> page = new PageImpl<>(Collections.singletonList(snapshotEntity));
        List<Long> globalIds = Collections.singletonList(1L);
        List<Long> commitIds = Collections.singletonList(2L);

        // when
        when(snapshotRepository.findAllByTypeIs(eq(SnapshotType.TERMINAL), any(Pageable.class))).thenReturn(page);
        when(snapshotRepository.findAllByGlobalIdIn(any())).thenReturn(Collections.singletonList(snapshotEntity));
        deleteHistoryService.deleteTerminatedSnapshots(pageSize);

        // then
        verify(snapshotRepository, times(1)).deleteByGlobalIdIn(eq(globalIds));
        verify(globalIdRepository, times(1)).deleteByIdIn(eq(globalIds));
        verify(commitPropertyRepository, times(1)).deleteByIdCommitIdIn(eq(commitIds));
        verify(commitRepository, times(1)).deleteByIdIn(eq(commitIds));
    }

    @Test
    public void deleteOldSnapshots_allEntitiesShouldBeDeleted() {
        // given
        final long globalId = 1L;
        final long version = 2L;
        final long commitId = 3L;
        List<JaversCountResponse> oldSnapshots = new ArrayList<>();
        oldSnapshots.add(new JaversCountResponseImpl(globalId, 101L));
        JvSnapshotEntity snapshotToDelete = new JvSnapshotEntity();
        snapshotToDelete.setGlobalId(globalId);
        snapshotToDelete.setVersion(version);
        snapshotToDelete.setCommitId(commitId);
        List<JvSnapshotEntity> listSnapshotsToDelete = Collections.singletonList(snapshotToDelete);
        JvSnapshotEntity firstNotDeletedSnapshot = new JvSnapshotEntity();
        firstNotDeletedSnapshot.setType(SnapshotType.UPDATE);

        // when
        when(snapshotRepository.findGlobalIdAndCountGreaterThan(eq(100L)))
                .thenReturn(oldSnapshots);
        when(snapshotRepository.findAllByGlobalIdOrderByVersionAsc(eq(globalId), any()))
                .thenReturn(listSnapshotsToDelete);
        when(snapshotRepository.countByCommitId(eq(commitId))).thenReturn(0L);
        when(snapshotRepository.findFirstByGlobalIdOrderByVersionAsc(eq(globalId))).thenReturn(firstNotDeletedSnapshot);
        deleteHistoryService.deleteOldSnapshots(100);

        // then
        verify(snapshotRepository, times(1))
                .deleteByVersionAndGlobalIdAndCommitId(eq(version), eq(globalId), eq(commitId));
        verify(commitPropertyRepository, times(1)).deleteByIdCommitId(eq(commitId));
        verify(commitRepository, times(1)).deleteById(eq(commitId));
        ArgumentCaptor<JvSnapshotEntity> snapshotCapture = ArgumentCaptor.forClass(JvSnapshotEntity.class);
        verify(snapshotRepository, times(1)).save(snapshotCapture.capture());
        JvSnapshotEntity updatedSnapshot = snapshotCapture.getValue();
        Assertions.assertEquals(SnapshotType.INITIAL, updatedSnapshot.getType());
    }

    @Test
    public void deleteSnapshotsByEntityIdsTest_allEntitiesShouldBeDeleted() {
        // given
        JvGlobalIdEntity globalIdEntity = new JvGlobalIdEntity();
        globalIdEntity.setId(1L);
        JvSnapshotEntity snapshotEntity = new JvSnapshotEntity();
        snapshotEntity.setGlobalId(1L);
        snapshotEntity.setCommitId(2L);
        List<Long> globalIds = Collections.singletonList(1L);
        List<Long> commitIds = Collections.singletonList(2L);

        // when
        when(globalIdRepository.findAllByEntityIds(any())).thenReturn(Collections.singletonList(globalIdEntity));
        when(snapshotRepository.findAllByGlobalIdIn(any())).thenReturn(Collections.singletonList(snapshotEntity));
        deleteHistoryService.deleteSnapshotsByEntityIds(Sets.newHashSet(UUID.randomUUID()));

        // then
        verify(snapshotRepository, times(1)).deleteByGlobalIdIn(eq(globalIds));
        verify(globalIdRepository, times(1)).deleteByIdIn(eq(globalIds));
        verify(commitPropertyRepository, times(1)).deleteByIdCommitIdIn(eq(commitIds));
        verify(commitRepository, times(1)).deleteByIdIn(eq(commitIds));
    }
}
