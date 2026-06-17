/*
 * # Copyright 2024-2026 NetCracker Technology Corporation
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

package org.qubership.atp.itf.lite.backend.dataaccess.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomFolderRepositoryImpl implements CustomFolderRepository {

    private final EntityManager em;

    /**
     * Get Folder id by path.
     */
    public UUID getFolderIdPyPath(UUID projectId, List<String> path) {
        if (CollectionUtils.isEmpty(path)) {
            throw new IllegalArgumentException("path can not be empty");
        }

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<UUID> cq = cb.createQuery(UUID.class);
        Root<Folder> folder = cq.from(Folder.class);
        cq.select(folder.get("id"));
        if (path.size() == 1) {
            cq.where(cb.and(
                    cb.equal(folder.get("name"), path.getFirst()),
                    cb.equal(folder.get("projectId"), projectId)),
                    cb.isNull(folder.get("parentId")));
        } else {
            Subquery<UUID> subquery = getParentFolder(cb, cq.subquery(UUID.class), path, path.size() - 2, projectId);
            cq.where(cb.and(
                    cb.equal(folder.get("name"), path.getLast()),
                    folder.get("parentId").in(subquery)));
        }

        cq.orderBy(cb.asc(folder.get("order")));
        try {
            return em.createQuery(cq).setMaxResults(1).getSingleResult();
        } finally {
            em.clear();
        }
    }

    private Subquery<UUID> getParentFolder(CriteriaBuilder cb, Subquery<UUID> parentFolderSq, List<String> path,
                                           int currentIndex, UUID projectId) {
        Root<Folder> parentFolder = parentFolderSq.from(Folder.class);
        parentFolderSq.select(parentFolder.get("id"));
        if (currentIndex == 0) {
            return parentFolderSq.where(cb.and(
                    cb.equal(parentFolder.get("name"), path.get(currentIndex)),
                    cb.equal(parentFolder.get("projectId"), projectId)),
                    cb.isNull(parentFolder.get("parentId")));
        } else {
            Subquery<UUID> subquery = getParentFolder(cb, parentFolderSq.subquery(UUID.class), path,
                    currentIndex - 1, projectId);
            return parentFolderSq.where(cb.and(
                    cb.equal(parentFolder.get("name"), path.get(currentIndex)),
                    parentFolder.get("parentId").in(subquery)));
        }
    }

    /**
     * Collect folder heirs ids with parent folder ids.
     * @param ids parent folder ids
     * @return Set of collected ids
     */
    public Set<UUID> findHeirsIdsByIdIn(Collection<UUID> ids) {
        Query query = em.createNativeQuery("""
                WITH RECURSIVE heirs(id, parent_id) AS (
                	SELECT folder.id, folder.parent_id
                	FROM public.folders folder
                	WHERE folder.id IN :ids
                	UNION ALL
                	SELECT child.id, child.parent_id
                	FROM public.folders child
                	JOIN heirs parent
                	ON parent.id = child.parent_id
                )
                SELECT CAST(id as text) from heirs""");
        query.setParameter("ids", ids);

        try {
            List<String> result = query.getResultList();
            return result.stream().map(UUID::fromString).collect(Collectors.toSet());
        } finally {
            em.clear();
        }
    }
}
