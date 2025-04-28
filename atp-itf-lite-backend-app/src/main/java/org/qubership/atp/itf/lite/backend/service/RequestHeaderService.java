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

package org.qubership.atp.itf.lite.backend.service;

import java.util.UUID;

import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestHeaderRepository;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestHeaderService extends CrudService<RequestHeader> {

    private final RequestHeaderRepository repository;

    /**
     * Disable request header.
     *
     * @param headerId request header identifier
     */
    public void disableRequestHeader(UUID headerId) {
        log.info("Disable request header with id '{}'", headerId);
        RequestHeader header = get(headerId);
        header.setDisabled(true);

        repository.save(header);
    }

    /**
     * Enable request header.
     *
     * @param headerId request header identifier
     */
    public void enableRequestHeader(UUID headerId) {
        log.info("Enable request header with id '{}'", headerId);
        RequestHeader header = get(headerId);
        header.setDisabled(false);

        repository.save(header);
    }

    @Override
    protected JpaRepository<RequestHeader, UUID> repository() {
        return repository;
    }
}
