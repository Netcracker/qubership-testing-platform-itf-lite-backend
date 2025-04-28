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

package org.qubership.atp.itf.lite.backend.model.entities;

import static java.util.Objects.nonNull;

import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.javers.core.metamodel.annotation.DiffInclude;
import org.qubership.atp.itf.lite.backend.converters.history.FolderVersioningMapper;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.converters.ListConverter;
import org.qubership.atp.itf.lite.backend.model.entities.converters.PermissionEntityConverter;
import org.qubership.atp.itf.lite.backend.service.FolderService;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "folders")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Folder extends AbstractNamedEntity {

    @Column(name = "project_id")
    @DiffInclude
    private UUID projectId;

    @Column(name = "parent_id")
    @DiffInclude
    private UUID parentId;

    @Column(name = "`order`")
    @DiffInclude
    private Integer order;

    @Column(name = "source_id")
    @DiffInclude
    private UUID sourceId;

    @Column(name = "permission_folder_id")
    @DiffInclude
    private UUID permissionFolderId;

    @Column(name = "disable_cookie_generation")
    @DiffInclude
    private boolean isAutoCookieDisabled;

    @Column(name = "disable_ssl_certificate_verification")
    @DiffInclude
    private boolean disableSslCertificateVerification;

    @Column(name = "disable_ssl_client_certificate")
    @DiffInclude
    private boolean disableSslClientCertificate;

    @Column(name = "disable_following_redirect")
    @DiffInclude
    private boolean disableFollowingRedirect;

    @Column(name = "disable_auto_encoding")
    @DiffInclude
    private boolean disableAutoEncoding;

    @Column(name = "description", columnDefinition = "TEXT")
    @DiffInclude
    private String description;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "authorization_id")
    @DiffInclude
    protected RequestAuthorization authorization;

    /** For History change usage.
     * Converter {@link PermissionEntityConverter}
     * is using in
     * 1. {@link FolderVersioningMapper}
     * 2. {@link FolderService}
     */
    @Column(name = "permission_info")
    @DiffInclude
    private String permission;

    /** For History change usage. DO NOT use in restore!
     * Converter {@link ListConverter}
     * is using in
     * 1. {@link FolderVersioningMapper}
     * 2. {@link FolderService}
     */
    @Column(name = "child_folders")
    @DiffInclude
    private String childFolders;

    /** For History change usage.  DO NOT use in restore!
     * Converter {@link ListConverter}
     * is using in
     * 1. {@link FolderVersioningMapper}
     * 2. {@link FolderService}
     */
    @Column(name = "child_requests")
    @DiffInclude
    private String childRequests;

    /**
     * Copy folder constructor.
     *
     * @param folder request to copy
     */
    public Folder(Folder folder) {
        this.id = folder.getId();
        this.name = folder.getName();
        this.projectId = folder.getProjectId();
        this.parentId = folder.getParentId();
        this.order = folder.getOrder();
        this.sourceId = folder.getSourceId();
        this.permissionFolderId = folder.getPermissionFolderId();
        this.isAutoCookieDisabled = folder.isAutoCookieDisabled();
        this.disableSslCertificateVerification = folder.isDisableSslCertificateVerification();
        this.disableSslClientCertificate = folder.isDisableSslClientCertificate();
        this.disableFollowingRedirect = folder.isDisableFollowingRedirect();
        this.description = folder.getDescription();
        RequestAuthorization authorization = folder.getAuthorization();
        if (nonNull(authorization)) {
            this.authorization = authorization.copy();
        }
        this.permission = folder.getPermission();
        this.childFolders = folder.getChildFolders();
        this.childRequests = folder.getChildRequests();
    }

    @JsonProperty(value = "isAutoCookieDisabled")
    public boolean isAutoCookieDisabled() {
        return isAutoCookieDisabled;
    }

    public void setAutoCookieDisabled(boolean isAutoCookieDisabled) {
        this.isAutoCookieDisabled = isAutoCookieDisabled;
    }
}
