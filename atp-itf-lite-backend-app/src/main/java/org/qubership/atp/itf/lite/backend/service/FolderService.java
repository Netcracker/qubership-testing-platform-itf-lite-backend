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

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
//import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.modelmapper.ModelMapper;
import org.qubership.atp.auth.springbootstarter.entities.Operation;
import org.qubership.atp.auth.springbootstarter.entities.Operations;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpEntityNotFoundException;
import org.qubership.atp.auth.springbootstarter.security.permissions.PolicyEnforcement;
import org.qubership.atp.auth.springbootstarter.services.UsersService;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.FolderRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestRepository;
import org.qubership.atp.itf.lite.backend.enums.EntityType;
import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.qubership.atp.itf.lite.backend.exceptions.access.ItfLiteAccessDeniedException;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderCopyRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderDeleteRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderEditRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderMoveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderOrderChangeRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderTreeSearchRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderUpsetRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.IdWithModifiedWhen;
import org.qubership.atp.itf.lite.backend.model.api.request.Permissions;
import org.qubership.atp.itf.lite.backend.model.api.request.Settings;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.GroupResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.ParentRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.AbstractNamedEntity;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.PermissionEntity;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.model.entities.auth.InheritFromParentRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.converters.ListConverter;
import org.qubership.atp.itf.lite.backend.model.entities.converters.PermissionEntityConverter;
import org.qubership.atp.itf.lite.backend.service.history.iface.DeleteHistoryService;
import org.qubership.atp.itf.lite.backend.service.history.iface.EntityHistoryService;
import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.qubership.atp.itf.lite.backend.utils.RequestUtils;
import org.qubership.atp.itf.lite.backend.utils.StreamUtils;
import org.qubership.atp.itf.lite.backend.utils.UserManagementEntities;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FolderService extends CrudService<Folder> implements EntityHistoryService {

    private final FolderRepository folderRepository;
    private final RequestRepository requestRepository;
    private final ModelMapper modelMapper;
    private final UsersService usersService;
    private final PolicyEnforcement policyEnforcement;
    private final Provider<UserInfo> userInfoProvider;
    private final RequestAuthorizationService requestAuthorizationService;
    private final FolderSpecificationService folderSpecificationService;
    private final DeleteHistoryService deleteHistoryService;
    private final PermissionEntityConverter permissionEntityConverter;
    private final ListConverter listConverter;

    @Override
    protected JpaRepository<Folder, UUID> repository() {
        return folderRepository;
    }

    /**
     * Save folder (and update folder children).
     *
     * @param folder folder to save
     * @return saved folder
     */
    @Override
    public Folder save(Folder folder) {
        Folder savedFolder;
        if (Objects.isNull(folder.getId())) {
            savedFolder = super.save(folder);
        } else {
            savedFolder = this.updateFolderChildren(folder);
        }
        this.updateParentFolderChildren(folder);
        return savedFolder;
    }

    /**
     * Save folders (and update folder children).
     *
     * @param folders folders to save
     * @return saved folders
     */
    @Override
    public List<Folder> saveAll(List<Folder> folders) {
        List<Folder> savedFolders = this.updateFoldersChildren(folders);
        this.updateParentFolderChildren(folders);
        return savedFolders;
    }

    /**
     * Get folder by specified identifier.
     *
     * @param folderId folder identifier
     * @return folder
     */
    public Folder getFolder(UUID folderId) {
        log.info("Find folder by id {}", folderId);

        return get(folderId);
    }

    /**
     * Get folder by specified identifier.
     *
     * @param folderId folder identifier
     * @return folder
     */
    public Settings getSettings(UUID folderId) {
        log.info("Get folder settings by id {}", folderId);
        return modelMapper.map(get(folderId), Settings.class);
    }

    /**
     * Collects set of permission ids from folders.
     * Need for PreAuthorize checks
     * @param folderIds set of folder ids
     * @return set of permission folder ids
     */
    public Set<UUID> getPermissionFolderIdsByFolderIds(Set<UUID> folderIds) {
        return folderRepository.findAllByIdIn(folderIds)
                .stream()
                .map(Folder::getPermissionFolderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public List<Folder> getFolderByIdAndName(UUID folderId, String folderName) {
        return folderRepository.findAllByIdAndName(folderId, folderName);
    }

    public List<Folder> getFoldersByIds(Set<UUID> folderIds) {
        return folderRepository.findAllById(folderIds);
    }

    /**
     * Get all folders by specified project identifier.
     *
     * @param projectId project identifier
     * @return collection of folders
     */
    public Collection<Folder> getAllFolders(UUID projectId) {
        log.info("Find all folders, filters: [projectId: {}]", projectId);
        if (nonNull(projectId)) {
            return folderRepository.findAllByProjectIdOrderByOrder(projectId);
        }

        return getAll();
    }

    /**
     * Gets all folders by specified project id and parent folder id.
     * @param projectId project id
     * @param parentFolderId parent folder id
     * @return list of found folders
     */
    public List<Folder> getAllByProjectIdAndParentId(UUID projectId, UUID parentFolderId) {
        log.info("Find all folders, filters: [projectId: {}, parentFolderId: {}]", projectId, parentFolderId);
        Specification<Folder> folderSpecification = Specification
                .where(folderSpecificationService.generateSpecificationToFilterFoldersByProjectIdAndParentFolderId(
                        projectId, parentFolderId));
        return folderRepository.findAll(folderSpecification);
    }

    /**
     * Get tree for folders and requests by specified search request.
     *
     * @param request search request
     * @return tree root nodes
     */
    public GroupResponse getFolderRequestsTree(Boolean onlyFolders, FolderTreeSearchRequest request) {
        final UUID projectId = request.getProjectId();
        final UUID parentId = request.getParentId();
        final String search = request.getSearch();
        log.info("Get folders and requests tree by project '{}', parent id '{}' and search contains '{}'",
                projectId, parentId, search);

        List<Request> requests;
        boolean isSearchEmpty = StringUtils.isEmpty(search);
        if (!onlyFolders) {
            requests = requestRepository.findAllByProjectId(projectId);
        } else {
            requests = new ArrayList<>();
        }

        List<Folder> projectFolders = folderRepository.findAllByProjectId(projectId);

        List<Folder> projectTopFolders = StreamUtils.filterList(projectFolders, folder -> isNull(folder.getParentId()));

        Map<String, Map<UUID, Operations>> servicePermissions =
                usersService.getObjectPermissionsForService(request.getProjectId());
        List<GroupResponse> topLevelEntities = projectTopFolders.stream()
                .map(rootFolder -> getFolderGroupResponse(rootFolder,
                        new GroupResponse(rootFolder, null), projectFolders, requests, search, servicePermissions))
                .filter(rootFolder -> !rootFolder.isFilteredOut())
                .collect(Collectors.toList());

        if (!isSearchEmpty) {
            topLevelEntities =
                    topLevelEntities.stream()
                            .filter(folder -> !CollectionUtils.isEmpty(folder.getChildren())
                                    || StringUtils.containsIgnoreCase(folder.getName(), search))
                            .collect(Collectors.toList());
        }

        List<GroupResponse> topLevelRequests = StreamUtils.filterList(requests, req -> isNull(req.getFolderId()))
                .stream()
                .filter(topLevelRequest -> isNull(search)
                        || StringUtils.containsIgnoreCase(topLevelRequest.getName(), search))
                .map(r -> {
                    GroupResponse gr = new GroupResponse(r, null);
                    gr.setHasWritePermissions(true);
                    return gr;
                })
                .collect(Collectors.toList());

        topLevelEntities.addAll(topLevelRequests);

        GroupResponse response = new GroupResponse();
        response.setChildren(topLevelEntities);
        sortFolderRequestTree(response);
        return response;
    }

    /**
     * Get request tree.
     */
    public GroupResponse getRequestTreeByParentFolderId(UUID parentFolderId) {
        Folder parentFolder = getFolder(parentFolderId);
        List<Request> projectRequests = requestRepository.findAllByProjectId(parentFolder.getProjectId());
        List<Folder> projectFolders = folderRepository.findAllByProjectId(parentFolder.getProjectId());
        GroupResponse response = getFolderGroupResponse(parentFolder, new GroupResponse(parentFolder, null),
                projectFolders, projectRequests, null, null);
        sortFolderRequestTree(response);
        return response;
    }

    @Override
    public AbstractNamedEntity restore(AbstractNamedEntity entity) {
        try {
            Folder folder = (Folder) entity;
            log.info("Restore folder to version {}", folder);
            PermissionEntity permission = permissionEntityConverter.convertToEntityAttribute(folder.getPermission());
            FolderEditRequest folderUpsetRequest = new FolderEditRequest(
                    folder.getName(),
                    folder.getProjectId(),
                    folder.getParentId(),
                    permission == null
                            ? null
                            : new Permissions(permission));
            return this.editFolder(folder.getId(), folderUpsetRequest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<GroupResponse> getChildFolders(GroupResponse root) {
        return root.getChildren()
                .stream()
                .filter(child -> child.getType().equals(EntityType.FOLDER))
                .collect(Collectors.toList());
    }

    private void sortFolderRequestTree(GroupResponse tree) {
        List<GroupResponse> children = tree.getChildren();
        if (!CollectionUtils.isEmpty(children)) {
            Collections.sort(children);
            children.forEach(this::sortFolderRequestTree);
        }
    }

    private GroupResponse getFolderGroupResponse(Folder rootFolder, GroupResponse response,
                                                 List<Folder> folders, List<Request> requests, String search,
                                                 Map<String, Map<UUID, Operations>> servicePermissions) {
        final UUID rootFolderId = rootFolder.getId();

        UUID permissionFolderId = rootFolder.getPermissionFolderId();
        Permissions perms = new Permissions();
        boolean hasWritePermissions;
        if (nonNull(permissionFolderId) && !CollectionUtils.isEmpty(servicePermissions)) {
            Map<UUID, Operations> objectPermissions = servicePermissions.get(usersService
                    .getObjectName(UserManagementEntities.FOLDER.getName(), permissionFolderId));
            if (rootFolder.getId().equals(permissionFolderId) && nonNull(objectPermissions)) {
                perms.setUserAccess(objectPermissions.keySet());
                perms.setIsEnable(true);
            }
            response.setPermissions(perms);
            hasWritePermissions = userHasWritePermissions(objectPermissions);
            response.setHasWritePermissions(hasWritePermissions);
        } else {
            hasWritePermissions = true;
            response.setHasWritePermissions(true);
        }

        List<Folder> childrenFolders =
                StreamUtils.filterList(folders, folder -> rootFolderId.equals(folder.getParentId()));

        final ParentRequestAuthorization parentAuth;
        RequestAuthorization rootFolderAuth = rootFolder.getAuthorization();
        if (nonNull(rootFolderAuth)) {
            // if inherit then use parent auth
            if (RequestAuthorizationType.INHERIT_FROM_PARENT.equals(rootFolderAuth.getType())) {
                parentAuth = response.getParentAuth();
            } else {
                // else user rootFolderAuth
                parentAuth = new ParentRequestAuthorization(rootFolder.getId(), rootFolder.getName(),
                        rootFolderAuth.getType());
            }
        } else {
            // if root auth not specified that means noauth
            parentAuth = new ParentRequestAuthorization(rootFolder.getId(), rootFolder.getName(), null);
        }

        childrenFolders.forEach(folder -> {
            GroupResponse childResponse = getFolderGroupResponse(folder, new GroupResponse(folder, parentAuth), folders,
                    requests, search, servicePermissions);
            if (!childResponse.isFilteredOut()) {
                response.addChildren(childResponse);
            }
        });

        List<Request> folderMatchedRequests =
                StreamUtils.filterList(requests, request -> rootFolderId.equals(request.getFolderId()));

        if (!CollectionUtils.isEmpty(folderMatchedRequests)) {
            final List<GroupResponse> childFolders = getChildFolders(response);
            boolean isAllChildrenFoldersFilteredOut = childFolders.isEmpty() || childFolders
                    .stream()
                    .allMatch(GroupResponse::isFilteredOut);
            boolean isNoneRequestNameMatchesSearch = folderMatchedRequests.stream()
                    .noneMatch(request -> StringUtils.containsIgnoreCase(request.getName(), search));

            List<GroupResponse> requestResponses;
            if (nonNull(search) && (!isNoneRequestNameMatchesSearch || !isAllChildrenFoldersFilteredOut)) {
                requestResponses = folderMatchedRequests.stream()
                        .filter(request -> StringUtils.containsIgnoreCase(request.getName(), search))
                        .map(request -> {
                            GroupResponse gr = new GroupResponse(request, parentAuth);
                            gr.setPermissions(perms);
                            gr.setHasWritePermissions(hasWritePermissions);
                            return gr;
                        }).collect(Collectors.toList());
            } else {
                requestResponses = folderMatchedRequests.stream()
                        .map(request -> {
                            GroupResponse gr = new GroupResponse(request, parentAuth);
                            gr.setPermissions(perms);
                            gr.setHasWritePermissions(hasWritePermissions);
                            return gr;
                        }).collect(Collectors.toList());
            }

            boolean isFolderNameMatchesSearch = StringUtils.containsIgnoreCase(rootFolder.getName(), search);
            if (nonNull(search)) {
                response.setFilteredOut(isNoneRequestNameMatchesSearch && isAllChildrenFoldersFilteredOut
                        && !isFolderNameMatchesSearch);
            }

            response.addChildren(requestResponses);
        } else {
            final List<GroupResponse> childFolders = getChildFolders(response);
            boolean isAllChildrenFoldersFilteredOut = childFolders.isEmpty() || childFolders
                    .stream()
                    .allMatch(GroupResponse::isFilteredOut);
            boolean isFolderNameMatchesSearch = StringUtils.containsIgnoreCase(rootFolder.getName(), search);
            if (nonNull(search)) {
                response.setFilteredOut(isAllChildrenFoldersFilteredOut && !isFolderNameMatchesSearch);
            }
        }

        return response;
    }

    private boolean userHasWritePermissions(Map<UUID, Operations> objectPermissions) {
        UserInfo userInfo = userInfoProvider.get();
        UUID userId = nonNull(userInfo) ? userInfo.getId() : null;
        return policyEnforcement.isAdmin() || (nonNull(userId) && !CollectionUtils.isEmpty(objectPermissions)
                && objectPermissions.containsKey(userId));
    }

    /**
     * Create folder.
     *
     * @param request creation request
     * @return created folder
     */
    @Transactional
    public Folder createFolder(FolderUpsetRequest request) throws Exception {
        log.info("Create folder by request: {}", request);

        AuthorizationSaveRequest authorization = request.getAuthorization();
        if (nonNull(authorization)) {
            requestAuthorizationService.encryptAuthorizationParameters(authorization);
        }

        final Folder folder = modelMapper.map(request, Folder.class);
        setOrder(folder);

        if (Objects.nonNull(request.getPermissions())) {
            // set folder permissions for new folder if needed
            updateFolderPermissions(folder, request.getPermissions());
        }

        updateAuthorizationFolderId(folder);
        log.debug("Saving folder: {}", folder);
        Folder savedFolder = this.save(folder);

        if (Objects.nonNull(request.getPermissions()) && request.getPermissions().getIsEnable()) {
            // if write permissions enabled
            // then create object with permissions in users service
            usersService.grantAllPermissions(
                    UserManagementEntities.FOLDER.getName(),
                    request.getProjectId(),
                    savedFolder.getPermissionFolderId(),
                    new ArrayList<>(request.getPermissions().getUserAccess()));
        }
        return savedFolder;
    }

    private void updateFolderPermissions(Folder folder, Permissions permissions) {
        // if write permissions enabled for current folder
        if (permissions.getIsEnable()) {
            // then set as permissionFolderId current folderId
            UUID folderId = folder.getId();
            if (Objects.isNull(folderId)) {
                folderId = UUID.randomUUID();
                folder.setId(folderId);
            }
            folder.setPermissionFolderId(folderId);
            folder.setPermission(
                    permissionEntityConverter.convertToDatabaseColumn(new PermissionEntity(
                            getUsersForPermission(permissions.getUserAccess(), folder.getProjectId()))));
        } else {
            // else get permissionFolderId from parent if exists
            if (Objects.nonNull(folder.getParentId())) {
                Folder parentFolder = getFolder(folder.getParentId());
                folder.setPermissionFolderId(parentFolder.getPermissionFolderId());
                folder.setPermission(parentFolder.getPermission());
            } else {
                folder.setPermissionFolderId(null);
                folder.setPermission(permissionEntityConverter.convertToDatabaseColumn(new PermissionEntity()));
            }
        }
    }

    private TreeMap<UUID,String> getUsersForPermission(Set<UUID> userIds, UUID projectId) {
        TreeMap<UUID, String> users = new TreeMap<>();
        List<UserInfo> userInfoList = usersService.getUsersInfoByProjectId(projectId, new ArrayList<>(userIds));
        if (userInfoList != null) {
            userInfoList.forEach(u -> users.put(u.getId(), u.getFullName()));
        }
        return users;
    }

    private void setChildPermissions(Folder parentFolder) {
        if (nonNull(parentFolder)) {
            GroupResponse rootNode = getRequestTreeByParentFolderId(parentFolder.getId());
            setChildPermission(rootNode, parentFolder.getPermissionFolderId());
        }
    }

    private void setChildPermissions(UUID parentFolderId) {
        if (nonNull(parentFolderId)) {
            Folder parentFolder = getFolder(parentFolderId);
            GroupResponse rootNode = getRequestTreeByParentFolderId(parentFolderId);
            setChildPermission(rootNode, parentFolder.getPermissionFolderId());
        }
    }

    private void setChildPermission(GroupResponse node, UUID permissionFolderId) {
        List<GroupResponse> children = node.getChildren();
        if (!CollectionUtils.isEmpty(children)) {
            children.forEach(child -> {
                if (EntityType.FOLDER.equals(child.getType())) {
                    Folder childFolder = getFolder(child.getId());
                    if (childFolder != null && !childFolder.getId().equals(childFolder.getPermissionFolderId())) {
                        childFolder.setPermissionFolderId(permissionFolderId);
                        this.save(childFolder);
                        setChildPermission(child, permissionFolderId);
                    }
                } else {
                    Request childRequest = requestRepository.findById(child.getId())
                            .orElseThrow(() -> new AtpEntityNotFoundException("Request", child.getId()));
                    childRequest.setPermissionFolderId(permissionFolderId);
                    requestRepository.save(childRequest);
                }
            });
        }
    }

    /**
     * Calculate and set order for the folder.
     */
    public void setOrder(Folder folder) {
        final UUID projectId = folder.getProjectId();
        final UUID parentId = folder.getParentId();

        Integer maxOrder = isNull(parentId) ? folderRepository.findMaxOrder(projectId) :
                folderRepository.findMaxOrder(projectId, parentId);

        final Integer calcOrder = nonNull(maxOrder) ? ++maxOrder : 0;
        log.debug("Folder order: {}", calcOrder);

        folder.setOrder(calcOrder);
    }

    /**
     * Create folders from list.
     *
     * @param folders list of folders to save
     * @return list of saved folders
     */
    public List<Folder> createFolders(List<Folder> folders) {
        log.info("Create folders by list folders: {}", folders);
        return this.saveAll(folders);
    }

    /**
     * Update folder.
     *
     * @param folderId folder identifier
     * @param request  update folder request
     */
    @Transactional
    public Folder editFolder(UUID folderId, FolderEditRequest request) throws Exception {
        log.info("Update folder by request: {}", request);

        AuthorizationSaveRequest authorization = request.getAuthorization();
        if (nonNull(authorization)) {
            requestAuthorizationService.encryptAuthorizationParameters(authorization);
        }

        Folder folder = get(folderId);
        if (!isSettingsAreEqual(folder, request)) {
            Set<UUID> copyFolderIds = folderRepository.findHeirsIdsByIdIn(Collections.singleton(folderId));
            List<Folder> foundFolders = folderRepository.findAllByIdIn(copyFolderIds);
            foundFolders.add(folder);
            changeOfInheritedProperties(foundFolders, request);
        }

        modelMapper.map(request, folder);

        if (Objects.nonNull(request.getPermissions())) {
            updateFolderPermissions(folder, request.getPermissions());
            setChildPermissions(folder);
        }

        updateAuthorizationFolderId(folder);
        Folder savedFolder = this.save(folder);

        if (Objects.nonNull(request.getPermissions()) && request.getPermissions().getIsEnable()) {
            usersService.grantAllPermissions(
                    UserManagementEntities.FOLDER.getName(),
                    request.getProjectId(),
                    savedFolder.getPermissionFolderId(),
                    new ArrayList<>(request.getPermissions().getUserAccess()));
        }
        return savedFolder;
    }

    /**
     * Updates InheritAuthorizationRequest.
     *
     * @param request request to update
     */
    public void updateAuthorizationFolderId(Request request) {
        updateAuthorizationFolderId(request.getAuthorization(), request.getFolderId());
    }

    /**
     * Updates InheritAuthorizationRequest.
     *
     * @param folder folder to update
     */
    public void updateAuthorizationFolderId(Folder folder) {
        updateAuthorizationFolderId(folder.getAuthorization(), folder.getParentId());
    }


    /**
     * Updates InheritAuthorizationRequest.
     *
     * @param authorization authorization to update
     * @param parentFolderId parent folder id for entity with passed authorization
     */
    public void updateAuthorizationFolderId(@Nullable RequestAuthorization authorization,
                                            @Nullable UUID parentFolderId) {
        // if authorization is inheritFromParent
        // then set authorizationFolderId from parentFolder
        if (nonNull(authorization)
                && RequestAuthorizationType.INHERIT_FROM_PARENT.equals(authorization.getType())) {
            InheritFromParentRequestAuthorization inheritAuth =
                    (InheritFromParentRequestAuthorization) authorization;
            inheritAuth.setAuthorizationFolderId(parentFolderId);
        }
    }

    private Set<UUID> getAllChildFolderIds(Folder parentFolder) {
        List<Folder> projectFolders = folderRepository.findAllByProjectId(parentFolder.getProjectId());
        return collectChildFolderIds(parentFolder, projectFolders);
    }

    /**
     * Change values for requests.
     */
    private void changeOfInheritedProperties(List<Folder> folderList, FolderEditRequest requestUpdate) {
        final Set<UUID> foldersIds = StreamUtils.extractIds(folderList);
        List<Request> requestList = requestRepository.findAllByFolderIdIn(foldersIds);

        requestList.forEach(request -> {
            request.setDisableSslCertificateVerification(requestUpdate.isDisableSslCertificateVerification());
            request.setDisableSslClientCertificate(requestUpdate.isDisableSslClientCertificate());
            request.setDisableFollowingRedirect(requestUpdate.isDisableFollowingRedirect());
            request.setAutoCookieDisabled(requestUpdate.isAutoCookieDisabled());
            request.setDisableAutoEncoding(requestUpdate.isDisableAutoEncoding());
        });
        folderList.forEach(folder -> {
            folder.setDisableSslCertificateVerification(requestUpdate.isDisableSslCertificateVerification());
            folder.setDisableSslClientCertificate(requestUpdate.isDisableSslClientCertificate());
            folder.setDisableFollowingRedirect(requestUpdate.isDisableFollowingRedirect());
            folder.setAutoCookieDisabled(requestUpdate.isAutoCookieDisabled());
            folder.setDisableAutoEncoding(requestUpdate.isDisableAutoEncoding());
        });

        log.debug("Change properties for folders: {} and requests: {}", foldersIds, StreamUtils.extractIds(folderList));
        requestRepository.saveAll(requestList);
        this.saveAll(folderList);
    }

    /**
     * Copy folder.
     *
     * @param request copy folder request
     */
    @Transactional
    public void copyFolders(FolderCopyRequest request) {

        log.info("Copy folders by request: {}", request);
        Set<UUID> sourceFolderIds = request.getIds();

        Set<UUID> copyFolderIds = folderRepository.findHeirsIdsByIdIn(sourceFolderIds);
        List<Folder> foundFolders = folderRepository.findAllByIdIn(copyFolderIds);

        Map<UUID, UUID> oldNewIdsMapping = new HashMap<>();
        copyFolderIds.forEach(folderId -> oldNewIdsMapping.put(folderId, UUID.randomUUID()));

        List<Folder> copyFolders = StreamUtils.map(foundFolders, Folder::new);
        UUID destinationFolderId = request.getToFolderId();
        UUID projectId = request.getProjectId();
        copyFolders.forEach(folder -> {
            UUID id = folder.getId();
            UUID parentId = folder.getParentId();
            if (sourceFolderIds.contains(id)) {
                List<Folder> destinationFolders = folderRepository.findAllByProjectIdAndParentId(
                        projectId, destinationFolderId);
                addPostfixIfFolderNameInDestinationIsTaken(destinationFolders, folder);
                folder.setParentId(destinationFolderId);
                setOrder(folder);
            } else {
                folder.setParentId(oldNewIdsMapping.get(parentId));
            }
            folder.setId(oldNewIdsMapping.get(id));
            folder.setPermissionFolderId(null);
            updateAuthorizationFolderId(folder);
        });

        Set<UUID> copyFoldersIds = StreamUtils.extractIds(copyFolders);
        log.debug("Coping folders: {}", copyFoldersIds);
        this.saveAll(copyFolders);
        this.updateParentFolderChildren(destinationFolderId);


        copyFoldersRequests(foundFolders, oldNewIdsMapping);

        if (nonNull(request.getToFolderId())) {
            // update folder permissions if target folder id isn't root
            setChildPermissions(request.getToFolderId());
        }
    }

    /**
     * Add postfix "Copy" if folder with the same name already exists in destination folder.
     *
     * @param destinationFolders list of folders destination under folder
     * @param folder             folder
     */
    public void addPostfixIfFolderNameInDestinationIsTaken(List<Folder> destinationFolders, Folder folder) {
        while (destinationFolders.stream().anyMatch(
                destinationFolder -> destinationFolder.getName().equals(folder.getName()))) {
            folder.setName(folder.getName() + Constants.COPY_POSTFIX);
        }
    }

    /**
     * Copy nested folders requests.
     *
     * @param folders          number of folders
     * @param oldNewIdsMapping corresponding mapping of new<>old folders Ids
     */
    private void copyFoldersRequests(List<Folder> folders, Map<UUID, UUID> oldNewIdsMapping) {
        final Set<UUID> foldersIds = StreamUtils.extractIds(folders);
        log.debug("Copy folder requests. Folders: {}, id's mapping: {}", foldersIds, oldNewIdsMapping);

        List<Request> foundRequests = requestRepository.findAllByFolderIdIn(foldersIds);
        // make copy of requests to avoid problems with entities versions
        List<Request> copyRequests = StreamUtils.map(foundRequests, RequestUtils::copyRequestFromRequest);
        copyRequests.forEach(request -> {
            request.setId(UUID.randomUUID());
            request.setFolderId(oldNewIdsMapping.get(request.getFolderId()));
            request.setPermissionFolderId(null);
            updateAuthorizationFolderId(request);
        });
        Set<UUID> copyRequestIds = StreamUtils.extractIds(copyRequests);
        log.debug("Coping requests with ids: {}", copyRequestIds);
        requestRepository.saveAll(copyRequests);
        foldersIds.forEach(this::updateParentFolderChildren);
    }

    /**
     * Move folder.
     *
     * @param request move folder request
     */
    @Transactional
    public void moveFolders(FolderMoveRequest request) {
        log.info("Move folders by request: {}", request);
        Set<UUID> sourceFolderIds = StreamUtils.extractIds(request.getIds(), IdWithModifiedWhen::getId);
        UUID destinationFolderId = request.getToFolderId();

        List<Folder> moveFolders = folderRepository.findAllByIdIn(sourceFolderIds);
        moveFolders.forEach(folder -> {
            folder.setParentId(destinationFolderId);
            setOrder(folder);
            folder.setPermissionFolderId(null);
            updateAuthorizationFolderId(folder);
        });
        log.debug("Moving folders: [{}]", moveFolders);
        this.saveAll(moveFolders);
        // update permissions after moving
        setChildPermissions(destinationFolderId);
    }

    /**
     * Delete folders.
     *
     * @param request delete folders request
     */
    @Transactional
    public void deleteFolders(FolderDeleteRequest request) {
        log.info("Delete folders by request: {}", request);
        UUID projectId = request.getProjectId();

        List<Folder> allProjectFolders = folderRepository.findAllByProjectId(projectId);
        Set<UUID> deleteFolderIds = folderRepository.findHeirsIdsByIdIn(request.getIds());
        Set<UUID> folderPermissionsIds = allProjectFolders.stream()
                .map(Folder::getPermissionFolderId)
                .filter(permissionFolderId -> nonNull(permissionFolderId)
                        && deleteFolderIds.contains(permissionFolderId))
                .collect(Collectors.toSet());

        checkThatAccessGranted(projectId, folderPermissionsIds, Operation.DELETE);

        List<Request> foldersRequests = requestRepository.findAllByFolderIdIn(deleteFolderIds);
        Set<UUID> foldersRequestsIds = StreamUtils.extractIds(foldersRequests);
        log.debug("Deleting request ids: [{}]", foldersRequestsIds);
        requestRepository.deleteByIdIn(foldersRequestsIds);
        log.debug("Delete javers history snapshots by request ids: {}", foldersRequestsIds);
        deleteHistoryService.deleteSnapshotsByEntityIds(foldersRequestsIds);

        log.debug("Deleting folder ids: [{}]", deleteFolderIds);
        deleteFolderIds.forEach(this::updateParentFolderChildren);
        folderRepository.deleteByIdIn(deleteFolderIds);
        
        log.debug("Delete javers history snapshots by folder ids: {}", deleteFolderIds);
        deleteHistoryService.deleteSnapshotsByEntityIds(deleteFolderIds);

        if (!CollectionUtils.isEmpty(folderPermissionsIds)) {
            usersService.deleteObjectPermissionsBulk(UserManagementEntities.FOLDER.getName(), request.getProjectId(),
                    new ArrayList<>(folderPermissionsIds));
        }
    }

    /**
     * Check that access granted for operation.
     * @param projectId project id.
     * @param folderPermissionsIds folder permissions.
     * @param operation operation for which permissions are required (CREATE,READ,UPDATE,DELETE,EXECUTE,LOCK,UNLOCK).
     * @exception ItfLiteAccessDeniedException if no access granted.
     */
    public void checkThatAccessGranted(UUID projectId, Set<UUID> folderPermissionsIds, Operation operation) {
        boolean isAccessGranted = false;
        try {
            isAccessGranted = policyEnforcement.checkAccess(UserManagementEntities.FOLDER.getName(),
                    projectId,
                    folderPermissionsIds, operation);
        } catch (Exception ex) {
            log.error("Failed to check access on {} folders, message: {}", operation.toString(), ex.getMessage());
        }
        if (!isAccessGranted) {
            String message = "You do not have access to " + operation + " folder.";
            log.error(message);
            throw new ItfLiteAccessDeniedException(message);
        }
    }

    /**
     * Find folders heirs.
     *
     * @param request count folders heirs in delete request
     */
    public long countFolderHeirs(FolderDeleteRequest request) {
        log.info("Get folders with heirs by request: {}", request);
        long contHeirs = 0;

        Set<UUID> summaryFolderIds = folderRepository.findHeirsIdsByIdIn(request.getIds());
        List<Request> foldersRequests = requestRepository.findAllByFolderIdIn(summaryFolderIds);
        if (!CollectionUtils.isEmpty(foldersRequests)) {
            contHeirs = foldersRequests.size();
        }

        log.debug("Folders heirs: [{}]", foldersRequests);
        return contHeirs;
    }

    /**
     * Change folder order.
     */
    public void order(UUID folderId, FolderOrderChangeRequest request) {
        log.debug("Change order for the folder with id '{}', request params: {}", folderId, request);
        final UUID projectId = request.getProjectId();
        final UUID parentFolderId = request.getParentFolderId();
        final int order = request.getOrder();

        List<Folder> folders = folderRepository.findAllByProjectIdAndParentId(projectId, parentFolderId);
        folders.sort(Comparator.comparingInt(Folder::getOrder));

        Folder changedFolder = StreamUtils.find(folders, folder -> folder.getId().equals(folderId));
        folders.remove(changedFolder);
        folders.add(order, changedFolder);

        int count = 0;
        for (Folder folder : folders) {
            folder.setOrder(count++);
        }
        this.saveAll(folders);
    }

    /**
     * Get Folder by project ID and source ID.
     *
     * @param projectId project ID
     * @param sourceId  source ID
     * @return Folder
     */
    public Folder getByProjectIdAndSourceId(UUID projectId, UUID sourceId) {
        return folderRepository.getByProjectIdAndSourceId(projectId, sourceId);
    }

    /**
     * Get ids by folder's path.
     */
    public UUID getIdByFoldersPath(UUID projectId, List<String> path) {
        try {
            return folderRepository.getFolderIdPyPath(projectId, path);
        } catch (Exception ex) {
            log.error("Failed to get folder id by path {}", path, ex);
            throw new AtpEntityNotFoundException("Folder", "path", path);
        }
    }

    /**
     * Get parent auth for folder or request with specified parent folder id.
     * @param parentFolderId paren folder id
     * @return parent auth
     */
    public ParentRequestAuthorization getParentAuth(@Nullable UUID parentFolderId) {
        if (isNull(parentFolderId)) {
            return null;
        }
        Folder folder = getFolder(parentFolderId);
        RequestAuthorization folderAuth = folder.getAuthorization();
        if (nonNull(folderAuth)) {
            if (RequestAuthorizationType.INHERIT_FROM_PARENT.equals(folderAuth.getType())) {
                return getParentAuth(folder.getParentId());
            } else {
                return new ParentRequestAuthorization(parentFolderId, folder.getName(), folderAuth.getType());
            }
        }
        return new ParentRequestAuthorization(parentFolderId, folder.getName(), null);
    }

    /**
     * Update folder children (for parent folder): child folders and child request. Need for history.
     *
     * @param folderId folder ID of entity
     * @return updated folder or NULL folder not found
     */
    public Folder updateParentFolderChildren(UUID folderId) {
        if (folderId != null) {
            try {
                return updateParentFolderChildren(get(folderId));
            } catch (AtpEntityNotFoundException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Update folder children (for parent folder): child folders and child request. Need for history.
     *
     * @param folder folder entity
     * @return updated folder
     */
    public Folder updateParentFolderChildren(Folder folder) {
        if (folder.getParentId() != null) {
            return updateFolderChildren(folder.getParentId());
        }
        return folder;
    }

    /**
     * Update folders children (for parent folder): child folders and child request. Need for history.
     *
     * @param folders folder entity
     * @return updated folder
     */
    public List<Folder> updateParentFolderChildren(List<Folder> folders) {
        Set<Folder> parents = folders.stream()
                .filter(folder -> folder.getParentId() != null)
                .map(folder -> {
                    try {
                        return updateFolderChildren(get(folder.getParentId()));
                    } catch (AtpEntityNotFoundException e) {
                        return null;
                    }
                })
                .collect(Collectors.toSet());
        return updateFoldersChildren(new ArrayList<>(parents));
    }

    /**
     * Update folder children: child folders and child request. Need for history.
     *
     * @param folderId folder ID of entity
     * @return updated folder or NULL if folderId = NULL or folder not found
     */
    public Folder updateFolderChildren(UUID folderId) {
        if (folderId != null) {
            try {
                return updateFolderChildren(get(folderId));
            } catch (AtpEntityNotFoundException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Update folder children: child folders and child request. Need for history.
     *
     * @param folder folder entity
     * @return updated folder
     */
    public Folder updateFolderChildren(Folder folder) {
        folder.setChildFolders(listConverter.convertToDatabaseColumn(getChildFolderNames(folder)));
        folder.setChildRequests(listConverter.convertToDatabaseColumn(getChildRequestNames(folder)));
        return super.save(folder);
    }

    /**
     * Update folders children: child folders and child request. Need for history.
     *
     * @param folders folder entity
     * @return updated folder
     */
    public List<Folder> updateFoldersChildren(List<Folder> folders) {
        folders.forEach(folder -> {
            if (folder != null) {
                folder.setChildFolders(listConverter.convertToDatabaseColumn(getChildFolderNames(folder)));
                folder.setChildRequests(listConverter.convertToDatabaseColumn(getChildRequestNames(folder)));
            }
        });
        return super.saveAll(folders);
    }

    private List<String> getChildRequestNames(Folder parentFolder) {
        return requestRepository.findAllByFolderId(parentFolder.getId())
                .stream().map(Request::getName).sorted().collect(Collectors.toList());
    }

    private List<String> getChildFolderNames(Folder parentFolder) {
        return new ArrayList<>(collectChildFolderIds(
                parentFolder,
                folderRepository.findAllByProjectId(parentFolder.getProjectId()),
                false).values());
    }

    private Set<UUID> collectChildFolderIds(Folder parentFolder, List<Folder> projectFolders) {
        Set<UUID> childIds = new HashSet<>();
        projectFolders
                .stream()
                .filter(folder -> parentFolder.getId().equals(folder.getParentId()))
                .forEach(folder -> {
                    childIds.add(folder.getId());
                    childIds.addAll(collectChildFolderIds(folder, projectFolders));
                });
        return childIds;
    }

    private TreeMap<UUID, String> collectChildFolderIds(Folder parentFolder, List<Folder> projectFolders,
                                                        boolean includeChild) {
        TreeMap<UUID, String> childIds = new TreeMap<>();
        projectFolders
                .stream()
                .filter(folder -> parentFolder.getId().equals(folder.getParentId()))
                .forEach(folder -> {
                    childIds.put(folder.getId(), folder.getName());
                    if (includeChild) {
                        childIds.putAll(collectChildFolderIds(folder, projectFolders, true));
                    }
                });
        return childIds;
    }

    /**
     * Compares folder and folderEditRequest settings.
     * @param folder folder
     * @param folderEditRequest folder edit request
     * @return true if settings are the same
     */
    private boolean isSettingsAreEqual(Folder folder, FolderEditRequest folderEditRequest) {
        return new EqualsBuilder()
                .append(folder.isAutoCookieDisabled(), folderEditRequest.isAutoCookieDisabled())
                .append(folder.isDisableSslCertificateVerification(),
                        folderEditRequest.isDisableSslCertificateVerification())
                .append(folder.isDisableSslClientCertificate(), folderEditRequest.isDisableSslClientCertificate())
                .append(folder.isDisableFollowingRedirect(), folderEditRequest.isDisableFollowingRedirect())
                .append(folder.isDisableAutoEncoding(), folderEditRequest.isDisableAutoEncoding())
                .isEquals();
    }
}
