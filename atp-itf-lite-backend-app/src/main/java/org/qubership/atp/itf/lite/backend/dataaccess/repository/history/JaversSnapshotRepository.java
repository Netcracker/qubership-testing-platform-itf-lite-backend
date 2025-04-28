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
import java.util.List;

import org.javers.core.metamodel.object.SnapshotType;
import org.qubership.atp.itf.lite.backend.model.api.response.history.JaversCountResponse;
import org.qubership.atp.itf.lite.backend.model.entities.javers.history.JvSnapshotEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JaversSnapshotRepository extends JpaRepository<JvSnapshotEntity, Long> {

    Page<JvSnapshotEntity> findAllByTypeIs(SnapshotType type, Pageable pageable);

    List<JvSnapshotEntity> findAllByGlobalIdIn(Collection<Long> globalIds);

    List<JvSnapshotEntity> findAllByGlobalIdOrderByVersionAsc(Long globalId, Pageable pageable);

    JvSnapshotEntity findFirstByGlobalIdOrderByVersionAsc(Long globalId);

    Long countByCommitId(Long commitId);

    @Modifying
    void deleteByVersionAndGlobalIdAndCommitId(Long version, Long globalId, Long commitId);

    @Modifying
    @Query("DELETE FROM JvSnapshotEntity e WHERE e.globalId IN (:globalIds)")
    void deleteByGlobalIdIn(@Param("globalIds") Collection<Long> globalIds);

    @Modifying
    @Query("DELETE FROM JvSnapshotEntity e WHERE e.commitId IN (:commitIds)")
    void deleteByCommitIdIn(@Param("commitIds") Collection<Long> commitIds);

    @Query("SELECT e.globalId AS id, COUNT(e.globalId) AS count "
            + "FROM JvSnapshotEntity e GROUP BY e.globalId HAVING COUNT(e.globalId) > :count")
    List<JaversCountResponse> findGlobalIdAndCountGreaterThan(@Param("count") long count);
}
