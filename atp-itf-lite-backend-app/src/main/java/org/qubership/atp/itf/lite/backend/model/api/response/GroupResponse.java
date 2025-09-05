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

package org.qubership.atp.itf.lite.backend.model.api.response;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.qubership.atp.itf.lite.backend.enums.EntityType;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.qubership.atp.itf.lite.backend.model.api.request.Permissions;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.model.entities.http.methods.HttpMethod;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GroupResponse implements Comparable<GroupResponse> {

    @NotNull
    private UUID id;

    @NotNull
    @NotEmpty
    private String name;

    private EntityType type;

    @JsonIgnore
    private Date createdWhen;

    private Date modifiedWhen;

    private Integer order;

    private List<GroupResponse> children = new ArrayList<>();

    @JsonIgnore
    private boolean filteredOut;

    private Permissions permissions;
    
    private boolean hasWritePermissions;

    @Nullable
    private RequestAuthorizationType authType;

    @Nullable
    private ParentRequestAuthorization parentAuth;

    @Nullable
    private TransportType transportType;

    @Nullable
    private HttpMethod httpMethod;

    /**
     * Constructor.
     * @param request request
     * @param parentAuth parent auth for request
     */
    public GroupResponse(Request request, @Nullable ParentRequestAuthorization parentAuth) {
        this(request.getId(), request.getName(), EntityType.REQUEST, request.getOrder(), request.getCreatedWhen(),
                parentAuth, request.getModifiedWhen());
        if (nonNull(request.getAuthorization())) {
            this.authType = request.getAuthorization().getType();
        }

        this.transportType = request.getTransportType();
        if (TransportType.REST.equals(request.getTransportType())
                || TransportType.SOAP.equals(request.getTransportType())) {
            this.httpMethod = ((HttpRequest) request).getHttpMethod();
        }
    }

    /**
     * Constructor.
     * @param folder folder
     * @param parentAuth parent auth for folder
     */
    public GroupResponse(Folder folder, @Nullable ParentRequestAuthorization parentAuth) {
        this(folder.getId(), folder.getName(), EntityType.FOLDER, folder.getOrder(), folder.getCreatedWhen(),
                parentAuth, folder.getModifiedWhen());
        if (nonNull(folder.getAuthorization())) {
            this.authType = folder.getAuthorization().getType();
        }
    }

    /**
     * GroupResponse constructor.
     */
    public GroupResponse(UUID id, String name, EntityType type, Integer order, Date createdWhen,
                         @Nullable ParentRequestAuthorization parentAuth, Date modifiedWhen) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.children = new ArrayList<>();
        this.createdWhen = createdWhen;
        this.modifiedWhen = modifiedWhen;
        this.order = order;
        this.parentAuth = parentAuth;
    }

    public void addChildren(GroupResponse child) {
        this.children.add(child);
    }

    public void addChildren(Collection<GroupResponse> children) {
        this.children.addAll(children);
    }

    @Override
    public int compareTo(GroupResponse node) {
        int typeOrderDiff = type.getOrder() - node.type.getOrder();
        if (typeOrderDiff != 0) {
            return typeOrderDiff;
        }

        if (isNull(order) || isNull(node.order)) {
            return 0;
        }

        return order - node.order;
    }
}
