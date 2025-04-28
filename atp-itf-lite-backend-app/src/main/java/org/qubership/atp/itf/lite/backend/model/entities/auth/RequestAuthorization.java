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

package org.qubership.atp.itf.lite.backend.model.entities.auth;

import static javax.persistence.EnumType.STRING;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.javers.core.metamodel.annotation.DiffInclude;
import org.javers.core.metamodel.annotation.ValueObject;
import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.qubership.atp.itf.lite.backend.model.entities.AbstractEntity;
import org.qubership.atp.itf.lite.backend.model.entities.Copyable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@ValueObject
@Table(name = "request_authorizations")
@Inheritance(strategy = InheritanceType.JOINED)
public class RequestAuthorization extends AbstractEntity implements Copyable {

    @Column(name = "type")
    @Enumerated(value = STRING)
    @DiffInclude
    protected RequestAuthorizationType type;

    /**
     * Copy RequestAuthorization constructor.
     * @param requestAuthorization requestAuthorization
     */
    public RequestAuthorization(RequestAuthorization requestAuthorization) {
        this.type = requestAuthorization.getType();
    }

    /**
     * Create copy of current authorization request.
     * @return RequestAuthorization
     */
    public RequestAuthorization copy() {
        return new RequestAuthorization(this);
    }
}
