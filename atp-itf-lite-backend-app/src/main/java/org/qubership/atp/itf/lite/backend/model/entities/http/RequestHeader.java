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

package org.qubership.atp.itf.lite.backend.model.entities.http;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.javers.core.metamodel.annotation.DiffInclude;
import org.javers.core.metamodel.annotation.ValueObject;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpHeaderSaveRequest;
import org.qubership.atp.itf.lite.backend.model.entities.AbstractNamedEntity;

import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@ValueObject
@Table(name = "request_headers")
@Data
@NoArgsConstructor
public class RequestHeader extends AbstractNamedEntity {

    @Column(name = "key")
    @DiffInclude
    private String key;

    @Column(name = "value", columnDefinition = "TEXT")
    @DiffInclude
    private String value;

    @Column(name = "description")
    @DiffInclude
    private String description;

    @Column(name = "disabled")
    @DiffInclude
    private boolean disabled;

    @Column(name = "generated")
    private boolean generated;

    /**
     * RequestHeader constructor.
     */
    public RequestHeader(UUID id, String key, String value, String description, boolean isDisabled, boolean generated) {
        this.id = id;
        this.key = key;
        this.value = value;
        this.description = description;
        this.disabled = isDisabled;
        this.generated = generated;
    }

    /**
     * RequestHeader constructor.
     */
    public RequestHeader(UUID id, String key, String value, String description, boolean isDisabled) {
        this(id, key, value, description, isDisabled, false);
    }

    /**
     * RequestHeader constructor.
     */
    public RequestHeader(String key, String value, String description, boolean isDisabled) {
        this(null, key, value, description, isDisabled, false);
    }

    /**
     * RequestHeader constructor.
     */
    public RequestHeader(String key, String value, String description, boolean isDisabled, boolean generated) {
        this(null, key, value, description, isDisabled, generated);
    }

    /**
     * HttpHeaderSaveRequest convert constructor.
     */
    public RequestHeader(HttpHeaderSaveRequest header) {
        this.key = header.getKey();
        this.value = header.getValue();
        this.description = header.getDescription();
        this.disabled = header.isDisabled();
        this.generated = header.isGenerated();
        this.id = header.getId();
    }

    /**
     * Copy header constructor.
     *
     * @param requestHeader header to copy
     */
    public RequestHeader(RequestHeader requestHeader) {
        this.key = requestHeader.getKey();
        this.value = requestHeader.getValue();
        this.description = requestHeader.getDescription();
        this.disabled = requestHeader.isDisabled();
        this.generated = requestHeader.isGenerated();
    }
}
