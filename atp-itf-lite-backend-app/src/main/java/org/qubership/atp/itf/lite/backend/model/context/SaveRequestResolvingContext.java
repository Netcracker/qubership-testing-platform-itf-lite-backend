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

package org.qubership.atp.itf.lite.backend.model.context;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qubership.atp.itf.lite.backend.enums.ContextScope;
import org.qubership.atp.itf.lite.backend.model.api.request.ContextVariable;
import org.qubership.atp.itf.lite.backend.model.api.response.environments.System;
import org.springframework.util.CollectionUtils;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SaveRequestResolvingContext {

    @Builder.Default
    private Map<String, Object> globals = new LinkedHashMap<>();
    @Builder.Default
    private Map<String, Object> collectionVariables = new LinkedHashMap<>();
    @Builder.Default
    private Map<String, Object> environment = new LinkedHashMap<>();
    @Builder.Default
    private Map<String, Object> iterationData = new LinkedHashMap<>();
    @Builder.Default
    private Map<String, Object> variables = new LinkedHashMap<>();

    // environment values from environment service
    private Map<String, Object> environmentVariables;

    private static final Pattern contextKeysPattern = Pattern.compile(
            "^(?<scope>ITF_LITE_GLOBALS_|"
                    + "ITF_LITE_COLLECTIONVARIABLES_|"
                    + "ITF_LITE_ENVIRONMENT_|"
                    + "ITF_LITE_ITERATIONDATA_)(?<key>.*)");

    /**
     * Parses the context and looks for parameters with a scope prefix.
     * If a parameter is found, truncates the prefix and adds the parameter to the result map
     *
     * @param context context
     * @param scope   desired scope
     * @return context for provided scope
     */
    public static Map<String, Object> parseScope(Map<String, Object> context, ContextScope scope) {
        Map<String, Object> parsedScope = new HashMap<>();
        if (Objects.nonNull(context)) {
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                Matcher matcher = contextKeysPattern.matcher(key);
                if (scope == ContextScope.LOCAL_VARIABLES) {
                    if (!matcher.matches()) {
                        // put to local scope context parameters without scope prefix only
                        parsedScope.put(key, value);
                    }
                } else {
                    if (matcher.matches()) {
                        // put parameter with scope prefix only if non-local scope provided
                        String variableScope = matcher.group("scope");
                        String parsedKey = matcher.group("key");
                        if (variableScope.equals(scope.getPrefix())) {
                            parsedScope.put(parsedKey, value);
                        }
                    }
                }
            }
        }
        return parsedScope;
    }

    /**
     * Collect map systems from list systems.
     */
    public static Map<String, Object> parseSystems(List<System> systems) {
        Map<String, Object> environmentVariables = new HashMap<>();
        if (Objects.isNull(systems)) {
            return environmentVariables;
        }
        systems.stream()
                .filter(s -> !CollectionUtils.isEmpty(s.getConnections()))
                .forEach(s -> s.getConnections().stream()
                        .filter(c -> !CollectionUtils.isEmpty(c.getParameters()))
                        .forEach(c -> c.getParameters()
                                .forEach((k, v) -> environmentVariables.put(
                                        String.format("%s.%s.%s", s.getName(), c.getName(), k).toLowerCase(), v))));
        return environmentVariables;
    }

    /**
     * Collect fields class to scope.
     */
    public Map<String, Object> mergeScopes() {
        Map<String, Object> mergedScope = new HashMap<>();
        mergedScope.putAll(globals);
        mergedScope.putAll(collectionVariables);
        mergedScope.putAll(environment);
        mergedScope.putAll(iterationData);
        mergedScope.putAll(variables);
        return mergedScope;
    }

    /**
     * Collect fields class to scope with prefix.
     */
    public Map<String, Object> mergeWithScopePrefixes() {
        Map<String, Object> mergedScope = new HashMap<>();
        globals.forEach((k, v) -> mergedScope.put(ContextScope.GLOBALS.getPrefix() + k, v));
        collectionVariables.forEach((k, v) -> mergedScope.put(ContextScope.COLLECTION.getPrefix() + k, v));
        environment.forEach((k, v) -> mergedScope.put(ContextScope.ENVIRONMENT.getPrefix() + k, v));
        iterationData.forEach((k, v) -> mergedScope.put(ContextScope.DATA.getPrefix() + k, v));
        mergedScope.putAll(variables);
        return mergedScope;
    }

    /**
     * Add context by classify.
     */
    public void parseAndClassifyContextVariables(List<ContextVariable> contextVariables) {
        if (!CollectionUtils.isEmpty(contextVariables)) {
            for (ContextVariable contextVariable : contextVariables) {
                switch (contextVariable.getContextVariableType()) {
                    case GLOBAL:
                        globals.put(contextVariable.getKey(), contextVariable.getValue());
                        break;
                    case COLLECTION:
                        collectionVariables.put(contextVariable.getKey(), contextVariable.getValue());
                        break;
                    case DATA:
                        iterationData.put(contextVariable.getKey(), contextVariable.getValue());
                        break;
                    case ENVIRONMENT:
                        environment.put(contextVariable.getKey(), contextVariable.getValue());
                        break;
                    case LOCAL:
                        variables.put(contextVariable.getKey(), contextVariable.getValue());
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
