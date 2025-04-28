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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.qubership.atp.itf.lite.backend.enums.EntityType;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderTreeSearchRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.GroupResponse;
import org.qubership.atp.itf.lite.backend.model.documentation.AbstractDocumentation;
import org.qubership.atp.itf.lite.backend.model.documentation.DocumentationResponse;
import org.qubership.atp.itf.lite.backend.model.documentation.FolderDocumentation;
import org.qubership.atp.itf.lite.backend.model.documentation.RequestDocumentation;
import org.qubership.atp.itf.lite.backend.model.documentation.RequestEntityEditDocumentation;
import org.qubership.atp.itf.lite.backend.model.documentation.TreeFolderDocumentationResponse;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentationService {
    private final FolderService folderService;
    private final RequestService requestService;
    private final ModelMapper modelMapper;
    private final WritePermissionsService writePermissionsService;

    /**
     * Get description when use Folder.
     */
    public TreeFolderDocumentationResponse getDescription(FolderTreeSearchRequest request, int page, int pageSize) {
        final UUID projectId = request.getProjectId();
        final UUID parentId = request.getParentId();
        log.debug("Collect description for folder and request. Project {} for parent folder {}",
                projectId, parentId);
        GroupResponse sortedDoc = folderService.getRequestTreeByParentFolderId(parentId);

        List<DocumentationResponse> listGroupResponse = new ArrayList<>();
        convertToListGroupResponse(listGroupResponse, sortedDoc);
        final int size = listGroupResponse.size();
        List<DocumentationResponse> listOfPartDocumentation = getPartOfObject(listGroupResponse, page, size, pageSize);

        List<AbstractDocumentation> abstractDocumentationList = new ArrayList<>();

        listOfPartDocumentation.stream().forEach(obj -> {
            if (EntityType.FOLDER.equals(obj.getType())) {
                Folder temporaryFolder = folderService.getFolder(obj.getId());
                FolderDocumentation folderDocumentation = modelMapper.map(temporaryFolder, FolderDocumentation.class);
                folderDocumentation.setHasWritePermissions(
                        writePermissionsService.hasWritePermissions(
                                temporaryFolder.getPermissionFolderId(),
                                temporaryFolder.getProjectId()));
                abstractDocumentationList.add(folderDocumentation);
            } else if (EntityType.REQUEST.equals(obj.getType())) {
                Request temporaryRequest = requestService.getRequest(obj.getId());
                abstractDocumentationList.add(modelMapper.map(temporaryRequest, RequestDocumentation.class));
            }
        });

        return new TreeFolderDocumentationResponse(size, abstractDocumentationList);
    }


    /**
     * Edit documentation for request.
     */
    public RequestDocumentation editDocumentationRequest(UUID requestId,
                                                         RequestEntityEditDocumentation documentationRequest) {
        log.debug("Check if request with requestId {} exists in project {}",
                requestId, documentationRequest.getProjectId());

        Request temporaryRequest = requestService.getRequest(requestId);
        temporaryRequest.setDescription(documentationRequest.getDescription());
        temporaryRequest = requestService.save(temporaryRequest);
        RequestDocumentation requestDocumentation = modelMapper.map(temporaryRequest, RequestDocumentation.class);
        log.debug("Edited documentation for Request {}", requestDocumentation);
        return requestDocumentation;
    }

    /**
     * Edit documentation for folder.
     */
    public FolderDocumentation editDocumentationFolder(UUID folderId,
                                                       RequestEntityEditDocumentation documentationRequest) {
        log.debug("Check if request with requestId {} exists in project {}",
                folderId, documentationRequest.getProjectId());

        Folder temporaryFolder = folderService.getFolder(folderId);
        modelMapper.map(documentationRequest, temporaryFolder);
        temporaryFolder = folderService.save(temporaryFolder);
        FolderDocumentation folderDocumentation = modelMapper.map(temporaryFolder, FolderDocumentation.class);
        log.debug("Edited documentation for Folder {}", folderDocumentation);
        return folderDocumentation;
    }

    /**
     * Get part of all list objects.
     *
     * @param listDocumentation list objects.
     * @param page              part of all list objects.
     * @param size              what needed get objects.
     * @param pageSize          sixe list objects.
     * @return list of documents.
     */
    private List<DocumentationResponse> getPartOfObject(List<DocumentationResponse> listDocumentation,
                                                        int page, int size, int pageSize) {
        if (size > pageSize) {
            List<DocumentationResponse> listOfPartDocumentation = listDocumentation.subList(
                    Math.min(listDocumentation.size() - 1, page * pageSize),
                    Math.min(listDocumentation.size(), ((page * pageSize) + pageSize)));
            return listOfPartDocumentation;
        }
        return listDocumentation;
    }

    private List<DocumentationResponse> convertToListGroupResponse(List<DocumentationResponse> list,
                                                                   GroupResponse tree) {
        list.add(modelMapper.map(tree, DocumentationResponse.class));
        if (tree.getChildren() != null) {
            tree.getChildren()
                    .forEach(child -> {
                        convertToListGroupResponse(list, child);
                    });
        }
        return list;
    }
}
