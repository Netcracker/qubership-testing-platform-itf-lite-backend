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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.transaction.Transactional;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.model.RequestRuntimeOptions;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

@JaversSpringDataAuditable
public interface RequestRepository extends JpaRepository<Request, UUID>, JpaSpecificationExecutor<Request> {

    List<Request> findAllByProjectId(UUID projectId);

    Optional<Request> findByProjectIdAndId(UUID projectId, UUID id);

    @Query(value = "select new org.qubership.atp.itf.lite.backend.model.RequestRuntimeOptions("
            + "r.disableSslCertificateVerification, "
            + "r.disableSslClientCertificate, "
            + "r.disableFollowingRedirect, "
            + "r.disableAutoEncoding) "
            + "from Request r where r.id = :id")
    Optional<RequestRuntimeOptions> getRequestRuntimeOptionsById(UUID id);

    List<Request> findAllByProjectIdAndFolderId(UUID projectId, UUID folderId);

    List<Request> findAllByFolderIdIn(Set<UUID> folderIds);

    List<Request> findAllByProjectIdAndNameContains(UUID projectId, String name);

    List<Request> findAllByFolderId(UUID folderId);

    List<Request> findAllByFolderIdOrderByOrder(UUID folderId);

    List<Request> findAllByIdIn(Set<UUID> requestIds);

    @Transactional
    void deleteByIdIn(Set<UUID> requestIds);

    List<Request> findAllByProjectIdAndIdIn(UUID projectId, Set<UUID> requestIds);

    List<Request> findAllByProjectIdAndIdInOrderByOrder(UUID projectId, Set<UUID> requestIds);

    @Query(value = "select r.transportType from Request r where r.id = :id")
    TransportType findTransportType(UUID id);

    @Query(value = "select max(r.order) from Request r where r.projectId = :projectId and r.folderId = :folderId")
    Integer findMaxOrder(UUID projectId, UUID folderId);

    @Query(value = "select max(r.order) from Request r where r.projectId = :projectId")
    Integer findMaxOrder(UUID projectId);

    Request getByProjectIdAndSourceId(UUID projectId, UUID sourceId);

    List<Request> findAllByName(String name);

    @Modifying
    @Query(value = "update Request r set r.isAutoCookieDisabled = :autoCookieDisabled where r.folderId in :ids")
    void updateAutoCookieDisabledByFolderIdIn(boolean autoCookieDisabled, Set<UUID> ids);
}
