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

package org.qubership.atp.itf.lite.backend.enums.http;

public enum RequestBodyMode {
    RAW("RAW"),
    URLENCODED("URLENCODED"),
    GRAPHQL("GRAPHQL"),
    FILE("FILE"),
    FORMDATA("FORMDATA");

    private String name;

    RequestBodyMode(String name) {
        this.name = name;
    }

    /**
     * Return RequestBodyMode by name ignoring case or null.
     *
     * @param name RequestBodyMode name
     * @return RequestBodyMode
     */
    public static RequestBodyMode valueOfIgnoreCase(String name) {
        for (RequestBodyMode value : RequestBodyMode.values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }
}
