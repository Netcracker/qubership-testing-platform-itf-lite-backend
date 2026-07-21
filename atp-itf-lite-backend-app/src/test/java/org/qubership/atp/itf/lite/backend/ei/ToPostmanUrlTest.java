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
    void constructor_urlWithRequestParams_exportsPostmanQuery() {
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
}
