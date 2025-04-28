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

package org.qubership.atp.itf.lite.backend.dataaccess.repository.history;

import java.util.Collection;

import org.qubership.atp.itf.lite.backend.model.entities.javers.history.JvCommitPropertyEntity;
import org.qubership.atp.itf.lite.backend.model.entities.javers.history.JvCommitPropertyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JaversCommitPropertyRepository extends JpaRepository<JvCommitPropertyEntity, JvCommitPropertyId> {

    @Modifying
    @Query("DELETE FROM JvCommitPropertyEntity e WHERE e.id.commitId = :commitId")
    void deleteByIdCommitId(@Param("commitId") Long commitId);

    @Modifying
    @Query("DELETE FROM JvCommitPropertyEntity e WHERE e.id.commitId IN (:commitIds)")
    void deleteByIdCommitIdIn(@Param("commitIds") Collection<Long> commitIds);
}
