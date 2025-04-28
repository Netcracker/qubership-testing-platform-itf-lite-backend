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
import java.util.UUID;

import javax.persistence.criteria.Predicate;

import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class FolderSpecificationService {
    private final String folderProjectIdFieldName = "projectId";
    private final String folderParentIdFieldName = "parentId";

    /**
     * Specification to filter folders by project id, parent folder id.
     *
     * @param projectId      project id
     * @param parentFolderId parent folder id
     * @return specification
     */
    public Specification<Folder> generateSpecificationToFilterFoldersByProjectIdAndParentFolderId(
            UUID projectId, UUID parentFolderId) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (projectId != null) {
                predicates.add(builder.equal(root.get(folderProjectIdFieldName), projectId));
            }
            if (parentFolderId != null) {
                predicates.add(builder.equal(root.get(folderParentIdFieldName), parentFolderId));
            }

            if (predicates.isEmpty()) {
                return builder.conjunction();
            }
            query.orderBy(builder.asc(root.get("order")));
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
