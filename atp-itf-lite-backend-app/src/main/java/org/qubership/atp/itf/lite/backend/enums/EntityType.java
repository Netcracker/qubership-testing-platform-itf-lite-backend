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

package org.qubership.atp.itf.lite.backend.enums;

import lombok.Getter;

public enum EntityType {

    FOLDER("FOLDER", 100),
    REQUEST("REQUEST", 200);

    private String name;
    @Getter
    private Integer order;

    EntityType(String name, Integer order) {
        this.name = name;
        this.order = order;
    }

    @Override
    public String toString() {
        return name;
    }
}
