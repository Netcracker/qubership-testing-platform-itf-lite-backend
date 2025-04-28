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

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import org.javers.core.metamodel.annotation.DiffInclude;
import org.javers.core.metamodel.annotation.ValueObject;
import org.qubership.atp.itf.lite.backend.annotations.SerializableCheckable;
import org.qubership.atp.itf.lite.backend.enums.ValueType;
import org.qubership.atp.itf.lite.backend.model.entities.AbstractEntity;

import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@ValueObject
@Table(name = "form_data_part")
@Data
@NoArgsConstructor
@SerializableCheckable
public class FormDataPart extends AbstractEntity implements Serializable {

    @Column(name = "key")
    @DiffInclude
    private String key;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    @DiffInclude
    private ValueType type;

    @Column(name = "value")
    @DiffInclude
    private String value;

    @Column(name = "file_id")
    @DiffInclude
    private UUID fileId;

    @Column(name = "file_size")
    @DiffInclude
    private long fileSize;

    @Column(name = "content_type")
    @DiffInclude
    private String contentType;

    @Column(name = "description")
    @DiffInclude
    private String description;

    @Column(name = "disabled")
    @DiffInclude
    private boolean disabled;

    /**
     * FormDataPart constructor.
     */
    public FormDataPart(UUID id, String key, ValueType type, String value, UUID fileId, String contentType,
                        String description, boolean isDisabled) {
        this.id = id;
        this.key = key;
        this.type = type;
        this.value = value;
        this.fileId = fileId;
        this.contentType = contentType;
        this.description = description;
        this.disabled = isDisabled;
    }

    /**
     * FormDataPart constructor.
     */
    public FormDataPart(String key, ValueType type, String value, UUID fileId, String contentType,
                        String description, boolean isDisabled) {
        this(null, key, type, value, fileId, contentType, description, isDisabled);
    }

    /**
     * Copy FormDataPart constructor.
     *
     * @param formDataPart formDataPart to copy
     */
    public FormDataPart(FormDataPart formDataPart) {
        this.key = formDataPart.getKey();
        this.type = formDataPart.getType();
        this.value = formDataPart.getValue();
        this.fileId = formDataPart.getFileId();
        this.contentType = formDataPart.getContentType();
        this.description = formDataPart.getDescription();
        this.disabled = formDataPart.isDisabled();
    }
}
