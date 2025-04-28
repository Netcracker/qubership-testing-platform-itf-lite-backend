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

package org.qubership.atp.itf.lite.backend.components.replacer;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteRequestEnvironmentNotSpecifiedException;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteRequestEnvironmentVariableNotFoundException;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteRequestVariableReplacingException;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnvironmentReplacer {

    private static final String envName = "name";
    private static final Pattern envPattern =
            Pattern.compile("\\$\\{(?i)ENV\\.(?<" + envName + ">[^}]+)\\}");

    /**
     * Replace environment variable in text with provided context.
     *
     * @param text the text in which need to replace the envs
     * @param context context with variables for replacement
     * @return replaced text.
     */
    public String replace(String text, Map<String, Object> context) {
        Matcher matcher = envPattern.matcher(text);
        while (matcher.find()) {
            if (Objects.isNull(context)) {
                // environment context is null only if environmentId not specified in request
                log.error("Found environment variable {}, but environment not specified in request", matcher.group(0));
                throw new ItfLiteRequestEnvironmentNotSpecifiedException();
            }
            text = replace(matcher, text, context);
        }
        return text;
    }


    /**
     * Replace environment variable in text with provided context.
     *
     * @param matcher matcher for replacement
     * @param value the text value in which need to replace the envs
     * @param context context with variables for replacement
     * @return replaced text.
     */
    public String replace(Matcher matcher, String value, Map<String, Object> context) {
        String environmentName = matcher.group(envName).toLowerCase();
        if (context.containsKey(environmentName)) {
            try {
                Object envValue = context.get(environmentName);
                String envValueStr = "null";
                if (Objects.nonNull(envValue)) {
                    envValueStr = envValue.toString();
                }
                value = value.replace(matcher.group(0), envValueStr);
                return value;
            } catch (RuntimeException ex) {
                String message = String.format("Error occurred while injecting environment [%s].\n%s\n%s",
                        matcher.group(0),
                        ex.getClass().getName(),
                        ex.getMessage());
                log.error(message);
                throw new ItfLiteRequestVariableReplacingException(matcher.group(0), ex);
            }
        } else {
            log.error("Found environment variable {}, but environment does not contain such a variable",
                    matcher.group(0));
            throw new ItfLiteRequestEnvironmentVariableNotFoundException(matcher.group(0));
        }
    }
}
