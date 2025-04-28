package org.qubership.atp.itf.lite.backend.components.replacer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.itf.lite.backend.service.EncryptionService;

public class ContextVariablesReplacerTest {

    private final ThreadLocal<EncryptionService> cryptService = new ThreadLocal<>();
    private final ThreadLocal<ContextVariablesReplacer> replacer = new ThreadLocal<>();

    @BeforeEach
    public void setUp() {
        EncryptionService cryptServiceMock = mock(EncryptionService.class);
        cryptService.set(cryptServiceMock);
        replacer.set(new ContextVariablesReplacer(cryptServiceMock));
    }

    @Test
    public void testReplace_replaceExistingParametersFromContext_successfullyReplaced() {
        Map<String, Object> map = new HashMap<>();
        map.put("parameter1", "value");
        mockExtContextVariablesValues(map);
        String result = replacer.get().replace("Send request ${parameter1}", map);
        Assertions.assertEquals("Send request value", result);
    }
    @Test
    public void testReplace_replaceNonExistingParametersFromContext_nothingHasBeenReplaced() {
        Map<String, Object> map = new HashMap<>();
        map.put("parameter1", "value");
        mockExtContextVariablesValues(map);
        String result = replacer.get().replace("Send request ${parameter2}", map);
        Assertions.assertEquals("Send request ${parameter2}", result);
    }

    @Test
    public void testReplace_replaceExistingParametersFromContext_whenAnotherTemplatesExists_successfullyReplaced() {
        Map<String, Object> map = new HashMap<>();
        map.put("parameter1", "value1");
        mockExtContextVariablesValues(map);
        String result = replacer.get().replace("Send request ${parameter1} $macros $tc.test", map);
        Assertions.assertEquals("Send request value1 $macros $tc.test", result);
    }

    @Test
    public void testReplace_replaceExistingParametersFromContextWithDots_successfullyReplaced() {
        Map<String, Object> map = new HashMap<>();
        map.put("param.id.1", "value");
        mockExtContextVariablesValues(map);
        String result = replacer.get().replace("Send request ${param.id.1}", map);
        Assertions.assertEquals("Send request value", result);
    }

    @Test
    public void testReplaceDoubleQuote_replaceExistingParametersFromContext_successfullyReplaced() {
        Map<String, Object> map = new HashMap<>();
        map.put("parameter1", "value");
        mockExtContextVariablesValues(map);
        String result = replacer.get().replace("Send request {{parameter1}}", map);
        Assertions.assertEquals("Send request value", result);
    }
    @Test
    public void testReplaceDoubleQuote_replaceNonExistingParametersFromContext_nothingHasBeenReplaced() {
        Map<String, Object> map = new HashMap<>();
        map.put("parameter1", "value");
        mockExtContextVariablesValues(map);
        String result = replacer.get().replace("Send request {{parameter2}}", map);
        Assertions.assertEquals("Send request {{parameter2}}", result);
    }

    @Test
    public void testReplaceDoubleQuote_replaceExistingParametersFromContext_whenAnotherTemplatesExists_successfullyReplaced() {
        Map<String, Object> map = new HashMap<>();
        map.put("parameter1", "value1");
        mockExtContextVariablesValues(map);
        String result = replacer.get().replace("Send request {{parameter1}} $macros $tc.test", map);
        Assertions.assertEquals("Send request value1 $macros $tc.test", result);
    }

    @Test
    public void testReplaceDoubleQuote_replaceExistingParametersFromContextWithDots_successfullyReplaced() {
        Map<String, Object> map = new HashMap<>();
        map.put("param.id.1", "value");
        mockExtContextVariablesValues(map);
        String result = replacer.get().replace("Send request {{param.id.1}}", map);
        Assertions.assertEquals("Send request value", result);
    }

    @Test
    public void testReplaceDoubleQuoteAndDefault_replaceExistingParametersFromContext_whenAnotherTemplatesExists_successfullyReplaced() {
        Map<String, Object> map = new HashMap<>();
        map.put("parameter1", "value1");
        map.put("parameter2", "value2");
        mockExtContextVariablesValues(map);
        String result = replacer.get().replace("Send request {{parameter1}} ${parameter2} $macros $tc.test", map);
        Assertions.assertEquals("Send request value1 value2 $macros $tc.test", result);
        result = replacer.get().replace("Send request {{parameter1}} {{parameter1}} ${parameter2} ${parameter2} $macros $tc.test", map);
        Assertions.assertEquals("Send request value1 value1 value2 value2 $macros $tc.test", result);
    }

    private void mockExtContextVariablesValues(Map<String, Object> map) {
        map.values().forEach(value -> {
            try {
                when(cryptService.get().decryptIfEncrypted((String)value)).thenReturn((String)value);
            } catch (AtpDecryptException e) {
                e.printStackTrace();
            }
        });
    }

}
