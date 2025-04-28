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

package org.qubership.atp.itf.lite.backend.service.macros;

import org.qubership.atp.crypt.api.Decryptor;
import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.itf.lite.backend.exceptions.macros.ItfLiteMacrosAtpDecryptException;
import org.qubership.atp.macros.core.parser.antlr4.MacrosParser;
import org.qubership.atp.macros.core.processor.SimpleContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class SimpleContextWithParametersDecrypting extends SimpleContext {

    private final Decryptor decryptor;

    @Override
    protected String getArgument(MacrosParser.MacroArgContext arg) {
        String argument = super.getArgument(arg);
        try {
            return decryptor.decryptEncryptedPlacesInString(argument);
        } catch (AtpDecryptException e) {
            log.error("Error occurred while decrypting macros argument {}", argument, e);
            throw new ItfLiteMacrosAtpDecryptException("Argument: " + argument);
        }
    }
}
