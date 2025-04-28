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

package org.qubership.atp.itf.lite.backend.service.history.impl;

import org.javers.core.Javers;
import org.javers.shadow.Shadow;
import org.qubership.atp.itf.lite.backend.converters.history.HttpRequestVersioningMapper;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.FolderRepository;
import org.qubership.atp.itf.lite.backend.feign.dto.history.CompareEntityResponseDto;
import org.qubership.atp.itf.lite.backend.feign.dto.history.HttpRequestHistoryDto;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class HttpRequestRetrieveHistoryService
        extends AbstractRequestRetrieveHistoryService<HttpRequest, HttpRequestHistoryDto> {

    public HttpRequestRetrieveHistoryService(Javers javers, HttpRequestVersioningMapper mapping,
                                             FolderRepository folderRepository) {
        super(javers, mapping, folderRepository);
    }

    @Override
    public Class<HttpRequest> getEntityClass() {
        return HttpRequest.class;
    }

    public HttpRequestVersioningMapper getMapper() {
        return (HttpRequestVersioningMapper) abstractVersioningMapper;
    }

    /**
     * Build compare entity.
     * @param revision snapshot version.
     * @param entity shadow from history.
     * @return response with compare entity and version.
     */
    public CompareEntityResponseDto buildCompareEntity(String revision, Shadow<Object> entity) {
        log.debug("version={}, entity={}", revision, entity);
        HttpRequest httpRequest = (HttpRequest) entity.get();
        HttpRequestHistoryDto resolvedEntity = getMapper().map(httpRequest);
        resolvedEntity.setFolderName(getFolderName(httpRequest.getFolderId()));
        setCommonFields(httpRequest, resolvedEntity, entity.getCommitMetadata());
        CompareEntityResponseDto compareEntityResponseDto = new CompareEntityResponseDto();
        compareEntityResponseDto.setRevision(revision);
        compareEntityResponseDto.setCompareEntity(resolvedEntity);
        return compareEntityResponseDto;
    }
}
