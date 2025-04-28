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

import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.javers.core.metamodel.annotation.DiffInclude;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.model.api.response.ParentRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "requests")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "transportType", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = HttpRequest.class,
                name = TransportType.TransportTypeNames.REST),
        @JsonSubTypes.Type(value = HttpRequest.class,
                name = TransportType.TransportTypeNames.SOAP)
})
public abstract class Request extends AbstractNamedEntity {

    @Column(name = "project_id")
    @DiffInclude
    protected UUID projectId;

    @Column(name = "folder_id")
    @DiffInclude
    protected UUID folderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_type")
    @DiffInclude
    protected TransportType transportType;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "authorization_id")
    @DiffInclude
    protected RequestAuthorization authorization;

    @Column(name = "`order`")
    @DiffInclude
    private Integer order;

    @Column(name = "source_id")
    @DiffInclude
    private UUID sourceId;

    @Column(name = "prescripts")
    @DiffInclude
    private String preScripts;

    @Column(name = "postscripts")
    @DiffInclude
    private String postScripts;

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
    private String description;

    @Transient
    private boolean hasWritePermissions;

    @Transient
    private ParentRequestAuthorization parentAuth;

    /**
     * Copy request constructor.
     *
     * @param request request to copy
     */
    public Request(Request request) {
        this.id = request.getId();
        this.name = request.getName();
        this.projectId = request.getProjectId();
        this.folderId = request.getFolderId();
        this.transportType = request.getTransportType();
        this.authorization = request.authorization;
        this.order = request.order;
        this.sourceId = request.getSourceId();
        this.preScripts = request.getPreScripts();
        this.postScripts = request.getPostScripts();
        this.permissionFolderId = request.getPermissionFolderId();
        this.isAutoCookieDisabled = request.isAutoCookieDisabled();
        this.disableSslCertificateVerification = request.isDisableSslCertificateVerification();
        this.disableSslClientCertificate = request.isDisableSslClientCertificate();
        this.disableFollowingRedirect = request.isDisableFollowingRedirect();
        this.description = request.getDescription();
        this.parentAuth = request.getParentAuth();
    }

    @JsonProperty(value = "isAutoCookieDisabled")
    public boolean isAutoCookieDisabled() {
        return isAutoCookieDisabled;
    }

    public void setAutoCookieDisabled(boolean isAutoCookieDisabled) {
        this.isAutoCookieDisabled = isAutoCookieDisabled;
    }

}
