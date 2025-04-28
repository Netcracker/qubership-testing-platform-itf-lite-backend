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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.qubership.atp.auth.springbootstarter.config.FeignConfiguration;
import org.qubership.atp.itf.lite.backend.feign.clients.ItfVelocityBalancerFeignClient;
import org.qubership.atp.itf.lite.backend.feign.dto.ResponseObjectDto;
import org.qubership.atp.itf.lite.backend.feign.dto.UIVelocityRequestBodyDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslResponse;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactProviderRule;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import lombok.extern.slf4j.Slf4j;

@RunWith(SpringRunner.class)
@EnableFeignClients(clients = {ItfVelocityBalancerFeignClient.class})
@ContextConfiguration(classes = {ItfVelocityFeignClientPactUnitTest.TestApp.class})
@Import({JacksonAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        FeignConfiguration.class,
        FeignAutoConfiguration.class,
        HttpClientConfiguration.class})
@TestPropertySource(properties = {"feign.atp.itf.name=atp-itf-executor", "feign.atp.itf.url=http://localhost:8889",
        "feign.atp.itf.route="})
@Slf4j
public class ItfVelocityFeignClientPactUnitTest {

    @Configuration
    public static class TestApp {
    }

    @Rule
    public PactProviderRule mockProvider
            = new PactProviderRule("atp-itf-executor", "localhost", 8889, this);

    @Autowired
    private ItfVelocityBalancerFeignClient itfVelocityBalancerFeignClient;
    private final UUID projectUuid = UUID.fromString("39cae351-9e3b-4fb6-a384-1c3616f4e76f");

    @Test
    @PactVerification()
    public void allPass() {
        ResponseEntity<ResponseObjectDto> result
                = itfVelocityBalancerFeignClient.get(projectUuid, null, getRequest());
        Assert.assertEquals(200, result.getStatusCode().value());
        Assert.assertTrue(result.getHeaders().get("Content-Type").contains("application/json"));
        Assert.assertEquals(result.getBody(), getResponse());
    }

    @Pact(consumer = "atp-itf-lite")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        DslPart requestBody = new PactDslJsonBody()
                .stringType("context", "9167234930111872001")
                .stringType("message","message test")
                .stringType("tc","")
                .stringType("sp","");

        DslPart responseBody = new PactDslJsonBody()
                .stringValue("response", "value");

        PactDslResponse response = builder
                .given("all ok")
                .uponReceiving("PUT /velocity OK")
                .path("/velocity")
                .query("projectUuid=" + projectUuid)
                .method("PUT")
                .headers(headers)
                .body(requestBody)
                .willRespondWith()
                .headers(headers)
                .body(responseBody)
                .status(200);

        return response.toPact();
    }

    private UIVelocityRequestBodyDto getRequest() {
        UIVelocityRequestBodyDto requestBodyDto = new UIVelocityRequestBodyDto();
        requestBodyDto.setContext("9167234930111872001");
        requestBodyDto.setMessage("message test");
        requestBodyDto.setTc("");
        requestBodyDto.setSp("");
        return requestBodyDto;
    }

    private ResponseObjectDto getResponse() {
        ResponseObjectDto responseObjectDto = new ResponseObjectDto();
        responseObjectDto.setResponse("value");
        return responseObjectDto;
    }

}
