package org.qubership.atp.itf.lite.backend.model.api.response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.qubership.atp.itf.lite.backend.enums.ContextVariableType;
import org.qubership.atp.itf.lite.backend.model.api.request.ContextVariable;
import org.qubership.atp.itf.lite.backend.model.context.SaveRequestResolvingContext;

public class RequestExecutionResponseTest {

    @Test
    public void parseAndSetContextVariables_variablesExist_fiveVariablesInContext() {
        SaveRequestResolvingContext context = SaveRequestResolvingContext.builder()
                .environmentVariables(new HashMap<>())
                .build();
        context.getEnvironment().put("env.variable", "{env.variable}");
        context.getVariables().put("test.variable", "{test.variable}");
        context.getCollectionVariables().put("collection.variable", "{collection.variable}");
        context.getGlobals().put("global.variable", "{global.variable}");
        context.getEnvironmentVariables().put("test.env.system", "{test.env.system}");
        RequestExecutionResponse requestExecutionResponse = new RequestExecutionResponse();

        requestExecutionResponse.parseAndSetContextVariables(context);

        Assertions.assertEquals(5, requestExecutionResponse.getContextVariables().size());
        Assertions.assertEquals(requestExecutionResponse.getContextVariables(), expectedVariables());
    }

    private List<ContextVariable> expectedVariables() {
        List<ContextVariable> expectedVariables = new ArrayList<>();
        expectedVariables.add(new ContextVariable("global.variable", "{global.variable}", ContextVariableType.GLOBAL));
        expectedVariables.add(new ContextVariable("collection.variable", "{collection.variable}", ContextVariableType.COLLECTION));
        expectedVariables.add(new ContextVariable("test.env.system", "{test.env.system}", ContextVariableType.ENVIRONMENT));
        expectedVariables.add(new ContextVariable("env.variable", "{env.variable}", ContextVariableType.ENVIRONMENT));
        expectedVariables.add(new ContextVariable("test.variable", "{test.variable}", ContextVariableType.LOCAL));
        return expectedVariables;
    }

    @Test
    public void parseAndSetContextVariables_variablesNotExist_null() {
        SaveRequestResolvingContext context = SaveRequestResolvingContext.builder()
                .environmentVariables(new HashMap<>())
                .build();
        RequestExecutionResponse requestExecutionResponse = new RequestExecutionResponse();

        requestExecutionResponse.parseAndSetContextVariables(context);

        Assertions.assertNull(requestExecutionResponse.getContextVariables());
    }

    @Test
    public void parseAndSetContextVariables_SaveRequestResolvingContextNotExist_null() {
        RequestExecutionResponse requestExecutionResponse = new RequestExecutionResponse();

        requestExecutionResponse.parseAndSetContextVariables(null);

        Assertions.assertNull(requestExecutionResponse.getContextVariables());
    }
}
