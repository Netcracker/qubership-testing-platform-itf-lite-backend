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

package org.qubership.atp.itf.lite.backend.model.api.response;

import java.util.UUID;

import org.qubership.atp.itf.lite.backend.enums.ImportCollectionError;
import org.qubership.atp.itf.lite.backend.enums.ImportCollectionStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportCollectionsResponse {
    private String requestName;
    private UUID requestId;
    private String collectionName;
    private String comment;
    private ImportCollectionStatus importStatus;
    private ImportCollectionError errorType;
    private UUID formDataPartId;
}
