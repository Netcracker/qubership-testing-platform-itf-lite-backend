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

import static org.qubership.atp.itf.lite.backend.converters.curl.CurlOptions.CONTENT_TYPE;

import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.springframework.http.MediaType;

public class CurlUtils {

    /**
     * Check if header is "Content-Type: application/xml" or "Content-Type: text/xml".
     * @param header request header
     * @return true/false
     */
    public static boolean isHeaderXmlContentType(RequestHeader header) {
        return CONTENT_TYPE.equals(header.getKey())
                && (MediaType.APPLICATION_XML_VALUE.equals(header.getValue())
                || MediaType.TEXT_XML_VALUE.equals(header.getValue()));
    }

    /**
     * Check if header is "Content-Type: application/json".
     * @param header request header
     * @return true/false
     */
    public static boolean isHeaderJsonContentType(RequestHeader header) {
        return CONTENT_TYPE.equals(header.getKey())
                && MediaType.APPLICATION_JSON_VALUE.equals(header.getValue());
    }
}
