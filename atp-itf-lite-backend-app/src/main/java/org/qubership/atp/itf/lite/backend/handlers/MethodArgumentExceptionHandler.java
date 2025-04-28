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

package org.qubership.atp.itf.lite.backend.handlers;

import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.qubership.atp.itf.lite.backend.model.api.response.ArgumentNotValidExceptionResponse;
import org.qubership.atp.itf.lite.backend.model.api.response.ArgumentNotValidExceptionResponse.FieldError;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
@ResponseBody
public class MethodArgumentExceptionHandler {

    /**
     * Handle MethodArgumentNotValidException exception.
     *
     * @param request request
     * @param ex      exception
     * @return exception info result
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ArgumentNotValidExceptionResponse handleArgumentException(HttpServletRequest request,
                                                                     MethodArgumentNotValidException ex) {
        BindingResult result = ex.getBindingResult();

        List<FieldError> fieldErrors = result.getFieldErrors()
                .stream()
                .map(fieldError -> new FieldError(fieldError.getField(), fieldError.getDefaultMessage()))
                .collect(Collectors.toList());

        return new ArgumentNotValidExceptionResponse(request.getServletPath(), fieldErrors);
    }
}
