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

package org.qubership.atp.itf.lite.backend.model.entities.user;

import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.qubership.atp.itf.lite.backend.model.entities.AbstractNamedEntity;
import org.qubership.atp.itf.lite.backend.model.entities.converters.ListConverter;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_settings")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class UserSettings extends AbstractNamedEntity {

    @Column(name = "user_id")
    protected UUID userId;

    @Column(name = "visible_columns", columnDefinition = "TEXT")
    @Convert(converter = ListConverter.class)
    private List<String> visibleColumns;
}