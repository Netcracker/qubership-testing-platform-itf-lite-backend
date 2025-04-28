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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

import org.hibernate.Hibernate;
import org.javers.core.Javers;
import org.javers.core.metamodel.annotation.DiffInclude;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.repository.jql.JqlQuery;
import org.javers.repository.jql.QueryBuilder;
import org.javers.shadow.Shadow;
import org.modelmapper.ModelMapper;
import org.qubership.atp.itf.lite.backend.exceptions.history.ItfLiteRevisionHistoryIncorrectClassException;
import org.qubership.atp.itf.lite.backend.exceptions.history.ItfLiteRevisionHistoryNotFoundException;
import org.qubership.atp.itf.lite.backend.model.entities.AbstractNamedEntity;
import org.qubership.atp.itf.lite.backend.service.history.iface.EntityHistoryService;
import org.qubership.atp.itf.lite.backend.service.history.iface.RestoreHistoryService;
import org.springframework.util.CollectionUtils;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractRestoreHistoryService<E extends AbstractNamedEntity> implements RestoreHistoryService {

    protected final Javers javers;
    protected final EntityHistoryService<E> entityHistoryService;
    private final ValidateReferenceExistsService validateReferenceExistsService;
    protected final ModelMapper modelMapper;

    public static final Predicate<Field> IS_DIFFINLCUDE_ANNOTATED_PROPERTY_FILTER =
            field -> field.isAnnotationPresent(DiffInclude.class);

    /**
     * Restores the object to a state defined by revision number.
     *
     * @param id         of object being restored.
     * @param revisionId revision number to restore.
     */
    public AbstractNamedEntity restoreToRevision(UUID id, long revisionId) {
        List<Shadow<E>> shadows = getShadows(id, revisionId);
        if (CollectionUtils.isEmpty(shadows)) {
            throw new ItfLiteRevisionHistoryNotFoundException(getItemType().toString(),
                    Long.toString(revisionId), id.toString());
        }
        Shadow<E> objectShadow = shadows.iterator().next();
        updateObjectWithChild(objectShadow);

        E actualObject = Hibernate.unproxy(getObject(id), (Class<E>) getEntityClass());
        validateReferenceExistsService.validateEntity(actualObject);

        return saveRestoredObject((E) restoreValues(objectShadow, actualObject));
    }

    protected Object restoreValues(Shadow<E> shadow, E actualObject) {

        E snapshot = shadow.get();
        if (!actualObject.getClass().equals(snapshot.getClass())) {
            throw new ItfLiteRevisionHistoryIncorrectClassException(snapshot.getClass().toString(),
                    actualObject.getClass().toString());
        }

        copyValues(snapshot, actualObject);
        return actualObject;
    }

    protected List<Predicate<Field>> getPredicates() {
        return Arrays.asList(IS_DIFFINLCUDE_ANNOTATED_PROPERTY_FILTER);
    }

    public E getObject(UUID id) {
        return entityHistoryService.get(id);
    }

    public AbstractNamedEntity saveRestoredObject(E object) {
        return entityHistoryService.restore(object);
    }

    abstract void updateObjectWithChild(Shadow<E> object);

    /**
     * Find shadows by snapshot commit id.
     * @param id entity id.
     * @param revisionId version.
     * @return list of shadows.
     */
    public List<Shadow<E>> getShadows(UUID id, long revisionId) {
        JqlQuery query = QueryBuilder.byInstanceId(id, getEntityClass())
                .withVersion(revisionId)
                .build();
        List<CdoSnapshot> snapshots = javers.findSnapshots(query);
        QueryBuilder queryBuilder = QueryBuilder
                .byInstanceId(id, getEntityClass())
                .withVersion(revisionId)
                .withScopeDeepPlus();
        if (Objects.nonNull(snapshots) && snapshots.size() > 0) {
            queryBuilder.withCommitId(snapshots.get(0).getCommitId());
        }
        return javers.findShadows(queryBuilder.build());
    }

    public abstract List<Shadow<Object>> getChildShadows(Shadow<E> parentShadow, Class targetObject);

    protected abstract void copyValues(E shadow, E actualObject);
}
