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

public enum ContextScope {
    GLOBALS("ITF_LITE_GLOBALS_"),
    COLLECTION("ITF_LITE_COLLECTIONVARIABLES_"),
    ENVIRONMENT("ITF_LITE_ENVIRONMENT_"),
    DATA("ITF_LITE_ITERATIONDATA_"),
    LOCAL_VARIABLES("");

    @Getter
    private final String prefix;

    ContextScope(String prefix) {
        this.prefix = prefix;
    }
}
