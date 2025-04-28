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

package org.qubership.atp.itf.lite.backend.components.auth;

import static java.util.Objects.nonNull;

import java.util.function.Consumer;

import org.codehaus.commons.compiler.util.Producer;
import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.crypt.exception.AtpEncryptException;
import org.qubership.atp.itf.lite.backend.service.EncryptionService;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractAuthorizationStrategy implements RequestAuthorizationStrategy {

    public static final String TO_ENCRYPT_FLAG = "{2ENC}";
    private static final String IS_ENCRYPTED_FLAG = "{ENC}";

    protected final EncryptionService encryptionService;

    protected static final String AUTH_HEADER_KEY = "Authorization";
    protected static final String CALCULATED_VALUE = "<calculated when request is sent>";

    protected void decodeParameter(Producer<String> getter, Consumer<String> setter) {
        String paramValue = getter.produce();
        if (nonNull(paramValue) && paramValue.startsWith(TO_ENCRYPT_FLAG)) {
            paramValue = paramValue.replace(TO_ENCRYPT_FLAG, "");
            paramValue = encryptionService.decodeBase64(paramValue);
            setter.accept(paramValue);
        }
    }

    protected void decryptParameter(Producer<String> getter, Consumer<String> setter) {
        String paramValue = getter.produce();
        if (nonNull(paramValue) && paramValue.startsWith(IS_ENCRYPTED_FLAG)) {
            try {
                paramValue = encryptionService.decrypt(paramValue);
            } catch (AtpDecryptException err) {
                log.error("Failed to decrypt parameter for request authorization", err);
                throw new IllegalStateException("Failed to decrypt parameter for request authorization");
            }
            setter.accept(paramValue);
        }
    }

    protected void encryptParameter(Producer<String> getter, Consumer<String> setter) {
        String paramValue = getter.produce();
        if (nonNull(paramValue) && paramValue.startsWith(TO_ENCRYPT_FLAG)) {
            decodeParameter(getter, setter);
            paramValue = getter.produce();
            try {
                paramValue = encryptionService.encrypt(paramValue);
            } catch (AtpEncryptException err) {
                log.error("Failed to encrypt parameter for request authorization", err);
                throw new IllegalStateException("Failed to encrypt parameter for request authorization");
            }
            setter.accept(paramValue);
        }
    }
}