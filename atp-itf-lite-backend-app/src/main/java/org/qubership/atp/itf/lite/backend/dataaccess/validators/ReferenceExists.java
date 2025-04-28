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

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import org.qubership.atp.itf.lite.backend.service.IdentifiedService;

@Target({FIELD})
@Retention(RUNTIME)
@Constraint(validatedBy = ReferenceValidator.class)
@Documented
public @interface ReferenceExists {
    /**
     * Message in case of violation.
     */
    String message() default "Reference is not found for field: %s with value: %s";
    /**
     * The parameter groups, allowing to define under which circumstances this validation is to be triggered.
     */
    Class<?>[] groups() default {};
    /**
     * The parameter payload, allowing to define a payload to be passed with this validation.
     */
    Class<? extends Payload>[] payload() default {};
    /**
     * Service class which use for get entity.
     */
    Class<? extends IdentifiedService> serviceClass();
}
