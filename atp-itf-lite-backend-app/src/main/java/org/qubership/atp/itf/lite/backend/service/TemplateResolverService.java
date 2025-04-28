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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.qubership.atp.crypt.CryptoTools;
import org.qubership.atp.crypt.api.Decryptor;
import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.itf.lite.backend.components.replacer.ContextVariablesReplacer;
import org.qubership.atp.itf.lite.backend.components.replacer.EnvironmentReplacer;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteDecryptException;
import org.qubership.atp.itf.lite.backend.model.api.request.ResolvableRequest;
import org.qubership.atp.itf.lite.backend.model.context.SaveRequestResolvingContext;
import org.qubership.atp.itf.lite.backend.service.macros.SimpleContextWithParametersDecrypting;
import org.qubership.atp.macros.core.processor.Evaluator;
import org.qubership.atp.macros.core.processor.SimpleContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateResolverService {

    private final ContextVariablesReplacer contextVariablesReplacer;
    private final EnvironmentReplacer  environmentReplacer;
    private final Decryptor decryptor;

    @Value("${template.resolver.recursion.depth.max:3}")
    private static int MAX_RECURSION_DEPTH;

    /**
     * Resolves templates in request.
     *
     * @param request request in which need to resolve templates
     * @param context context with variables for replacement
     * @param evaluator macros evaluator
     */
    public void resolveTemplatesWithOrder(ResolvableRequest request,
                                          SaveRequestResolvingContext context,
                                          Evaluator evaluator) {
        List<Function<String, String>> replacers = new ArrayList<>();
        Map<String, Object> mergedScopes = context.mergeScopes();
        if (Objects.nonNull(mergedScopes) && !mergedScopes.isEmpty()) {
            replacers.add((String value) -> contextVariablesReplacer.replace(value, mergedScopes));
        }
        Map<String, Object> environmentContext = context.getEnvironmentVariables();
        replacers.add((String value) -> environmentReplacer.replace(value, environmentContext));

        SimpleContext macrosContext = new SimpleContextWithParametersDecrypting(decryptor);
        macrosContext.setContextParameters(mergedScopes);
        replacers.add((String value) -> evaluator.evaluate(value, macrosContext));
        request.resolveTemplates((String value) -> resolveVariables(value, replacers, 0));
    }

    /**
     * Resolves templates in request.
     *
     * @param request request in which need to resolve templates
     * @param safely if true replaces secret values by stars
     */
    public void processEncryptedValues(ResolvableRequest request,
                                          boolean safely) {

        List<Function<String, String>> replacers = new ArrayList<>();
        if (safely) {
            replacers.add(CryptoTools::maskEncryptedData);
        } else {
            replacers.add(value -> {
                try {
                    return decryptor.decryptEncryptedPlacesInString(value);
                } catch (AtpDecryptException e) {
                    log.error("Error occurred while decrypting value in request", e);
                   throw new ItfLiteDecryptException();
                }
            });
        }
        request.resolveTemplates((String value) -> resolveVariables(value, replacers, 0));
    }

    private String resolveVariables(String value, List<Function<String, String>> replacers, int recursionDepth) {
        if (value == null) {
            return null;
        }
        for (Function<String, String> replacer: replacers) {
            value = replacer.apply(value);
        }
        if (isStringContainsTemplate(value) && recursionDepth < MAX_RECURSION_DEPTH) {
            return resolveVariables(value, replacers, ++recursionDepth);
        }
        return value;
    }

    /**
     * Simple check that the string contains a pattern.
     * May return a false positive.
     * Used for early interruption of recursion.
     *
     * @param value string
     * @return false if the string does not exactly contain a template, true if there can be a template
     */
    private boolean isStringContainsTemplate(String value) {
        return value.contains("$") || value.contains("{");
    }
}
