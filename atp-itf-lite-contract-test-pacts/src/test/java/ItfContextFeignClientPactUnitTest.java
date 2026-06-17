/*
 * # Copyright 2024-2026 NetCracker Technology Corporation
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.auth.springbootstarter.config.FeignConfiguration;
import org.qubership.atp.itf.lite.backend.feign.clients.ItfContextBalancerFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslResponse;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;

@ExtendWith(SpringExtension.class)
@EnableFeignClients(clients = {ItfContextBalancerFeignClient.class})
@ExtendWith(PactConsumerTestExt.class)
@Import({JacksonAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        FeignConfiguration.class,
        FeignAutoConfiguration.class})
@TestPropertySource(properties = {
        "feign.atp.itf.name=atp-itf-executor",
        "feign.atp.itf.url=http://localhost:8888",
        "feign.atp.itf.route="})
@PactTestFor(providerName = "atp-itf-executor", port = "8888", pactVersion = PactSpecVersion.V3)
public class ItfContextFeignClientPactUnitTest {

    @Autowired
    private ItfContextBalancerFeignClient itfContextBalancerFeignClient;
    private final String contextId = "9167234930111872000";
    private final UUID projectUuid = UUID.fromString("39cae351-9e3b-4fb6-a384-1c3616f4e76f");

    @Test
    @PactTestFor(pactMethod = "createPact")
    public void allPass() {
        ResponseEntity<String> result = itfContextBalancerFeignClient.get(contextId, projectUuid);
        Assertions.assertEquals(200, result.getStatusCode().value());
        Assertions.assertTrue(Objects.requireNonNull(result.getHeaders().get("Content-Type"))
                .contains("application/json"));
        Assertions.assertEquals(result.getBody(), getResponse());
    }

    @Pact(consumer = "atp-itf-lite")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        DslPart responseBody = new PactDslJsonBody()
                .stringValue("status", "STARTED");

        PactDslResponse response = builder
                .given("all ok")
                .uponReceiving("GET /context/get OK")
                .path("/context/get")
                .query("id=" + contextId + "&projectUuid=" + projectUuid)
                .method("GET")
                .willRespondWith()
                .headers(headers)
                .body(responseBody)
                .status(200);

        return response.toPact();
    }

    private String getResponse() {
        return "{\"status\":\"STARTED\"}";
    }
}
