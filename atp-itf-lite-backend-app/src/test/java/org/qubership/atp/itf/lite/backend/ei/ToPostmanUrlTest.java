/*
 * # Copyright 2026 NetCracker Technology Corporation
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

package org.qubership.atp.itf.lite.backend.ei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.qubership.atp.itf.lite.backend.model.ei.ToPostmanMapDescriptionAndDisabled;
import org.qubership.atp.itf.lite.backend.model.ei.ToPostmanRequest;
import org.qubership.atp.itf.lite.backend.model.ei.ToPostmanUrl;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestParam;

class ToPostmanUrlTest {

    private static final String BASE_URL = "https://example.com/resource";

    @Test
    void exportsPostmanQuery() {
        HttpRequest request = new HttpRequest();
        request.setUrl(BASE_URL);
        request.setRequestParams(List.of(
                new RequestParam("key", "value", "description", false)));

        ToPostmanUrl url = new ToPostmanRequest(request).getUrl();

        assertEquals(BASE_URL + "?key=value", url.getRaw());
        assertEquals(1, url.getQuery().size());
        ToPostmanMapDescriptionAndDisabled queryParam =
                assertInstanceOf(ToPostmanMapDescriptionAndDisabled.class, url.getQuery().getFirst());
        assertEquals("key", queryParam.getKey());
        assertEquals("value", queryParam.getValue());
        assertEquals("description", queryParam.getDescription());
        assertFalse(queryParam.isDisabled());
    }

    @Test
    void encodesUnicodeAndSpecialCharacters() {
        String key = "ключ name&[]";
        String value = "тест value&[]=✓";
        HttpRequest request = new HttpRequest();
        request.setUrl(BASE_URL);
        request.setRequestParams(List.of(
                new RequestParam(key, value, null, false)));

        ToPostmanUrl url = new ToPostmanRequest(request).getUrl();
        String expected = BASE_URL
                + "?%D0%BA%D0%BB%D1%8E%D1%87%20name%26%5B%5D="
                + "%D1%82%D0%B5%D1%81%D1%82%20value%26%5B%5D%3D%E2%9C%93";

        assertEquals(expected, url.getRaw());
        assertEquals(key, url.getQuery().getFirst().getKey());
        assertEquals(value, url.getQuery().getFirst().getValue());
    }

    @Test
    void encodesEmailSpecialCharacters() {
        HttpRequest request = new HttpRequest();
        request.setUrl(BASE_URL);
        request.setRequestParams(List.of(
                new RequestParam("email", "john+test@example.com", null, false)));

        ToPostmanUrl url = new ToPostmanRequest(request).getUrl();

        assertEquals(BASE_URL + "?email=john%2Btest%40example.com", url.getRaw());
    }

    @Test
    void encodesKeyWithoutValue() {
        HttpRequest request = new HttpRequest();
        request.setUrl(BASE_URL);
        request.setRequestParams(List.of(
                new RequestParam("key with space", null, null, false)));

        ToPostmanUrl url = new ToPostmanRequest(request).getUrl();

        assertEquals(BASE_URL + "?key%20with%20space", url.getRaw());
    }
}
