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

package org.qubership.atp.itf.lite.backend.model.entities.history;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import org.qubership.atp.itf.lite.backend.enums.TestingStatus;
import org.qubership.atp.itf.lite.backend.model.entities.AbstractEntity;

import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "testing_statuses")
@Data
@NoArgsConstructor
public class TestStatus extends AbstractEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private TestingStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Constructor TestStatus.
     */
    public TestStatus(UUID id, String name, TestingStatus status, String errorMessage) {
        this.id = id;
        this.name = name;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    public TestStatus(String name, TestingStatus status, String errorMessage) {
        this(null, name, status, errorMessage);
    }

    public TestStatus(String name, TestingStatus status) {
        this(null, name, status, null);
    }
}
