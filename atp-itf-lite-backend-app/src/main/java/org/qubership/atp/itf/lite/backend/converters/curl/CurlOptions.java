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

package org.qubership.atp.itf.lite.backend.converters.curl;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

public interface CurlOptions {

    String CURL = "curl";
    String SHORT_DATA = "-d";
    String DATA = "--data";
    String DATA_RAW = "--data-raw";
    String SHORT_HEADER = "-H";
    String HEADER = "--header";
    String SHORT_REQUEST = "-X";
    String REQUEST = "--request";

    String CONTENT_TYPE = "Content-Type";

    String SHORT_FORM = "-F";
    String FORM = "--form";

    String BINARY = "--data-binary";

    Map<String, Integer> optionsMap = ImmutableMap.<String, Integer>builder()
            .put(SHORT_DATA, 1)
            .put(DATA, 1)
            .put(DATA_RAW, 1)
            .put(SHORT_HEADER, 2)
            .put(HEADER, 2)
            .put(SHORT_REQUEST, 3)
            .put(REQUEST, 3)
            .put(SHORT_FORM, 4)
            .put(FORM, 4)
            .put(BINARY, 5)
            .build();
}
