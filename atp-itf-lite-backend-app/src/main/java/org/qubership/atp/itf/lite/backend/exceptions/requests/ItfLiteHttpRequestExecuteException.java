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

package org.qubership.atp.itf.lite.backend.exceptions.requests;

import org.qubership.atp.itf.lite.backend.exceptions.ItfLiteException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "ITFL-1002")
public class ItfLiteHttpRequestExecuteException extends ItfLiteException {

    public static final String DEFAULT_MESSAGE = "Failed to execute HTTP request";

    public ItfLiteHttpRequestExecuteException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Atp Http request exception.
     */
    public ItfLiteHttpRequestExecuteException(HttpClientErrorException httpClientErrorException) {
        super(DEFAULT_MESSAGE + String.format("\nCode:%s\n Message:%s",
                httpClientErrorException.getResponseBodyAsString(),
                httpClientErrorException.getStatusCode().value()));
    }

    public ItfLiteHttpRequestExecuteException(Exception exception) {
        super(DEFAULT_MESSAGE + String.format("\n%s", exception.getMessage()));
    }

    public ItfLiteHttpRequestExecuteException(Throwable throwable) {
        super(DEFAULT_MESSAGE + String.format("\n%s", throwable.getMessage()));
    }
}
