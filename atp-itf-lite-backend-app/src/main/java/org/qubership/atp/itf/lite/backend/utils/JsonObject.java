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

package org.qubership.atp.itf.lite.backend.utils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import lombok.Getter;

@Getter
public class JsonObject {

    JSONObject obj;

    public JsonObject(Object obj) {
        this.obj = (JSONObject) obj;
    }

    public JsonObject getObject(String key) {
        return new JsonObject(obj.get(key));
    }

    public boolean containsKey(String key) {
        return obj.containsKey(key);
    }

    public JSONArray getArray(String key) {
        return (JSONArray) obj.get(key);
    }

    public String getString(String key) {
        return String.valueOf(obj.get(key));
    }

    public String getStringOrDefault(String key, String defaultValue) {
        return String.valueOf(obj.getOrDefault(key, defaultValue));
    }

    public boolean isJsonObject(String key) {
        return obj.containsKey(key) && !(obj.get(key) instanceof String);
    }
}
