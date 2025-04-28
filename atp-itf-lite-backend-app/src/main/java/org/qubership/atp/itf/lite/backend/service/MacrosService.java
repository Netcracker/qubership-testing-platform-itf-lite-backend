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

import java.util.List;
import java.util.UUID;

import javax.script.ScriptEngineManager;

import org.qubership.atp.macros.core.calculator.ScriptMacrosCalculator;
import org.qubership.atp.macros.core.model.Macros;
import org.qubership.atp.macros.core.processor.Evaluator;
import org.qubership.atp.macros.core.processor.EvaluatorImpl;
import org.qubership.atp.macros.core.registry.MacroRegistry;
import org.qubership.atp.macros.core.registry.MacroRegistryImpl;
import org.qubership.atp.macros.core.repository.MacrosRepository;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MacrosService {

    private final MacrosRepository macrosRepository;

    /**
     * Create Evaluator by project uuid.
     */
    public Evaluator createMacrosEvaluator(UUID projectId) {
        List<Macros> macros = macrosRepository.findByProjectId(projectId);
        MacroRegistry macroRegistry = new MacroRegistryImpl(macros);
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        return new EvaluatorImpl(macroRegistry,
                new ScriptMacrosCalculator(scriptEngineManager));
    }
}