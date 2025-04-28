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

package org.qubership.atp.itf.lite.backend.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.itf.lite.backend.model.api.request.IdWithModifiedWhen;
import org.qubership.atp.itf.lite.backend.model.entities.AbstractNamedEntity;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ConcurrentModificationService {

    /**
     * Check modify date was changing.
     *
     * @param modifyDateFromRequest modify date from request
     * @param modifyDateFromMongo   modify date from db
     * @return true if modify date different
     */
    public <T> Boolean isModifyDateChanging(T modifyDateFromRequest, T modifyDateFromMongo) {
        return modifyDateFromRequest != null && !modifyDateFromRequest.equals(modifyDateFromMongo);
    }

    /**
     * Provide http status for concurrent modification.
     *
     * @param entityId       entity uuid from request
     * @param modifyDateFromRequest modify date from request
     * @param service               crud service for entity
     * @param skipTypeCheck         value to skip entity type check
     * @return http status
     */
    public <T extends AbstractNamedEntity> HttpStatus getConcurrentModificationHttpStatus(
            UUID entityId, Date modifyDateFromRequest, CrudService<T> service,
            Boolean skipTypeCheck) {
        AbstractNamedEntity entity = service.get(entityId);
        Boolean isConcurrentModification =
                isModifyDateChanging(modifyDateFromRequest, entity.getModifiedWhen());

        if (Boolean.TRUE.equals(isConcurrentModification)) {
            return (skipTypeCheck == null || Boolean.FALSE.equals(skipTypeCheck))
                    && (entity instanceof Folder || entity instanceof Request)
                    ? HttpStatus.CONFLICT : HttpStatus.IM_USED;
        }
        return HttpStatus.OK;
    }

    /**
     * Provide http status for concurrent modification.
     *
     * @param entityId       entity uuid from request
     * @param modifyDateFromRequest modify date from request
     * @param service               crud service for entity
     * @return http status
     */
    public <T extends AbstractNamedEntity> HttpStatus getConcurrentModificationHttpStatus(
            UUID entityId, Date modifyDateFromRequest, CrudService<T> service) {
        return getConcurrentModificationHttpStatus(entityId, modifyDateFromRequest, service, false);
    }


    /**
     * Provide http status for concurrent modification.
     *
     * @param entityIds       entity uuid from request
     * @param service               crud service for entity
     * @return http status
     */
    public <T extends AbstractNamedEntity> Pair<HttpStatus, List<UUID>> getConcurrentModificationHttpStatus(
            Collection<IdWithModifiedWhen> entityIds, CrudService<T> service) {
        HttpStatus status = HttpStatus.OK;
        List<UUID> concurrentModification = new ArrayList<>();
        if (!CollectionUtils.isEmpty(entityIds)) {
            for (IdWithModifiedWhen id : entityIds) {
                HttpStatus res = getConcurrentModificationHttpStatus(id.getId(), id.getModifiedWhen(), service, false);
                if (!HttpStatus.OK.equals(res)) {
                    status = res;
                    concurrentModification.add(id.getId());
                }
            }
        }
        return Pair.of(status, concurrentModification);
    }

}
