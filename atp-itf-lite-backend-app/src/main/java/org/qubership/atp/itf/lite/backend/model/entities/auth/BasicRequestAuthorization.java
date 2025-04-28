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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

import org.javers.core.metamodel.annotation.DiffInclude;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "basic_request_authorizations")
@Inheritance(strategy = InheritanceType.JOINED)
public class BasicRequestAuthorization extends RequestAuthorization {

    @Column(name = "username", nullable = false)
    @DiffInclude
    private String username;

    @Column(name = "password", nullable = false)
    @DiffInclude
    private String password;


    /**
     * Copy RequestAuthorization constructor.
     * @param authorization authorization
     */
    public BasicRequestAuthorization(BasicRequestAuthorization authorization) {
        super(authorization);
        this.username = authorization.getUsername();
        this.password = authorization.getPassword();
    }

    /**
     * Create copy of current authorization request.
     * @return BasicRequestAuthorization
     */
    @Override
    public BasicRequestAuthorization copy() {
        return new BasicRequestAuthorization(this);
    }

}
