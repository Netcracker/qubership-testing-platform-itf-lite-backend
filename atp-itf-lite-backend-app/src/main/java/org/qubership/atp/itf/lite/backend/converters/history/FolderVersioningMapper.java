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

package org.qubership.atp.itf.lite.backend.converters.history;

import java.util.ArrayList;
import java.util.Objects;

import org.modelmapper.ModelMapper;
import org.qubership.atp.itf.lite.backend.feign.dto.history.AuthorizationDto;
import org.qubership.atp.itf.lite.backend.feign.dto.history.FolderHistoryChangeDto;
import org.qubership.atp.itf.lite.backend.feign.dto.history.HistoryItemTypeDto;
import org.qubership.atp.itf.lite.backend.feign.dto.history.PermissionDto;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.PermissionEntity;
import org.qubership.atp.itf.lite.backend.model.entities.converters.ListConverter;
import org.qubership.atp.itf.lite.backend.model.entities.converters.PermissionEntityConverter;
import org.springframework.stereotype.Component;

@Component
public class FolderVersioningMapper extends AbstractVersioningMapper<Folder, FolderHistoryChangeDto> {

    private final PermissionEntityConverter permissionEntityConverter;
    private final ListConverter listConverter;

    FolderVersioningMapper(PermissionEntityConverter permissionEntityConverter, ListConverter listConverter,
                           ModelMapper mapper) {
        super(Folder.class, FolderHistoryChangeDto.class, mapper);
        this.permissionEntityConverter = permissionEntityConverter;
        this.listConverter = listConverter;
    }

    @Override
    HistoryItemTypeDto getEntityTypeEnum() {
        return HistoryItemTypeDto.FOLDER;
    }

    @Override
    void mapSpecificFields(Folder source, FolderHistoryChangeDto destination) {
        super.mapSpecificFields(source, destination);
        destination.isAutoCookieDisabled(source.isAutoCookieDisabled());
        destination.setDisableFollowingRedirect(source.isDisableFollowingRedirect());
        destination.setDisableSslClientCertificate(source.isDisableSslClientCertificate());
        destination.setDisableSslCertificateVerification(source.isDisableSslCertificateVerification());
        destination.setChildFolders(listConverter.convertToEntityAttribute(source.getChildFolders()));
        destination.requests(listConverter.convertToEntityAttribute(source.getChildRequests()));
        PermissionEntity permissionEntity = permissionEntityConverter.convertToEntityAttribute(source.getPermission());
        if (permissionEntity != null) {
            destination.setPermission(new PermissionDto()
                    .isEnable(permissionEntity.isEnable())
                    .userAccess(permissionEntity.getUserAccess() == null
                            ? null
                            : new ArrayList<>(permissionEntity.getUserAccess().values())));
        }
        if (Objects.nonNull(source.getAuthorization())) {
            destination.setAuthorization(mapper.map(source.getAuthorization(), AuthorizationDto.class));
        }
    }
}
