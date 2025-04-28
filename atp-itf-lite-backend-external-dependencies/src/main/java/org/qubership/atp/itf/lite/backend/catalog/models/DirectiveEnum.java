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

package org.qubership.atp.itf.lite.backend.catalog.models;

import java.util.UUID;

public enum DirectiveEnum {

    USE(
            UUID.fromString("96c65818-d959-11e9-8a34-2a2ae2dbcce4"),
            "@Use()",
            "@Use(resource_system_name)"
    );

    private Directive directive;


    DirectiveEnum(UUID id, String name, String description) {
        this.directive = new Directive(id, name, description);
    }

    public String getName() {
        return directive.getName();
    }
}

