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

package org.qubership.atp.itf.lite.backend.dataaccess.migration;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.qubership.atp.itf.lite.backend.utils.StreamUtils.filterList;
import static org.qubership.atp.itf.lite.backend.utils.StreamUtils.map;
import static org.qubership.atp.itf.lite.backend.utils.StreamUtils.toEntityListMap;
import static org.qubership.atp.itf.lite.backend.utils.StreamUtils.toIdEntityMap;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.qubership.atp.itf.lite.backend.configuration.SpringLiquibaseBeanAware;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.FolderRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestRepository;
import org.qubership.atp.itf.lite.backend.enums.EntityType;
import org.qubership.atp.itf.lite.backend.model.api.response.GroupResponse;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.Request;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FolderRequestsOrderMigrationTask implements CustomTaskChange {

    @Override
    public void execute(Database database) {
        log.debug("Trying to execute migration script for setting requests and folders order");
        try {
            FolderRepository folderRepository = SpringLiquibaseBeanAware.getBean(FolderRepository.class);
            RequestRepository requestRepository = SpringLiquibaseBeanAware.getBean(RequestRepository.class);

            final List<Folder> allFolders = folderRepository.findAll();
            final List<Request> allRequests = requestRepository.findAll();

            final Map<UUID, List<Folder>> foldersToProjectMap = toEntityListMap(allFolders, Folder::getProjectId);
            final Map<UUID, List<Request>> requestsToProjectMap = toEntityListMap(allRequests, Request::getProjectId);
            final Set<UUID> projectIds = foldersToProjectMap.keySet();

            for (UUID projectId : projectIds) {
                final GroupResponse root = new GroupResponse();
                final List<Folder> projectFolders = foldersToProjectMap.get(projectId);
                final List<Request> projectRequests = requestsToProjectMap.get(projectId);

                final GroupResponse tree = buildTree(root, projectFolders, projectRequests);
                setOrder(tree, toIdEntityMap(projectFolders), toIdEntityMap(projectRequests));

                folderRepository.saveAll(allFolders);
                requestRepository.saveAll(projectRequests);
            }
        } catch (Exception err) {
            log.error("Failed to complete migration script for setting requests and folders order", err);
        }
        log.debug("Migration script have been successfully executed");
    }

    private void setOrder(GroupResponse root, Map<UUID, Folder> folderMap, Map<UUID, Request> requestMap) {
        List<GroupResponse> children = root.getChildren();
        if (!isEmpty(children)) {
            int order = 0;
            for (GroupResponse child : children) {
                EntityType type = child.getType();
                UUID id = child.getId();
                if (type.equals(EntityType.FOLDER)) {
                    setOrder(child, folderMap, requestMap);
                    Folder folder = folderMap.get(id);
                    folder.setOrder(order++);
                } else {
                    Request request = requestMap.get(id);
                    request.setOrder(order++);
                }
            }
        }
    }

    private GroupResponse buildTree(GroupResponse root, List<Folder> allFolders, List<Request> allRequests) {
        final EntityType type = root.getType();
        final UUID parentId = root.getId();
        final List<GroupResponse> children = root.getChildren();
        List<Folder> folders;
        List<Request> requests;

        if (isNull(type)) {
            folders = filterList(allFolders, folder -> isNull(folder.getParentId()));
            requests = filterList(allRequests, request -> isNull(request.getFolderId()));
        } else {
            folders = filterList(allFolders, folder -> parentId.equals(folder.getParentId()));
            requests = filterList(allRequests, request -> parentId.equals(request.getFolderId()));
        }
        if (!isEmpty(requests)) {
            children.addAll(map(requests, request -> new GroupResponse(request, null)));
        }
        if (!isEmpty(folders)) {
            children.addAll(map(folders, folder ->
                    buildTree(new GroupResponse(folder, null), allFolders, allRequests)));
        }
        children.sort((first, second) -> {
            Date firstCreatedWhen = first.getCreatedWhen();
            Date secondCreatedWhen = second.getCreatedWhen();

            if (nonNull(firstCreatedWhen) && nonNull(secondCreatedWhen)) {
                return firstCreatedWhen.compareTo(secondCreatedWhen);
            }

            return 0;
        });

        return root;
    }

    @Override
    public String getConfirmationMessage() {
        return null;
    }

    @Override
    public void setUp() throws SetupException {

    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {

    }

    @Override
    public ValidationErrors validate(Database database) {
        return null;
    }
}
