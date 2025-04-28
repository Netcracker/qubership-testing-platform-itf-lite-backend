/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 */

package org.qubership.atp.itf.lite.backend.service.history.impl;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.javers.core.metamodel.object.SnapshotType;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.history.JaversCommitPropertyRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.history.JaversCommitRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.history.JaversGlobalIdRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.history.JaversSnapshotRepository;
import org.qubership.atp.itf.lite.backend.model.api.response.history.JaversCountResponse;
import org.qubership.atp.itf.lite.backend.model.entities.javers.history.JvGlobalIdEntity;
import org.qubership.atp.itf.lite.backend.model.entities.javers.history.JvSnapshotEntity;
import org.qubership.atp.itf.lite.backend.service.history.iface.DeleteHistoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Iterators;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeleteHistoryServiceImpl implements DeleteHistoryService {

    private static final Integer FIRST_PAGE = 0;

    private final JaversSnapshotRepository snapshotRepository;
    private final JaversGlobalIdRepository globalIdRepository;
    private final JaversCommitRepository commitRepository;
    private final JaversCommitPropertyRepository commitPropertyRepository;

    @Transactional(rollbackFor = Exception.class)
    public void deleteOldAndUpdateAsInitial(Long globalId, List<JvSnapshotEntity> snapshots) {
        snapshots.forEach(snapshot -> deleteOldSnapshot(globalId, snapshot));
        findTheOldestSnapshotByGlobalIdAndUpdateTypeAsInitial(globalId);
    }

    private void deleteOldSnapshot(Long globalId, JvSnapshotEntity snapshot) {
        final Long commitId = snapshot.getCommitId();
        final Long version = snapshot.getVersion();
        snapshotRepository.deleteByVersionAndGlobalIdAndCommitId(version, globalId, commitId);
        log.debug("Deleted snapshots with version '{}', globalId '{}', commitId '{}'", version, globalId, commitId);
        Long commitCount = snapshotRepository.countByCommitId(commitId);
        // remove all commits and properties by commitId if they are no longer referenced by snapshots
        if (commitCount.equals(0L)) {
            commitPropertyRepository.deleteByIdCommitId(commitId);
            commitRepository.deleteById(commitId);
            log.debug("Deleted commit properties and commits with commitId '{}'", commitId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOldSnapshots(long maxRevisionCount) {
        findGlobalIdAndCount(maxRevisionCount).forEach(response -> {
            final Long globalId = response.getId();
            final long numberOfOldSnapshots = response.getCount() - maxRevisionCount;
            final List<JvSnapshotEntity> oldSnapshots = findOldSnapshots(globalId, numberOfOldSnapshots);
            deleteOldAndUpdateAsInitial(globalId, oldSnapshots);
        });
    }

    /**
     * Delete terminated snapshots.
     * @param pageSize number of snapshots deleted in one step.
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTerminatedSnapshots(Integer pageSize) {
        while (true) {
            Page<JvSnapshotEntity> page =
                    snapshotRepository.findAllByTypeIs(SnapshotType.TERMINAL, PageRequest.of(FIRST_PAGE, pageSize));
            deleteTerminatedSnapshots(page);
            if (!page.hasNext()) {
                break;
            }
        }
    }

    private void deleteTerminatedSnapshots(Page<JvSnapshotEntity> page) {
        List<JvSnapshotEntity> terminalSnapshots = page.getContent();
        Set<Long> globalIds = getIds(terminalSnapshots, JvSnapshotEntity::getGlobalId);
        log.debug("Number of terminal globalIds '{}'", globalIds.size());
        List<JvSnapshotEntity> snapshots = new ArrayList<>();
        doAction(globalIds, ids -> snapshots.addAll(snapshotRepository.findAllByGlobalIdIn(ids)));
        Set<Long> commitIds = getIds(snapshots, JvSnapshotEntity::getCommitId);
        log.debug("Number of terminal commitIds '{}'", commitIds.size());
        doAction(globalIds, snapshotRepository::deleteByGlobalIdIn);
        log.debug("Terminated snapshots deleted");
        doAction(globalIds, globalIdRepository::deleteByIdIn);
        log.debug("Terminated globalIds deleted");
        doAction(commitIds, commitPropertyRepository::deleteByIdCommitIdIn);
        log.debug("Terminated commit properties deleted");
        doAction(commitIds, commitRepository::deleteByIdIn);
        log.debug("Terminated commits deleted");
    }

    /**
     * Delete snapshots related with entityId.
     * @param entityIds entityIds for history cleanup.
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteSnapshotsByEntityIds(Set<UUID> entityIds) {
        List<JvGlobalIdEntity> globalIdEntities = globalIdRepository.findAllByEntityIds(
                entityIds.stream().map(entityId -> "\"" + entityId.toString() + "\"").collect(Collectors.toSet()));
        Set<Long> globalIds = globalIdEntities.stream().map(JvGlobalIdEntity::getId).collect(Collectors.toSet());
        log.debug("Number of globalIds '{}' for entityIds: {}", globalIdEntities.size(), entityIds);
        List<JvSnapshotEntity> snapshots = new ArrayList<>();
        doAction(globalIds, ids -> snapshots.addAll(snapshotRepository.findAllByGlobalIdIn(ids)));
        Set<Long> commitIds = getIds(snapshots, JvSnapshotEntity::getCommitId);
        log.debug("Number of commitIds '{}' for entityIds: {}", commitIds.size(), entityIds);
        doAction(globalIds, snapshotRepository::deleteByGlobalIdIn);
        log.debug("Snapshots deleted for entityIds: {}", entityIds);
        doAction(commitIds, snapshotRepository::deleteByCommitIdIn);
        log.debug("Snapshots deleted for commitIds: {}", commitIds);
        doAction(globalIds, globalIdRepository::deleteByIdIn);
        log.debug("GlobalIds deleted for entityIds: {}", entityIds);
        doAction(commitIds, commitPropertyRepository::deleteByIdCommitIdIn);
        log.debug("Commit properties deleted for entityIds: {}", entityIds);
        doAction(commitIds, commitRepository::deleteByIdIn);
        log.debug("Commits deleted for entityIds: {}", entityIds);
    }

    /**
     * Get the oldest snapshot and update snapshot type with INITIAL value.
     *
     * @param globalId globalId
     * @return {@link JvSnapshotEntity} entity;
     */
    private JvSnapshotEntity findTheOldestSnapshotByGlobalIdAndUpdateTypeAsInitial(Long globalId) {
        JvSnapshotEntity snapshot = snapshotRepository.findFirstByGlobalIdOrderByVersionAsc(globalId);
        if (isNull(snapshot)) {
            return null;
        }
        snapshot.setType(SnapshotType.INITIAL);
        return snapshotRepository.save(snapshot);
    }

    private List<JvSnapshotEntity> findOldSnapshots(Long globalId, Long count) {
        PageRequest pageRequest = PageRequest.of(0, Math.toIntExact(count));
        List<JvSnapshotEntity> oldSnapshots =
                snapshotRepository.findAllByGlobalIdOrderByVersionAsc(globalId, pageRequest);
        log.debug("Number of old snapshots '{}' for globalId '{}'", oldSnapshots.size(), globalId);
        return oldSnapshots;
    }

    /**
     * Get globalId and number of old objects.
     *
     * @param maxRevisionCount number of the last revisions.
     * @return {@link List} of {@link JaversCountResponse}
     */
    private List<JaversCountResponse> findGlobalIdAndCount(long maxRevisionCount) {
        List<JaversCountResponse> response = snapshotRepository.findGlobalIdAndCountGreaterThan(maxRevisionCount);
        log.debug("Number of unique globalId '{}'", response.size());
        return response;
    }

    private <T> void doAction(Collection<T> collection, Consumer<? super List<T>> action) {
        Iterators.partition(collection.iterator(), 100).forEachRemaining(action);
    }

    private <T, R> Set<R> getIds(List<T> snapshots, Function<T, R> function) {
        return snapshots.stream()
                .map(function)
                .collect(Collectors.toSet());
    }
}
