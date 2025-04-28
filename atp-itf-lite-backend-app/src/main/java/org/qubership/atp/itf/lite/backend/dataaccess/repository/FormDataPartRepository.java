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

import java.util.UUID;

import org.qubership.atp.itf.lite.backend.model.entities.http.FormDataPart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface FormDataPartRepository extends JpaRepository<FormDataPart, UUID> {

    @Modifying
    @Query("update FormDataPart f set f.fileId = ?1, f.fileSize = ?2, f.value = ?3 where f.id = ?4")
    void updateFileInfoById(UUID fileId, long fileSize, String value, UUID id);
}
