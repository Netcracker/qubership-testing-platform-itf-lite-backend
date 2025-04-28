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
import java.util.UUID;

import org.qubership.atp.itf.lite.backend.model.entities.Cookie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CookiesRepository extends JpaRepository<Cookie, UUID> {

    void removeAllByExecutionRequestIdIsNotNullAndTestRunIdIsNotNull();

    void removeAllByIdIn(Collection<UUID> ids);

    void removeAllByUserIdAndProjectId(UUID userId, UUID projectId);

    void removeAllByExecutionRequestIdAndTestRunId(UUID executionRequestId, UUID testRunId);

    List<Cookie> findAllByUserIdAndProjectId(UUID userId, UUID projectId);

    @Query(value = "select * from cookies "
            + "where execution_request_id = ?1 and (test_run_id = ?2 or test_run_id is null)",
            nativeQuery = true)
    List<Cookie> findAllByExecutionRequestIdAndTestRunIdOrTestRunIdIsNull(UUID executionRequestId,
                                                                          UUID testRunId);
}
