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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class FlagsDeserializer extends JsonDeserializer<List<Flags>> {
    private static final String ID_KEY = "id";

    @Override
    public List<Flags> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        JsonNode flagEntitiesJson = jsonParser.readValueAsTree();

        List<Flags> flags = new ArrayList<>();
        for (JsonNode flagEntity : flagEntitiesJson) {
            if (flagEntity.isObject()) {
                UUID id = UUID.fromString(flagEntity.get(ID_KEY).asText());
                Flags flag = Flags.getById(id);
                flags.add(flag);
            } else {
                String enumName = flagEntity.asText();
                Flags flag = Flags.valueOf(enumName);
                flags.add(flag);
            }
        }
        return flags;
    }
}
