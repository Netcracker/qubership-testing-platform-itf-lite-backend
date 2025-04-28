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

import static java.util.Objects.nonNull;

import java.util.Map;
import java.util.UUID;

import org.qubership.atp.auth.springbootstarter.entities.Operations;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.security.permissions.PolicyEnforcement;
import org.qubership.atp.auth.springbootstarter.services.UsersService;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.qubership.atp.itf.lite.backend.utils.UserManagementEntities;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WritePermissionsService {

    private final PolicyEnforcement entityAccess;
    private final UsersService usersService;
    private final Provider<UserInfo> userInfoProvider;

    /**
     * Check that user has write permissions for this permission folder.
     * @param permissionFolderId permission folder id.
     * @param projectId project id.
     * @return true if user has write permissions.
     */
    public boolean hasWritePermissions(UUID permissionFolderId, UUID projectId) {
        if (nonNull(permissionFolderId)) {
            Map<String, Map<UUID, Operations>> objectPermissions = usersService.getPermissionsByObjectId(
                    UserManagementEntities.FOLDER.getName(), projectId, permissionFolderId);
            String objectName = usersService.getObjectName(UserManagementEntities.FOLDER.getName(),
                    permissionFolderId);
            if (!CollectionUtils.isEmpty(objectPermissions) && objectPermissions.containsKey(objectName)) {
                Map<UUID, Operations> assignedUsers = objectPermissions.get(objectName);
                return userHasWritePermissions(assignedUsers);
            }
        }
        return true;
    }

    private boolean userHasWritePermissions(Map<UUID, Operations> objectPermissions) {
        UserInfo userInfo = userInfoProvider.get();
        UUID userId = nonNull(userInfo) ? userInfo.getId() : null;
        return entityAccess.isAdmin() || (nonNull(userId) && objectPermissions.containsKey(userId));
    }
}
