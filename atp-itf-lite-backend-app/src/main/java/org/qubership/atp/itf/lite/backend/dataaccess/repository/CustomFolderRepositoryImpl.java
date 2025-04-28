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

package org.qubership.atp.itf.lite.backend.dataaccess.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

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
                    cb.equal(folder.get("name"), path.get(0)),
                    cb.equal(folder.get("projectId"), projectId)),
                    cb.isNull(folder.get("parentId")));
        } else {
            Subquery<UUID> subquery = getParentFolder(cb, cq.subquery(UUID.class), path, path.size() - 2, projectId);
            cq.where(cb.and(
                    cb.equal(folder.get("name"), path.get(path.size() - 1)),
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
     * @return list of collected ids
     */
    public Set<UUID> findHeirsIdsByIdIn(Collection<UUID> ids) {
        Query query = em.createNativeQuery("WITH RECURSIVE heirs(id, parent_id) AS (\n"
                + "\tSELECT folder.id, folder.parent_id\n"
                + "\tFROM public.folders folder\n"
                + "\tWHERE folder.id IN :ids\n"
                + "\tUNION ALL\n"
                + "\tSELECT child.id, child.parent_id\n"
                + "\tFROM public.folders child\n"
                + "\tJOIN heirs parent\n"
                + "\tON parent.id = child.parent_id\n"
                + ")\n"
                + "SELECT CAST(id as text) from heirs");
        query.setParameter("ids", ids);

        try {
            List<String> result = query.getResultList();
            return result.stream().map(UUID::fromString).collect(Collectors.toSet());
        } finally {
            em.clear();
        }
    }
}
