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

package org.qubership.atp.itf.lite.backend.dataaccess.validators;

import static org.qubership.atp.itf.lite.backend.utils.Constants.COPY_POSTFIX;

import java.util.UUID;

import org.qubership.atp.itf.lite.backend.dataaccess.repository.FolderRepository;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderUpsetRequest;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FolderCreationRequestValidator implements Validator {

    private final FolderRepository folderRepository;

    @Override
    public boolean supports(Class<?> clazz) {
        return FolderUpsetRequest.class.equals(clazz);
    }

    @Override
    public void validate(Object obj, Errors errors) {
        FolderUpsetRequest request = (FolderUpsetRequest) obj;

        UUID projectId = request.getProjectId();
        String folderName = request.getName();

        boolean isFolderExists = folderRepository.existsFolderByProjectIdAndName(projectId, folderName);

        if (isFolderExists) {
            String errorMessage = "Folder with name " + folderName + " already exists in the database";
            log.error(errorMessage);
            folderName += COPY_POSTFIX;
            request.setName(folderName);
            String notificationMessage = "Set new folder name \"" + folderName + "\" for entity creation";
            log.info(notificationMessage);
        }
    }
}
