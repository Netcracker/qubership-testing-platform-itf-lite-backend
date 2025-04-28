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

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "ITFL-1014")
public class ItfLiteRequestPostScriptEvaluationException extends ItfLiteException {

    public static final String DEFAULT_MESSAGE = "Failed to evaluate request post-script";

    public ItfLiteRequestPostScriptEvaluationException() {
        super(DEFAULT_MESSAGE);
    }

    public ItfLiteRequestPostScriptEvaluationException(String message) {
        super(DEFAULT_MESSAGE + ": " + message);
    }

    public ItfLiteRequestPostScriptEvaluationException(String message, int line) {
        super(String.format("%s: %s. Line: %d", DEFAULT_MESSAGE, message, line));
    }
}
