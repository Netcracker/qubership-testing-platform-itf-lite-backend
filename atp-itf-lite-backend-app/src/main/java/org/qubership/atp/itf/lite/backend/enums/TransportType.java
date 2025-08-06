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

package org.qubership.atp.itf.lite.backend.enums;

import lombok.Getter;

@Getter
public enum TransportType {

    REST(TransportTypeNames.REST),
    SOAP(TransportTypeNames.SOAP),
    Diameter(TransportTypeNames.Diameter);

    private String name;

    TransportType(String name) {
        this.name = name;
    }

    public static class TransportTypeNames {
        public static final String REST = "REST";
        public static final String SOAP = "SOAP";
        public static final String Diameter = "Diameter";
    }
}
