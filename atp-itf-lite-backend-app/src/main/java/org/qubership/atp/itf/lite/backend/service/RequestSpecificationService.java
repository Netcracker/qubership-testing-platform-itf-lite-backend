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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.persistence.criteria.Predicate;

import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RequestSpecificationService {

    private final String requestProjectIdFieldName = "projectId";
    private final String requestFolderIdFieldName = "folderId";
    private final String requestIdFieldName = "id";

    /**
     * Specification to filter requests by project id, folder ids, request ids.
     *
     * @param projectId  project id
     * @param folderIds  folder ids
     * @param requestIds request ids
     * @return specification
     */
    public Specification<Request> generateSpecificationToFilterRequestsByProjectIdFolderIdsRequestIds(
            UUID projectId, Set<UUID> folderIds, Set<UUID> requestIds) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (projectId != null) {
                predicates.add(builder.equal(root.get(requestProjectIdFieldName), projectId));
            }
            if (!CollectionUtils.isEmpty(folderIds)) {
                predicates.add(builder.or(folderIds.stream()
                        .map(value -> builder.equal(root.get(requestFolderIdFieldName), value))
                        .toArray(Predicate[]::new)));
            }
            if (!CollectionUtils.isEmpty(requestIds)) {
                predicates.add(builder.or(requestIds.stream()
                        .map(value -> builder.equal(root.get(requestIdFieldName), value))
                        .toArray(Predicate[]::new)));
            }

            if (predicates.isEmpty()) {
                return builder.conjunction();
            }
            query.orderBy(builder.asc(root.get("order")));
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
