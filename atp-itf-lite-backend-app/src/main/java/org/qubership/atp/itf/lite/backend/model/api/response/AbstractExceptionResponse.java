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

import java.util.Date;

import org.springframework.http.HttpStatus;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public abstract class AbstractExceptionResponse {

    protected Date timestamp;

    protected int status;

    protected String error;

    protected String path;

    /**
     * AbstractExceptionResponse constructor.
     *
     * @param path error path
     */
    public AbstractExceptionResponse(String path) {
        this.timestamp = new Date();
        this.status = HttpStatus.BAD_REQUEST.value();
        this.error = HttpStatus.BAD_REQUEST.getReasonPhrase();
        this.path = path;
    }
}
