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

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.itf.lite.backend.model.entities.RequestSnapshot;
import org.qubership.atp.itf.lite.backend.model.entities.key.RequestSnapshotKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequestSnapshotRepository extends JpaRepository<RequestSnapshot, RequestSnapshotKey> {

    @Query(value = "SELECT session_id FROM request_snapshots e WHERE ABS(EXTRACT(EPOCH FROM "
            + ":referenceDate - e.created_when)) > :expirationPeriod",
            nativeQuery = true)
    List<RequestSnapshotKey> findAllByCreatedWhenDifferenceGreaterThanReferenceDate(
            @Param("referenceDate") Date referenceDate,
            @Param("expirationPeriod") Long expirationPeriod
    );

    void deleteAllBySessionIdAndRequestIdIn(UUID sessionId, List<UUID> requestId);

    void deleteBySessionIdAndRequestId(UUID sessionId, UUID requestId);
}
