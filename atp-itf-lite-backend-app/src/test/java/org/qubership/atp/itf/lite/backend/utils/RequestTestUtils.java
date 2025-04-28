package org.qubership.atp.itf.lite.backend.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngineManager;

import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.qubership.atp.itf.lite.backend.model.context.SaveRequestResolvingContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.atp.macros.core.calculator.ScriptMacrosCalculator;
import org.qubership.atp.macros.core.model.Macros;
import org.qubership.atp.macros.core.processor.Evaluator;
import org.qubership.atp.macros.core.processor.EvaluatorImpl;
import org.qubership.atp.macros.core.registry.MacroRegistry;
import org.qubership.atp.macros.core.registry.MacroRegistryImpl;

public class RequestTestUtils {

    public static Evaluator generateEvaluator() throws IOException {
        File macros = new File("src/test/resources/tests/macros.json");
        String macrosString = new String(Files.readAllBytes(macros.toPath()));
        List<Macros> macrosList = Arrays.asList(new ObjectMapper().readValue(macrosString, Macros[].class));
        MacroRegistry macroRegistry = new MacroRegistryImpl(macrosList);
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        scriptEngineManager.registerEngineName("javascript", new NashornScriptEngineFactory());
        return new EvaluatorImpl(macroRegistry, new ScriptMacrosCalculator(scriptEngineManager));
    }

    public static SaveRequestResolvingContext generateContext() {
        Map<String, Object> localVariables = new HashMap<>();
        localVariables.put("localContextVar", "localValue");
        Map<String, Object> dataVariables = new HashMap<>();
        dataVariables.put("localContextVar", "data1");
        dataVariables.put("iterDataVar", "dataValue");
        Map<String, Object> collectionVariables = new HashMap<>();
        collectionVariables.put("localContextVar", "collection1");
        collectionVariables.put("iterDataVar", "collection2");
        collectionVariables.put("collectionVar", "collectionValue");
        Map<String, Object> globalVariables = new HashMap<>();
        globalVariables.put("localContextVar", "global1");
        globalVariables.put("iterDataVar", "global2");
        globalVariables.put("collectionVar", "global3");
        globalVariables.put("globalVar", "globalValue");

        return SaveRequestResolvingContext.builder()
                .variables(localVariables)
                .iterationData(dataVariables)
                .collectionVariables(collectionVariables)
                .globals(globalVariables).build();
    }
}
