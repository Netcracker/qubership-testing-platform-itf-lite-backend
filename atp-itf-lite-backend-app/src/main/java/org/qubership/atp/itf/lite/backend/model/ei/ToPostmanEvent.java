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

package org.qubership.atp.itf.lite.backend.model.ei;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ToPostmanEvent {

    private ToPostmanListen listen;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ToPostmanScript script;

    public static ToPostmanEvent prerequest(String value) {
        return new ToPostmanEvent(ToPostmanListen.PREREQUEST, new ToPostmanScript(value));
    }

    public static ToPostmanEvent test(String value) {
        return new ToPostmanEvent(ToPostmanListen.TEST, new ToPostmanScript(value));
    }
}
