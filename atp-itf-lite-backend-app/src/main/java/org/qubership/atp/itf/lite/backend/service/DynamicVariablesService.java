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

package org.qubership.atp.itf.lite.backend.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicVariablesService {

    static final String PRE_SCRIPT_TEMPLATE = "pm.variables.set('%s', pm.variables.replaceIn('%s'));";
    private static final Pattern patternVariables = Pattern.compile("\\{\\{\\$.*?\\}\\}");

    /**
     * Adds new scripts with the found dynamic variables.
     */
    public HttpRequest enrichPreScriptsByDynamicVariables(HttpRequest httpRequest) {
        Set<String> dynamicVariables = getDynamicVariables(
                httpRequest.getPreScripts(), httpRequest.getUrl(),
                httpRequest.getRequestParams() != null
                        ? Arrays.toString(httpRequest.getRequestParams().toArray()) : "",
                httpRequest.getRequestHeaders() != null
                        ? Arrays.toString(httpRequest.getRequestHeaders().toArray()) : "",
                httpRequest.getBody() != null
                        ? httpRequest.getBody().getContent() : "",
                httpRequest.getBody() != null
                        ? httpRequest.getBody().getQuery() : "",
                httpRequest.getBody() != null
                        ? httpRequest.getBody().getVariables() : "",
                (String.valueOf(httpRequest.getAuthorization())));

        httpRequest.setPreScripts(insertDynamicVariablesIntoPreScripts(httpRequest.getPreScripts(), dynamicVariables));
        return httpRequest;
    }

    /**
     * Inserts new scripts with the new dynamic variables found.
     */
    public String insertDynamicVariablesIntoPreScripts(String preScript, Set<String> dynamicVariables) {
        StringBuffer stringBuffer = new StringBuffer();
        if (preScript == null) {
            preScript = "";
        }
        if (dynamicVariables != null) {
            for (String variable : dynamicVariables) {
                String temporary = String.format(PRE_SCRIPT_TEMPLATE,
                        variable.replaceAll("\\{\\{", "")
                                .replaceAll("\\}\\}", ""),
                        variable);
                if (!preScript.contains(temporary)) {
                    stringBuffer.append(temporary + "\r\n");
                }
            }
        }
        stringBuffer.append(preScript);
        return stringBuffer.toString();
    }

    /**
     * Enrich set.
     *
     * @param anyStrings some needed new value variables.
     */
    public static Set<String> getDynamicVariables(String... anyStrings) {
        Set<String> dynamicVariables = new HashSet<>();
        for (String str : anyStrings) {
            Matcher matcher = patternVariables.matcher(String.valueOf(str));
            while (matcher.find()) {
                dynamicVariables.add(matcher.group(0));
            }
        }
        return dynamicVariables;
    }
}
