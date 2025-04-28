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

@Getter
public enum ActionName {
    EXECUTE_REQUEST_BY_ID("Execute request", "Execute request \".*\""),
    EXECUTE_FOLDER_BY_ID("Execute requests folder", "Execute requests folder \".*\""),
    EXECUTE_FOLDER_BY_PATH("Execute requests folder by path", "Execute requests folder by path (.*)");

    private final String name;
    private final String regexp;

    ActionName(String name, String regexp) {
        this.name = name;
        this.regexp = regexp;
    }
}
