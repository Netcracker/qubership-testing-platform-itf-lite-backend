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

package org.qubership.atp.itf.lite.backend.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestParam;

public class UrlParsingUtils {

    /**
     * Fill request url and url parameters by splitting url token from tokenized string.
     * @param httpRequest httpRequest
     * @param token string token
     */
    public static void parseUrlAndRequestParams(HttpRequest httpRequest, String token) {
        String[] splitOption = token.split("\\?");
        httpRequest.setUrl(splitOption[0]);
        if (splitOption.length > 1) {
            List<RequestParam> requestParams = parseRequestParams(splitOption[1]);
            httpRequest.setRequestParams(requestParams);
        } else {
            httpRequest.setRequestParams(new ArrayList<>());
        }
    }

    public static String getDomain(String url) {
        return url.replaceAll("http(s)?://|www\\.|:.*|/.*", "");
    }

    /**
     * Parse request parameters from url.
     * @param paramsString url parameters string
     * @return list of request params
     */
    public static List<RequestParam> parseRequestParams(String paramsString) {
        List<String> parameters = Arrays.asList(paramsString.split("&"));
        List<RequestParam> params = new ArrayList<>();
        parameters.forEach(parameter -> {
            String[] keyValue = parameter.split("=");
            String key = keyValue[0];
            String value = "";
            if (keyValue.length > 1) {
                value = keyValue[1];
            }
            params.add(new RequestParam(UUID.randomUUID(), key, value, "", false));
        });
        return params;
    }

}
