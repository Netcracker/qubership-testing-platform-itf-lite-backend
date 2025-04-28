package org.qubership.atp.itf.lite.backend.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRandomHttpRequestEntitySaveRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.qubership.atp.itf.lite.backend.enums.ValueType;
import org.qubership.atp.itf.lite.backend.enums.http.RequestBodyType;
import org.qubership.atp.itf.lite.backend.model.api.request.HttpRequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpHeaderSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpParamSaveRequest;
import org.qubership.atp.itf.lite.backend.model.entities.FileBody;
import org.qubership.atp.itf.lite.backend.model.entities.RequestBody;
import org.qubership.atp.itf.lite.backend.model.entities.http.FormDataPart;
import org.qubership.atp.itf.lite.backend.model.entities.http.methods.HttpMethod;

public class RequestToCurlFormatConverterTest {

    private final static RequestToCurlFormatConverter requestToCurlFormatConverter = new RequestToCurlFormatConverter();

    @Test
    void convertRequest_postRequestWithVelocityTypeSpecified_shouldSuccessfullyConvert() {
        // given
        String bodyString = "{\"id\":\"123\"}";
        String expectedResult = "curl -X POST -H \"Content-Type: application/json\" -d '{\"id\":\"123\"}' 'http://test.test?name=name'";
        RequestBody body = new RequestBody(bodyString, RequestBodyType.Velocity);
        HttpRequestEntitySaveRequest httpRequest = generateRandomHttpRequestEntitySaveRequest();
        httpRequest.setBody(body);

        // when
        String actualResult = requestToCurlFormatConverter.convertRequestToCurlStringBuilder(httpRequest);
        // then
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void convertRequest_requestWithJsonTypeSpecified_shouldSuccessfullyConvert() {
        // given
        String expectedResult = "curl -X POST -H \"Content-Type: application/json\" -d '{\"id\": \"123\"}' 'http://test.test?name=name'";
        HttpRequestEntitySaveRequest httpRequest = generateRandomHttpRequestEntitySaveRequest();

        // when
        String actualResult = requestToCurlFormatConverter.convertRequestToCurlStringBuilder(httpRequest);

        // then
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void convertRequest_requestWithXmlTypeAndWithoutParametersSpecified_shouldSuccessfullyConvert() {
        // given
        String expectedResult = "curl -X POST -H \"Content-Type: text/xml\" -d " +
                "'<?xml version=\"1.0\" encoding=\"UTF-8\"?><note></note>' 'http://test.test'";
        HttpRequestEntitySaveRequest httpRequest = generateRandomHttpRequestEntitySaveRequest();
        httpRequest.setRequestHeaders(Collections.singletonList(
                new HttpHeaderSaveRequest("Content-Type", "text/xml", "", false)));
        httpRequest.setBody(
                new RequestBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?><note></note>", RequestBodyType.XML));
        httpRequest.setRequestParams(new ArrayList<>());

        // when
        String actualResult = requestToCurlFormatConverter.convertRequestToCurlStringBuilder(httpRequest);

        // then
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void convertRequest_getRequestWithEmptyHeadersSpecified_shouldSuccessfullyConvert() {
        // given
        String expectedResult = "curl -X GET 'http://test.test'";
        HttpRequestEntitySaveRequest httpRequest = generateRandomHttpRequestEntitySaveRequest();
        httpRequest.setHttpMethod(HttpMethod.GET);
        httpRequest.setRequestHeaders(new ArrayList<>());
        httpRequest.setBody(null);
        httpRequest.setRequestParams(new ArrayList<>());

        // when
        String actualResult = requestToCurlFormatConverter.convertRequestToCurlStringBuilder(httpRequest);

        // then
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void convertRequest_requestsWithNoContentTypeSpecified_shouldSuccessfullyConvert() {
        // given
        HttpRequestEntitySaveRequest xmlRequest = new HttpRequestEntitySaveRequest();
        HttpRequestEntitySaveRequest jsonRequest = new HttpRequestEntitySaveRequest();
        String xmlExpectedRequest = "curl -X POST -d '<note></note>' 'http://test.test'";
        String jsonExpectedRequest = "curl -X POST -d '{\"name\": \"name\"}' 'http://test.test'";
        xmlRequest.setHttpMethod(HttpMethod.POST);
        xmlRequest.setBody(new RequestBody("<note></note>", RequestBodyType.XML));
        xmlRequest.setUrl("http://test.test");
        jsonRequest.setHttpMethod(HttpMethod.POST);
        jsonRequest.setBody(new RequestBody("{\"name\": \"name\"}", RequestBodyType.JSON));
        jsonRequest.setUrl("http://test.test");

        // when
        String xmlActualRequest = requestToCurlFormatConverter.convertRequestToCurlStringBuilder(xmlRequest);
        String jsonActualRequest = requestToCurlFormatConverter.convertRequestToCurlStringBuilder(jsonRequest);
        // then
        assertEquals(xmlExpectedRequest, xmlActualRequest);
        assertEquals(jsonExpectedRequest, jsonActualRequest);
    }

    @Test
    public void convertRequest_requestWithDisabledAndNonDisabledHeadersAndParams_shouldSuccessfullyConvert() {
        // given
        HttpRequestEntitySaveRequest request = new HttpRequestEntitySaveRequest();
        String expectedRequest = "curl -X POST -H \"Header1: headerValue1\" -H \"Header3: headerValue3\" -d '{\"name\": \"name\"}' "
                + "'http://test.test?param1=value1&param3=value3'";
        request.setHttpMethod(HttpMethod.POST);
        request.setBody(new RequestBody("{\"name\": \"name\"}", RequestBodyType.JSON));
        request.setUrl("http://test.test");
        request.setRequestHeaders(Arrays.asList(
                new HttpHeaderSaveRequest("Header1", "headerValue1", "", false),
                new HttpHeaderSaveRequest("Header2", "headerValue2", "", true),
                new HttpHeaderSaveRequest("Header3", "headerValue3", "", false)));
        request.setRequestParams(Arrays.asList(
                new HttpParamSaveRequest("param1", "value1", "", false),
                new HttpParamSaveRequest("param2", "value2", "", true),
                new HttpParamSaveRequest("param3", "value3", "", false)));

        // when
        String actualRequest = requestToCurlFormatConverter.convertRequestToCurlStringBuilder(request);

        // then
        assertEquals(expectedRequest, actualRequest);
    }

    @Test
    public void convertRequest_requestWithGraphQlTypeSpecified_shouldSuccessfullyConvert() {
        // given
        String expectedResult = "curl -X POST -H \"Content-Type: application/json\" -H \"Authorization: Bearer {{authToken}}\" -d '{\"variables\":{\"filter\":[\"msisdn=590110865\"]},\"query\":\"query searchBillingAccount($filter: [String!]) {     searchBillingAccount @ filter(filters: $filter) {         id         name         billingMethod{             id             name         }         accountNumber         status         customer{             id             name             }         relatedProducts{             id             name             status             }      } } \"}' 'http://test.test/api/graphql-server/graphql'";
        HttpRequestEntitySaveRequest httpRequest = generateRandomHttpRequestEntitySaveRequest();
        httpRequest.setHttpMethod(HttpMethod.POST);
        httpRequest.setUrl("http://test.test/api/graphql-server/graphql");
        httpRequest.setRequestParams(new ArrayList<>());
        httpRequest.setRequestHeaders(new ArrayList<>());
        httpRequest.getRequestHeaders().add(new HttpHeaderSaveRequest("Content-Type", "application/json", "", false));
        httpRequest.getRequestHeaders().add(new HttpHeaderSaveRequest("Authorization", "Bearer {{authToken}}", "", false));
        RequestBody body = new RequestBody();
        body.setContent("{\"query\":\"query searchBillingAccount($filter: [String!]) {\\r\\n    searchBillingAccount @ filter(filters: $filter) {\\r\\n        id\\r\\n        name\\r\\n        billingMethod{\\r\\n            id\\r\\n            name\\r\\n        }\\r\\n        accountNumber\\r\\n        status\\r\\n        customer{\\r\\n            id\\r\\n            name\\r\\n            }\\r\\n        relatedProducts{\\r\\n            id\\r\\n            name\\r\\n            status\\r\\n            }\\r\\n\\r\\n    }\\r\\n}\\r\\n\",\"variables\":{\"filter\":[\"msisdn=590110865\"]}}");
        body.setQuery("query searchBillingAccount($filter: [String!]) {\n"
                + "    searchBillingAccount @ filter(filters: $filter) {\n"
                + "        id\n"
                + "        name\n"
                + "        billingMethod{\n"
                + "            id\n"
                + "            name\n"
                + "        }\n"
                + "        accountNumber\n"
                + "        status\n"
                + "        customer{\n"
                + "            id\n"
                + "            name\n"
                + "            }\n"
                + "        relatedProducts{\n"
                + "            id\n"
                + "            name\n"
                + "            status\n"
                + "            }\n"
                + "\n"
                + "    }\n"
                + "}\n");
        body.setVariables("{\"filter\":[\"msisdn=590110865\"]}");
        body.setType(RequestBodyType.GraphQL);
        httpRequest.setBody(body);

        // when
        String actualResult = requestToCurlFormatConverter.convertRequestToCurlStringBuilder(httpRequest);

        // then
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void convertRequest_requestWithFormData_shouldSuccessfullyConvert() {
        // given
        HttpRequestEntitySaveRequest request = new HttpRequestEntitySaveRequest();
        String expectedRequest = "curl -X POST -F 'text1=text1' -F 'text2=text2;type=plain/text' -F 'file1=@text1.txt' "
                + "-F 'file2=@text2.txt;type=plain/text' -F 'default1=default1' -F 'default2=default1;type=plain/text' 'http://test.test'";
        request.setHttpMethod(HttpMethod.POST);
        request.setBody(new RequestBody(Arrays.asList(
                // not disabled text type without Content-Type
                new FormDataPart("text1", ValueType.TEXT, "text1", null, null, null, false),
                // not disabled text type with Content-Type
                new FormDataPart("text2", ValueType.TEXT, "text2", null, "plain/text", null, false),
                // disabled text type
                new FormDataPart("text3", ValueType.TEXT, "text3", null, "plain/text", null, true),
                // not disabled file type without Content-Type
                new FormDataPart("file1", ValueType.FILE, "text1.txt", null, null, null, false),
                // not disabled file type with Content-Type
                new FormDataPart("file2", ValueType.FILE, "text2.txt", null, "plain/text", null, false),
                // disabled file type
                new FormDataPart("file3", ValueType.FILE, "text3.txt", null, "plain/text", null, true),
                // not disabled default type without Content-Type
                new FormDataPart("default1", ValueType.TEXT, "default1", null, null, null, false),
                // not disabled default type with Content-Type
                new FormDataPart("default2", ValueType.DEFAULT, "default1", null, "plain/text", null, false),
                // disabled default type
                new FormDataPart("default3", ValueType.DEFAULT, "default1", null, "plain/text", null, true)),
                RequestBodyType.FORM_DATA));
        request.setUrl("http://test.test");

        // when
        String actualRequest = requestToCurlFormatConverter.convertRequestToCurlStringBuilder(request);

        // then
        assertEquals(expectedRequest, actualRequest);
    }

    @Test
    public void convertRequest_requestWithBinary_shouldSuccessfullyConvert() {
        // given
        HttpRequestEntitySaveRequest request = new HttpRequestEntitySaveRequest();
        String expectedRequest = "curl -X POST --data-binary \"@fileName.txt\" 'http://test.test'";
        request.setHttpMethod(HttpMethod.POST);
        request.setBody(new RequestBody(new FileBody("fileName.txt", UUID.randomUUID()), RequestBodyType.Binary));
        request.setUrl("http://test.test");

        // when
        String actualRequest = requestToCurlFormatConverter.convertRequestToCurlStringBuilder(request);

        // then
        assertEquals(expectedRequest, actualRequest);
    }

}
