package org.qubership.atp.itf.lite.backend.dataaccess.validators;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.service.FolderService;
import org.springframework.context.ApplicationContext;

@ExtendWith(MockitoExtension.class)
public class ReferenceValidatorTest {

    @Mock
    ApplicationContext applicationContext;

    @Mock
    private FolderService folderService;

    private ReferenceValidator referenceValidator;

    @Mock
    private ReferenceExists referenceExists;

    private UUID folderId;

    private void prepareReferenceValidator() {
        when(applicationContext.getBean(FolderService.class)).thenAnswer(answer -> folderService);
        when(referenceExists.serviceClass()).thenAnswer(answer -> FolderService.class);
        referenceValidator.initialize(referenceExists);
    }

    @BeforeEach
    public void setUp() {
        referenceValidator = new ReferenceValidator(applicationContext);
        folderId = UUID.randomUUID();
    }

    @Test
    void isValid_folderIdIsNull_shouldBeValid() {
        // given, when
        boolean isValid = referenceValidator.isValid(null, null);
        // then
        Assertions.assertTrue(isValid);
    }

    @Test
    void isValid_folderExists_shouldBeValid() {
        // given, when
        prepareReferenceValidator();
        when(folderService.isEntityExists(any())).thenReturn(true);
        boolean isValid = referenceValidator.isValid(folderId, null);
        // then
        Assertions.assertTrue(isValid);
    }

    @Test
    void isValid_folderDoesNotExist_shouldBeInvalid() {
        // given, when
        prepareReferenceValidator();
        when(folderService.isEntityExists(any())).thenReturn(false);
        boolean isValid = referenceValidator.isValid(folderId, null);
        // then
        Assertions.assertFalse(isValid);
    }

    @Test
    void isValid_listOfFoldersExist_shouldBeValid() {
        // given, when
        prepareReferenceValidator();
        when(folderService.isEntityExists(any())).thenReturn(true);
        boolean isValid = referenceValidator.isValid(Collections.singletonList(folderId), null);
        // then
        Assertions.assertTrue(isValid);
    }

    @Test
    void isValid_notUuidInstance_shouldBeInvalid() {
        // given
        Folder folder = new Folder();
        // when
        prepareReferenceValidator();
        boolean isValid = referenceValidator.isValid(folder, null);
        // then
        Assertions.assertFalse(isValid);
    }
}