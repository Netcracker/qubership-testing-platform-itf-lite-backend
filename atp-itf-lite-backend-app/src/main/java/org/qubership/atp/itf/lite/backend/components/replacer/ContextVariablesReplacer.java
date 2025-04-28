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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.itf.lite.backend.service.EncryptionService;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ContextVariablesReplacer {

    private static final String CONTEXT_VARIABLE_REGEXP = "\\$\\{([^}]+)}|\\{\\{([^}]+)}}";
    private static final Pattern pattern = Pattern.compile(CONTEXT_VARIABLE_REGEXP);

    private final EncryptionService cryptService;

    /**
     * Function for injection context variables into "value".
     *
     * @param value   serialized object
     * @param context context variables
     */
    public String replace(String value,
                          Map<String, Object> context) {
        Matcher matcher = pattern.matcher(value);
        while (matcher.find()) {
            try {
                String match = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                String foundParameter = findParameterByName(match.replace("\\\"", ""), context);
                value = foundParameter == null ? value : value.replace(matcher.group(0), foundParameter);
            } catch (RuntimeException | AtpDecryptException exception) {
                String message = String.format("Error occurred while injecting context variable [%s].\n%s\n%s",
                        matcher.group(0),
                        exception.getClass().getName(),
                        exception.getMessage());
                log.error(message);
                throw new RuntimeException(message);
            }
        }
        return value;
    }

    private String findParameterByName(String parameterName,
                                       Map<String, Object> context) throws AtpDecryptException {
        Object foundValue = context.get(parameterName);
        if (foundValue == null) {
            log.debug(String.format("Parameter %s not found in any variables context", parameterName));
            return null;
        }
        return cryptService.decryptIfEncrypted(foundValue.toString());
    }
}
