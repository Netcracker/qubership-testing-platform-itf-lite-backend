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

package org.qubership.atp.itf.lite.backend.service;

import java.nio.charset.StandardCharsets;

import org.qubership.atp.crypt.ConverterTools;
import org.qubership.atp.crypt.api.Decryptor;
import org.qubership.atp.crypt.api.Encryptor;
import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.crypt.exception.AtpEncryptException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EncryptionService {

    @Autowired
    protected Encryptor atpEncryptor;

    @Autowired
    protected Decryptor atpDecryptor;

    public String decrypt(String value) throws AtpDecryptException {
        return atpDecryptor.decrypt(value);
    }

    public String encrypt(String value) throws AtpEncryptException {
        return atpEncryptor.encrypt(value);
    }

    public String decryptIfEncrypted(String value) throws AtpDecryptException {
        return atpDecryptor.decryptIfEncrypted(value);
    }

    public boolean isEncrypted(String value) {
        return atpDecryptor.isEncrypted(value);
    }

    public String decodeBase64(String source) {
        return new String(ConverterTools.decode(source));
    }

    public String encodeBase64(String source) {
        return ConverterTools.encode(source.getBytes(StandardCharsets.UTF_8));
    }
}
