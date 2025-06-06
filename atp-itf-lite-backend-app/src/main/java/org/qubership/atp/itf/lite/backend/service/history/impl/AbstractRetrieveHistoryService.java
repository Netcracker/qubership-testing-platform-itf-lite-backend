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

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;

import org.javers.core.Changes;
import org.javers.core.ChangesByCommit;
import org.javers.core.Javers;
import org.javers.core.commit.CommitId;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.PropertyChange;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.diff.changetype.container.ContainerChange;
import org.javers.core.diff.changetype.container.ValueAdded;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.core.metamodel.object.SnapshotType;
import org.javers.repository.jql.JqlQuery;
import org.javers.repository.jql.QueryBuilder;
import org.javers.shadow.Shadow;
import org.qubership.atp.itf.lite.backend.converters.history.AbstractVersioningMapper;
import org.qubership.atp.itf.lite.backend.enums.Operation;
import org.qubership.atp.itf.lite.backend.feign.dto.history.AbstractCompareEntityDto;
import org.qubership.atp.itf.lite.backend.feign.dto.history.CompareEntityResponseDto;
import org.qubership.atp.itf.lite.backend.feign.dto.history.HistoryItemDto;
import org.qubership.atp.itf.lite.backend.feign.dto.history.HistoryItemResponseDto;
import org.qubership.atp.itf.lite.backend.feign.dto.history.PageInfoDto;
import org.qubership.atp.itf.lite.backend.model.entities.AbstractNamedEntity;
import org.qubership.atp.itf.lite.backend.model.entities.ObjectOperation;
import org.qubership.atp.itf.lite.backend.service.history.iface.RetrieveHistoryService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractRetrieveHistoryService<S extends AbstractNamedEntity, D extends AbstractCompareEntityDto>
        implements RetrieveHistoryService {

    private final Javers javers;
    protected final AbstractVersioningMapper<S, D> abstractVersioningMapper;

    public AbstractRetrieveHistoryService(Javers javers, AbstractVersioningMapper<S, D> abstractVersioningMapper) {
        this.abstractVersioningMapper = abstractVersioningMapper;
        this.javers = javers;
    }

    abstract AbstractVersioningMapper<S, D> getMapper();

    /**
     * Finds all history of changes for provided entity id.
     *
     * @param id     object id of requested entity
     * @param offset index of the first element in collection
     * @param limit  number of items in collection
     * @return list of changes
     */
    public HistoryItemResponseDto getAllHistory(UUID id, Integer offset, Integer limit) {

        log.debug(String.format("Get All History for entity = %s, offset = %s, limit = %s", id, offset, limit));

        List<CdoSnapshot> snapshots = javers.findSnapshots(getSnapshotsByLimit(id, offset, limit));
        log.debug(String.format("Snapshots found for entity = %s, snapshots = %s", id, snapshots));

        JqlQuery query = getChangesByIdPaginationQuery(id, snapshots.stream()
                .map(snapshot -> snapshot.getCommitId().valueAsNumber()).collect(Collectors.toList()));

        Changes changes = javers.findChanges(query);
        log.debug(String.format("Changes found for entity = %s,  changes = %s", id, changes.prettyPrint()));

        List<ChangesByCommit> changesByCommits = changes.groupByCommit();

        List<HistoryItemDto> historyItemDtoList = changesByCommits
                .stream()
                .map(changesByCommit -> createHistoryItem(changesByCommit, snapshots))
                .collect(Collectors.toList());

        HistoryItemResponseDto response = new HistoryItemResponseDto();
        response.setHistoryItems(historyItemDtoList);
        response.setPageInfo(getPageInfo(id, offset, limit));

        return response;
    }

    /**
     * Returns collection of entities with requested revision numbers.
     *
     * @param id       uuid of entity in DB.
     * @param versions collection of requested revision numbers.
     * @return collection of CompareEntityResponse.
     */
    @Override
    public List<CompareEntityResponseDto> getEntitiesByVersions(UUID id, List<String> versions) {
        return versions.stream()
                .map(version -> getEntityByVersion(version, id))
                .collect(Collectors.toList());
    }

    protected JqlQuery getChangesByIdPaginationQuery(UUID id, Collection<BigDecimal> commitIds) {
        return QueryBuilder.byInstanceId(id, getEntityClass())
                .withNewObjectChanges()
                .withChildValueObjects()
                .withCommitIds(commitIds)
                .build();
    }

    protected JqlQuery getSnapshotsByLimit(UUID id, Integer offset, Integer limit) {
        return QueryBuilder.byInstanceId(id, getEntityClass())
                .skip(offset)
                .limit(limit)
                .build();
    }

    protected JqlQuery getChangesByIdQuery(UUID id) {
        return QueryBuilder.byInstanceId(id, getEntityClass())
                .limit(Integer.MAX_VALUE)
                .withNewObjectChanges()
                .build();
    }

    protected Integer getVersionByCommitId(List<CdoSnapshot> snapshots, CommitId id) {
        Integer version = null;
        Optional<CdoSnapshot> snapshot = snapshots
                .stream()
                .filter(cdoSnapshot -> cdoSnapshot.getCommitId().equals(id))
                .findFirst();

        if (snapshot.isPresent()) {
            version = Long.valueOf(snapshot.get().getVersion()).intValue();
        }

        return version;
    }

    protected void setCommonFields(AbstractNamedEntity source, AbstractCompareEntityDto destination,
                                   CommitMetadata commitMetadata) {
        destination.setModifiedBy(commitMetadata.getAuthor());
        if (Objects.nonNull(source.getModifiedWhen())) {
            destination.setModifiedWhen(source.getModifiedWhen().getTime());
        }
        if (Objects.nonNull(source.getCreatedWhen())) {
            destination.setCreatedWhen(source.getCreatedWhen().getTime());
        }
        List<CdoSnapshot> snapshots = javers.findSnapshots(QueryBuilder
                .byInstanceId(source.getId(), source.getClass())
                .withSnapshotType(SnapshotType.INITIAL)
                .build());
        if (Objects.nonNull(snapshots) && snapshots.size() > 0) {
            destination.setCreatedBy(snapshots.get(0).getCommitMetadata().getAuthor());
        }
    }

    protected void processChildChanges(Change propertyChange, HistoryItemDto historyItemDto) {
        ContainerChange containerChange = (ContainerChange) propertyChange;
        List<ValueAdded> valueAddedChanges = containerChange.getValueAddedChanges();
        List<ObjectOperation> objectOperations = valueAddedChanges.stream()
                .map(ValueAdded::getAddedValue)
                .filter(o -> o instanceof ObjectOperation)
                .map(o -> (ObjectOperation) o)
                .collect(Collectors.toList());
        historyItemDto.setAdded(getOperationNames(objectOperations, Operation.ADD));
        historyItemDto.setDeleted(getOperationNames(objectOperations, Operation.REMOVE));
    }

    private List<String> calculateCommonChanges(List<Change> changes) {
        return changes
                .stream()
                .map(change -> {
                    if (!getEntityClass().getTypeName().equals(change.getAffectedGlobalId().getTypeName())
                            && change instanceof ValueChange) {
                        String propertyNameWithPath = ((ValueChange) change).getPropertyNameWithPath();
                        return propertyNameWithPath.split("/")[0].split("\\.")[0];
                    } else {
                        return ((PropertyChange) change).getPropertyName();
                    }
                })
                .distinct()
                .collect(Collectors.toList());
    }

    private HistoryItemDto createHistoryItem(ChangesByCommit changesByCommit, List<CdoSnapshot> snapshots) {

        HistoryItemDto historyItemDto = new HistoryItemDto();
        CommitMetadata commit = changesByCommit.getCommit();
        historyItemDto.setVersion(getVersionByCommitId(snapshots, commit.getId()));
        historyItemDto.setType(getItemType());
        historyItemDto.setModifiedWhen(commit.getCommitDate().atOffset(ZoneOffset.UTC).toString());
        historyItemDto.setModifiedBy(defaultIfEmpty(commit.getAuthor(), EMPTY));

        Map<Boolean, List<Change>> partitions = changesByCommit.get()
                .stream()
                .filter(change -> change instanceof PropertyChange)
                .collect(Collectors.partitioningBy(change ->
                        CHILD_ACTIONS_PROPERTY.equals(((PropertyChange) change).getPropertyName())));

        historyItemDto.setChanged(calculateCommonChanges(partitions.get(false)));

        Optional<Change> childChanges = partitions.get(true).stream().findFirst();
        childChanges.ifPresent(change -> processChildChanges(change, historyItemDto));

        return historyItemDto;
    }

    public Integer getCountOfCommits(UUID id) {
        Changes changes = javers.findChanges(getChangesByIdQuery(id));
        return changes.groupByCommit().size();
    }

    private CompareEntityResponseDto getEntityByVersion(String version, UUID uuid) {
        Optional<Shadow<Object>> entity = getShadow(version, uuid);
        if (entity.isPresent()) {
            return buildCompareEntity(version, entity.get());
        } else {
            log.error("Failed to found entity with id: {}", uuid);
            throw new EntityNotFoundException("Failed to found shadow with id: " + uuid);
        }
    }

    private List<String> getOperationNames(List<ObjectOperation> objectOperations, Operation operation) {
        return objectOperations.stream()
                .filter(obj -> operation.equals(obj.getOperationType()))
                .map(ObjectOperation::getName)
                .collect(Collectors.toList());
    }

    private PageInfoDto getPageInfo(UUID id, Integer offset, Integer limit) {
        PageInfoDto pageInfo = new PageInfoDto();
        pageInfo.setOffset(offset);
        pageInfo.setLimit(limit);
        pageInfo.setItemsTotalCount(this.getCountOfCommits(id));
        return pageInfo;
    }

    private Optional<Shadow<Object>> getShadow(String version, UUID uuid) {
        JqlQuery query = QueryBuilder.byInstanceId(uuid, getEntityClass())
                .withVersion(Long.parseLong(version))
                .withScopeDeepPlus()
                .build();
        List<CdoSnapshot> snapshots = javers.findSnapshots(query);
        QueryBuilder queryBuilder = QueryBuilder.byInstanceId(uuid, getEntityClass())
                .withVersion(Long.parseLong(version)).withScopeDeepPlus();
        if (Objects.nonNull(snapshots) && snapshots.size() > 0) {
            queryBuilder.withCommitId(snapshots.get(0).getCommitId());
        }
        List<Shadow<Object>> shadows = javers.findShadows(queryBuilder.build());
        log.debug("Shadows found : {}", shadows);
        return shadows.stream().findFirst();
    }
}
