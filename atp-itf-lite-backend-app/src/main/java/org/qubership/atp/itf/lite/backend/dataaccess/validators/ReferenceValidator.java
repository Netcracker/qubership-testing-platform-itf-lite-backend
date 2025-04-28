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

import java.util.List;
import java.util.UUID;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.qubership.atp.itf.lite.backend.service.IdentifiedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

public class ReferenceValidator implements ConstraintValidator<ReferenceExists, Object> {
    private final ApplicationContext applicationContext;
    public Class<? extends IdentifiedService> serviceClass;

    @Autowired
    public ReferenceValidator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void initialize(ReferenceExists constraintAnnotation) {
        this.serviceClass = constraintAnnotation.serviceClass();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        IdentifiedService service = applicationContext.getBean(serviceClass);

        if (value instanceof List) {
            return ((List)value)
                    .stream()
                    .allMatch(service::isEntityExists);
        } else if (value instanceof UUID) {
            return service.isEntityExists(value);
        }

        return false;
    }
}
