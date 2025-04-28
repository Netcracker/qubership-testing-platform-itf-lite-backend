package org.qubership.atp.itf.lite.backend.components.replacer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteRequestEnvironmentVariableNotFoundException;
import org.qubership.atp.itf.lite.backend.service.EncryptionService;

@ExtendWith(MockitoExtension.class)
public class EnvironmentReplacerTest {

    @Mock
    private EncryptionService encryptionService;
    @InjectMocks
    private EnvironmentReplacer environmentReplacer;

    @Test
    public void replaceTest_allEnvironmentsShouldBeReplaced() {
        Map<String, Object> context = new HashMap<>();
        context.put("qa.http.user", "user");
        context.put("qa.http.password", "secure_password");
        String result = environmentReplacer.replace(
                "User: ${ENV.QA.http.user}, Password: ${ENV.QA.http.password}",
             context);
        Assertions.assertEquals("User: user, Password: secure_password", result);
    }

    @Test
    public void replaceTest_NoEnvironmentsInContext_throwException() {
        Map<String, Object> context = new HashMap<>();
        assertThrows(ItfLiteRequestEnvironmentVariableNotFoundException.class,
                () -> environmentReplacer.replace(
                "User: ${ENV.QA.http.user}, Password: ${EncryptedParameterENV.QA.http.password}",
                context));
    }

    private void mockEncryptService_decrypt(String value) {
        try {
            when(encryptionService.decrypt(value)).thenReturn(value);
        } catch (AtpDecryptException e) {
            e.printStackTrace();
        }
    }
}
