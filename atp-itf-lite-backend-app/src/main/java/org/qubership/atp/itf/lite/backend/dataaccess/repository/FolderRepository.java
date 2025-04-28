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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

@JaversSpringDataAuditable
public interface FolderRepository extends JpaRepository<Folder, UUID>, JpaSpecificationExecutor<Folder>,
        CustomFolderRepository  {

    List<Folder> findAllByProjectIdAndParentId(UUID projectId, UUID parentId);

    List<Folder> findAllByProjectIdAndParentIdOrderByOrder(UUID projectId, UUID parentId);

    List<Folder> findAllByProjectId(UUID projectId);

    List<Folder> findAllByProjectIdOrderByOrder(UUID projectId);

    List<Folder> findAllByIdIn(Set<UUID> ids);

    List<Folder> findAllByIdInOrderByOrder(Set<UUID> ids);

    @Transactional
    void deleteByIdIn(Set<UUID> ids);

    boolean existsFolderByProjectIdAndName(UUID projectId, String name);

    @Query(value = "select max(f.order) from Folder f where f.projectId = :projectId and f.parentId = :parentId")
    Integer findMaxOrder(UUID projectId, UUID parentId);

    @Query(value = "select max(f.order) from Folder f where f.projectId = :projectId")
    Integer findMaxOrder(UUID projectId);

    Folder getByProjectIdAndSourceId(UUID projectId, UUID sourceId);

    List<Folder> findAllByIdAndName(UUID id, String name);

    @Modifying
    @Query(value = "update Folder f set f.isAutoCookieDisabled = :autoCookieDisabled where f.id in :ids")
    void updateAutoCookieDisabledByIdIn(boolean autoCookieDisabled, Set<UUID> ids);
}
