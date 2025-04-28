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

package org.qubership.atp.itf.lite.backend.feign.dto;

public enum FlagsEnum {
    COLLECT_LOGS("COLLECT_LOGS"),
    COLLECT_LOGS_ON_FAIL("COLLECT_LOGS_ON_FAIL"),
    COLLECT_LOGS_ON_SKIPPED("COLLECT_LOGS_ON_SKIPPED"),
    COLLECT_LOGS_ON_WARNING("COLLECT_LOGS_ON_WARNING"),
    DO_NOT_PASS_INITIAL_CONTEXT("DO_NOT_PASS_INITIAL_CONTEXT"),
    EXECUTE_ANYWAY("EXECUTE_ANYWAY"),
    FAIL_IMMEDIATELY("FAIL_IMMEDIATELY"),
    IGNORE_PREREQUISITE_IN_PASS_RATE("IGNORE_PREREQUISITE_IN_PASS_RATE"),
    IGNORE_VALIDATION_IN_PASS_RATE("IGNORE_VALIDATION_IN_PASS_RATE"),
    INVERT_RESULT("INVERT_RESULT"),
    SKIP("SKIP"),
    SKIP_IF_DEPENDENCIES_FAIL("SKIP_IF_DEPENDENCIES_FAIL"),
    STOP_ON_FAIL("STOP_ON_FAIL"),
    TERMINATE_IF_FAIL("TERMINATE_IF_FAIL"),
    TERMINATE_IF_PREREQUISITE_FAIL("TERMINATE_IF_PREREQUISITE_FAIL"),
    COLLECT_SSM_METRICS_ON_FAIL("COLLECT_SSM_METRICS_ON_FAIL");

    private String value;

    FlagsEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
