package org.qubership.atp.itf.lite.backend.dataaccess.validators;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.qubership.atp.itf.lite.backend.utils.Constants.COPY_POSTFIX;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.FolderRepository;
import org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderUpsetRequest;

@ExtendWith(MockitoExtension.class)
class FolderCreationRequestValidatorTest {

    private final ThreadLocal<FolderRepository> folderRepository = new ThreadLocal<>();
    private final ThreadLocal<FolderCreationRequestValidator> validator = new ThreadLocal<>();

    @BeforeEach
    public void setUp() {
        FolderRepository folderRepositoryMock = mock(FolderRepository.class);
        folderRepository.set(folderRepositoryMock);
        validator.set(new FolderCreationRequestValidator(folderRepositoryMock));
    }

    @Test
    void supports_folderCreationRequestClassIsSpecified_shouldSupports() {
        // given
        Class<FolderUpsetRequest> creationRequestClass = FolderUpsetRequest.class;

        // when
        boolean isSupports = validator.get().supports(creationRequestClass);

        // then
        Assertions.assertTrue(isSupports);
    }

    @Test
    void validate_correctFolderCreationRequestSpecified_shouldValidateWithoutErrors() {
        // given
        final FolderUpsetRequest folderCreationRequest =
                EntitiesGenerator.createFolderUpsetRequest("New folder");
        final UUID projectId = folderCreationRequest.getProjectId();
        final String requestFolderName = folderCreationRequest.getName();

        // when
        when(folderRepository.get().existsFolderByProjectIdAndName(
                eq(projectId), eq(requestFolderName))).thenReturn(false);

        validator.get().validate(folderCreationRequest, null);

        // then
        verify(folderRepository.get()).existsFolderByProjectIdAndName(
                eq(projectId), eq(folderCreationRequest.getName()));
    }

    @Test
    void validate_incorrectFolderCreationRequestSpecified_shouldValidateAndUpdateDuplicatedFolderName() {
        // given
        final FolderUpsetRequest folderCreationRequest =
                EntitiesGenerator.createFolderUpsetRequest("New folder");
        UUID projectId = folderCreationRequest.getProjectId();
        String requestFolderName = folderCreationRequest.getName();
        when(folderRepository.get().existsFolderByProjectIdAndName(eq(projectId), eq(requestFolderName)))
                .thenReturn(true);

        // when
        validator.get().validate(folderCreationRequest, null);

        // then
        Assertions.assertEquals(requestFolderName + COPY_POSTFIX, folderCreationRequest.getName());
    }
}
