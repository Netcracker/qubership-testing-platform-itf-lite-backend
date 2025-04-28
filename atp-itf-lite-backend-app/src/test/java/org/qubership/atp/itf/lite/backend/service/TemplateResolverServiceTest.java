package org.qubership.atp.itf.lite.backend.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.crypt.api.Decryptor;
import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.itf.lite.backend.components.replacer.ContextVariablesReplacer;
import org.qubership.atp.itf.lite.backend.components.replacer.EnvironmentReplacer;
import org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator;
import org.qubership.atp.itf.lite.backend.model.api.request.HttpRequestEntitySaveRequest;

@ExtendWith(MockitoExtension.class)
public class TemplateResolverServiceTest {
    private final ThreadLocal<ContextVariablesReplacer> contextVariablesReplacer = new ThreadLocal<>();
    private final ThreadLocal<EnvironmentReplacer> environmentReplacer = new ThreadLocal<>();
    private final ThreadLocal<Decryptor> decryptor = new ThreadLocal<>();
    private final ThreadLocal<TemplateResolverService> templateResolverService = new ThreadLocal<>();


    @BeforeEach
    public void setUp() throws AtpDecryptException {
        ContextVariablesReplacer contextVariablesReplacerMock = mock(ContextVariablesReplacer.class);
        EnvironmentReplacer environmentReplacerMock = mock(EnvironmentReplacer.class);
        Decryptor decryptorMock = mock(Decryptor.class);
        contextVariablesReplacer.set(contextVariablesReplacerMock);
        environmentReplacer.set(environmentReplacerMock);
        decryptor.set(decryptorMock);
        templateResolverService.set(new TemplateResolverService(
                contextVariablesReplacerMock,
                environmentReplacerMock,
                decryptorMock
        ));
    }

    @Test
    public void processEncryptedValues_safely_gotMaskedValues() {
        HttpRequestEntitySaveRequest request = EntitiesGenerator.generateRandomHttpRequestEntitySaveRequest();
        request.setUrl("{ENC}{9s637qdqkLj6xCvA5yXzQw==}{QBvxG0Rfmp+N360gW7HKuA==}");
        templateResolverService.get().processEncryptedValues(request, true);
        Assertions.assertEquals(request.getUrl(), "********");
    }

    @Test
    public void processEncryptedValues_notSafely_gotDecryptedValues() throws AtpDecryptException {
        HttpRequestEntitySaveRequest request = EntitiesGenerator.generateRandomHttpRequestEntitySaveRequest();
        request.setUrl("{ENC}{9s637qdqkLj6xCvA5yXzQw==}{QBvxG0Rfmp+N360gW7HKuA==}");
        when(decryptor.get().decryptEncryptedPlacesInString(any())).thenReturn("test");
        templateResolverService.get().processEncryptedValues(request, false);
        Assertions.assertEquals(request.getUrl(), "test");
    }

}
