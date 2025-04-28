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

import static java.util.UUID.fromString;
import static org.qubership.atp.itf.lite.backend.catalog.models.FlagEntity.Type.COLLECTION;
import static org.qubership.atp.itf.lite.backend.catalog.models.FlagEntity.Type.EXECUTION;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public enum Flags {
    FAIL_IMMEDIATELY(fromString("07fc1fd7-2ae3-44d5-b78d-b9dd82d5d198"), "Fail Immediately", EXECUTION),
    INVERT_RESULT(fromString("5e482fa3-1f9b-42eb-52b8-789591342c78"), "Invert Result", EXECUTION),
    EXECUTE_ANYWAY(fromString("ea06f846-28cb-4e02-85d1-e693ac77d954"), "Execute anyway", EXECUTION),
    STOP_ON_FAIL(fromString("3e482af5-19fb-4e2b-82b5-879519342c68"), "Stop on fail", EXECUTION),
    SKIP_ON_PASS(fromString("339b4111-5438-4ea1-b1d6-9effd3d03708"),
            "Skip on pass", EXECUTION),
    SKIP_IF_DEPENDENCIES_FAIL(fromString("8596d3da-0226-4df8-9877-0a05f7784586"),
            "Skip if dependencies fail", EXECUTION),
    SKIP(fromString("5d9b8af2-9c21-4750-afda-b605f52cacec"), "Skip", EXECUTION),
    TERMINATE_IF_FAIL(fromString("547c84b1-4111-457d-bc7d-76e3a2a9d157"),
            "Terminate if fail", EXECUTION),
    COLLECT_LOGS(fromString("b7025ffb-fa42-4c70-997e-715bc8324946"), "Collect logs", COLLECTION),
    COLLECT_LOGS_ON_FAIL(fromString("c9b450db-1799-4911-b804-25f4e808ef89"),
            "Collect logs on fail", COLLECTION),
    COLLECT_LOGS_ON_SKIPPED(fromString("adf75cc8-0dc6-4776-803f-8ab314c17fce"),
            "Collect logs on skipped", COLLECTION),
    COLLECT_LOGS_ON_WARNING(fromString("8a9db7a6-6ba6-4501-b4d3-9f09f8719afd"),
            "Collect logs on warning", COLLECTION),
    TERMINATE_IF_PREREQUISITE_FAIL(fromString("bff9505b-961d-43b7-a757-7a893abb0f1b"),
            "Terminate if prerequisite fail", EXECUTION),
    IGNORE_PREREQUISITE_IN_PASS_RATE(fromString("10f36a40-5ed9-473d-a037-87a949bb56f0"),
            "Ignore prerequisite section in pass rate", EXECUTION),
    IGNORE_VALIDATION_IN_PASS_RATE(fromString("1e1a7ebb-23b3-48dc-83e5-5b0c4b92788b"),
            "Ignore validation section in pass rate", EXECUTION),
    DO_NOT_PASS_INITIAL_CONTEXT(fromString("1e1a7ebb-23b3-48dc-83e5-5b0c4b92787b"),
            "Do not pass initial context", EXECUTION),
    COLLECT_SSM_METRICS_ON_FAIL(fromString("df34da77-0703-4d71-916d-02d137b5920b"),
            "Collect SSM metrics on fail", EXECUTION);

    private static final Map<UUID, Flags> byIdIndex = new HashMap<>();

    static {
        for (Flags flag : Flags.values()) {
            byIdIndex.put(flag.getId(), flag);
        }
    }

    private FlagEntity flagEntity;

    Flags(UUID id, String name, FlagEntity.Type type) {
        this.flagEntity = new FlagEntity(id, name, type);
    }

    /**
     * Get flag by id.
     */
    public static Flags getById(UUID id) {
        return byIdIndex.get(id);
    }

    public String getName() {
        return flagEntity.getName();
    }

    public UUID getId() {
        return flagEntity.getId();
    }
}
