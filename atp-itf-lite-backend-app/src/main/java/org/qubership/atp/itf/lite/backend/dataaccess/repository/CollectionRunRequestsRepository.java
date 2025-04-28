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

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.itf.lite.backend.model.entities.collection.run.CollectionRunRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CollectionRunRequestsRepository extends JpaRepository<CollectionRunRequest, UUID> {

    @Modifying
    @Transactional
    @Query("DELETE FROM CollectionRunRequest cr WHERE cr.createdWhen < :createdWhen")
    int deleteByCreatedWhenBefore(@Param("createdWhen") Timestamp createdWhen);

    boolean existsByCollectionRunIdAndRequestId(UUID testRunId, UUID requestId);

    CollectionRunRequest findFirstByCollectionRunIdAndRequestNameOrderByOrderDesc(UUID testRunId, String requestName);

    CollectionRunRequest findFirstByCollectionRunIdAndRequestIdOrderByOrderDesc(UUID testRunId, UUID requestId);

    int countByCollectionRunId(UUID testRunId);

    List<CollectionRunRequest> findAllByCollectionRunIdAndOrderGreaterThanEqualOrderByOrder(UUID testRunId,
                                                                                            Integer order);
}
