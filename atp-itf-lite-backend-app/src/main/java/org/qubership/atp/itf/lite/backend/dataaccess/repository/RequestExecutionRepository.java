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

import org.qubership.atp.itf.lite.backend.model.entities.history.RequestExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface RequestExecutionRepository extends JpaRepository<RequestExecution, UUID> {

    String GET_EXECUTORS_IN_HISTORY_BY_PROJECT_ID = ""
            + "SELECT DISTINCT(executor) FROM request_executions WHERE project_id=:projectId";

    @Modifying
    @Transactional
    @Query("DELETE FROM RequestExecution re WHERE re.executedWhen < :executedWhen")
    int deleteByExecutedWhenBefore(@Param("executedWhen") Timestamp executedWhen);

    @Query(value = GET_EXECUTORS_IN_HISTORY_BY_PROJECT_ID, nativeQuery = true)
    List<String> findByProjectId(@Param("projectId") UUID projectId);

    RequestExecution findBySseId(UUID sseId);
}
