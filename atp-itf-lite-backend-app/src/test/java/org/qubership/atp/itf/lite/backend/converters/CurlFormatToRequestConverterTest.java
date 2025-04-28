package org.qubership.atp.itf.lite.backend.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRandomHttpRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.qubership.atp.itf.lite.backend.enums.ValueType;
import org.qubership.atp.itf.lite.backend.enums.http.RequestBodyType;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.model.entities.RequestBody;
import org.qubership.atp.itf.lite.backend.model.entities.http.FormDataPart;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestParam;
import org.qubership.atp.itf.lite.backend.model.entities.http.methods.HttpMethod;

public class CurlFormatToRequestConverterTest {

    CurlFormatToRequestConverter curlConverter = new CurlFormatToRequestConverter();

    @Test
    public void convertCurlToRequest_curlStringWithCustomAndXmlHeadersSpecified_shouldSuccessfullyConvert() {
        // given
        HttpRequest httpRequest = generateRandomHttpRequest();
        HttpRequest expectedRequest = new HttpRequest(httpRequest);
        expectedRequest.setRequestParams(Collections.singletonList(new RequestParam(UUID.randomUUID(),
                "novalue", "", "", false)));
        expectedRequest.setRequestHeaders(Arrays.asList(
                new RequestHeader(UUID.randomUUID(), "Custom-Header", "", "", false),
                new RequestHeader(UUID.randomUUID(), "Content-Type", "text/xml", "", false)));
        expectedRequest.setBody(
                new RequestBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?><note></note>", RequestBodyType.XML));
        String curlString = "curl -X POST -H \"Custom-Header\" -H \"Content-Type: text/xml\" -d" +
                " '<?xml version=\"1.0\" encoding=\"UTF-8\"?><note></note>' 'http://test.test?novalue'";

        // when
        HttpRequest actualRequest = curlConverter.convertCurlStringToRequest(httpRequest, curlString);

        // then
        compareRequestsParameters(expectedRequest, actualRequest);
    }

    @Test
    public void convertCurlToRequest_curlStringWithGraphQlJsonBodySpecified_shouldSuccessfullyConvert() {
        // given
        HttpRequest httpRequest = generateRandomHttpRequest();
        HttpRequest expectedRequest = new HttpRequest(httpRequest);
        expectedRequest.setHttpMethod(HttpMethod.POST);
        expectedRequest.setUrl("http://test.test/api/graphql-server/graphql");
        expectedRequest.setRequestParams(new ArrayList<>());
        expectedRequest.setRequestHeaders(new ArrayList<>());
        expectedRequest.getRequestHeaders().add(new RequestHeader("Content-Type", "application/json", "", false));
        expectedRequest.getRequestHeaders().add(new RequestHeader("Authorization", "Bearer {{authToken}}", "", false));
        RequestBody expectedBody = new RequestBody();
        expectedBody.setContent("{\"query\":\"query searchBillingAccount($filter: [String!]) {\\r\\n    searchBillingAccount @ filter(filters: $filter) {\\r\\n        id\\r\\n        name\\r\\n        billingMethod{\\r\\n            id\\r\\n            name\\r\\n        }\\r\\n        accountNumber\\r\\n        status\\r\\n        customer{\\r\\n            id\\r\\n            name\\r\\n            }\\r\\n        relatedProducts{\\r\\n            id\\r\\n            name\\r\\n            status\\r\\n            }\\r\\n\\r\\n    }\\r\\n}\\r\\n\",\"variables\":{\"filter\":[\"msisdn=590110865\"]}}");
        expectedBody.setQuery("query searchBillingAccount($filter: [String!]) {\n"
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
        expectedBody.setVariables("{\"filter\":[\"msisdn=590110865\"]}");
        expectedBody.setType(RequestBodyType.GraphQL);
        expectedRequest.setBody(expectedBody);
        // TODO: Discuss if we should set requestMethod (if not present in curl) based on --data parameter.
        String curlString = "curl -X POST --location --globoff 'http://test.test/api/graphql-server/graphql' \\\n"
                + "--header 'Content-Type: application/json' \\\n"
                + "--header 'Authorization: Bearer {{authToken}}' \\\n"
                + "--data '{\"query\":\"query searchBillingAccount($filter: [String!]) {\\r\\n    searchBillingAccount @ filter(filters: $filter) {\\r\\n        id\\r\\n        name\\r\\n        billingMethod{\\r\\n            id\\r\\n            name\\r\\n        }\\r\\n        accountNumber\\r\\n        status\\r\\n        customer{\\r\\n            id\\r\\n            name\\r\\n            }\\r\\n        relatedProducts{\\r\\n            id\\r\\n            name\\r\\n            status\\r\\n            }\\r\\n\\r\\n    }\\r\\n}\\r\\n\",\"variables\":{\"filter\":[\"msisdn=590110865\"]}}'";

        // when
        HttpRequest actualRequest = curlConverter.convertCurlStringToRequest(httpRequest, curlString);

        // then
        compareRequestsParameters(expectedRequest, actualRequest);
    }

    @Test
    public void convertCurlToRequest_curlStringSpecified_shouldSuccessfullyConvert() {
        // given
        HttpRequest httpRequest = generateRandomHttpRequest();
        HttpRequest expectedRequest = new HttpRequest(httpRequest);
        expectedRequest.setHttpMethod(HttpMethod.POST);
        expectedRequest.setUrl("http://test.test/");
        expectedRequest.setRequestParams(new ArrayList<>());
        expectedRequest.getRequestParams().add(new RequestParam("name", "name", "", false));
        expectedRequest.setRequestHeaders(new ArrayList<>());
        expectedRequest.getRequestHeaders().add(new RequestHeader("Content-Type", "application/json", "", false));
        RequestBody expectedBody = new RequestBody();
        expectedBody.setContent("{\"id\": \"123\"}");
        expectedBody.setType(RequestBodyType.JSON);
        expectedRequest.setBody(expectedBody);
        String curlString = "curl -X POST -H \"Content-Type: application/json\" -d '{\"id\": \"123\"}' " +
                "'http://test.test/?name=name'";

        // when
        HttpRequest actualRequest = curlConverter.convertCurlStringToRequest(httpRequest, curlString);
        // then
        compareRequestsParameters(expectedRequest, actualRequest);
    }

    @Test
    public void convertCurlToRequest_curlStringWithoutOptionsValuesSpecified_shouldSuccessfullyConvert() {
        // given
        HttpRequest httpRequest = generateRandomHttpRequest();
        HttpRequest expectedRequest = new HttpRequest(httpRequest);
        expectedRequest.setRequestParams(new ArrayList<>());
        expectedRequest.setRequestHeaders(new ArrayList<>());
        expectedRequest.setHttpMethod(HttpMethod.GET);
        expectedRequest.setBody(null);
        // after -d follows --unknown option, url will not be used as request body
        String curlString = "curl -X -H -d --unknown 'http://test.test'";

        // when
        Request actualRequest = curlConverter.convertCurlStringToRequest(httpRequest, curlString);

        // then
        assertEquals(expectedRequest, actualRequest);
    }

    @Test
    public void convertCurlToRequest_curlStringWithUnknownOptionSpecified_shouldSuccessfullyConvert() {
        // given
        HttpRequest httpRequest = generateRandomHttpRequest();
        HttpRequest expectedRequest = new HttpRequest(httpRequest);
        expectedRequest.setHttpMethod(HttpMethod.POST);
        expectedRequest.setUrl("http://test.test/");
        expectedRequest.setRequestParams(new ArrayList<>());
        expectedRequest.getRequestParams().add(new RequestParam("name", "name", "", false));
        expectedRequest.setRequestHeaders(new ArrayList<>());
        expectedRequest.getRequestHeaders().add(new RequestHeader("Content-Type", "application/json", "", false));
        RequestBody expectedBody = new RequestBody();
        expectedBody.setContent("{\"id\": \"123\"}");
        expectedBody.setType(RequestBodyType.JSON);
        expectedRequest.setBody(expectedBody);
        String curlString = "curl --unknownOption unknownOptionValue -X POST -H \"Content-Type: application/json\" -d" +
                " '{\"id\": \"123\"}' 'http://test.test/?name=name'";

        // when
        HttpRequest actualRequest = curlConverter.convertCurlStringToRequest(httpRequest, curlString);

        // then
        compareRequestsParameters(expectedRequest, actualRequest);
    }

    @Test
    public void convertCurlToRequest_curlWithFormdata_shouldSuccessfullyConvert() {
        // given
        HttpRequest httpRequest = generateRandomHttpRequest();
        HttpRequest expectedRequest = new HttpRequest(httpRequest);
        expectedRequest.setRequestHeaders(new ArrayList<>());
        expectedRequest.setRequestParams(new ArrayList<>());
        expectedRequest.setHttpMethod(HttpMethod.POST);
        expectedRequest.setUrl("http://test.test/");
        RequestBody expectedBody = new RequestBody();
        List<FormDataPart> formDataParts = new ArrayList<>();
        formDataParts.add(new FormDataPart("text", ValueType.TEXT, "text", null, null, "", false));
        formDataParts.add(new FormDataPart("textWithContentType", ValueType.TEXT, "text", null, "plain/text", "", false));
        expectedBody.setFormDataBody(formDataParts);
        expectedBody.setType(RequestBodyType.FORM_DATA);
        expectedRequest.setBody(expectedBody);
        String curlString = "curl -X POST -F \"text=text\" -F \"textWithContentType=text;type=plain/text\" "
                + "-F \"file=@file.txt\" -F \"fileWithContentType=@file.txt;type=plain/text\" 'http://test.test/'";

        // when
        HttpRequest actualRequest = curlConverter.convertCurlStringToRequest(httpRequest, curlString);

        // then
        compareRequestsParameters(expectedRequest, actualRequest);
    }

    @Test
    public void convertCurlToRequest_curlWithBinary_shouldSuccessfullyConvert_binaryBodySkipped() {
        // given
        HttpRequest httpRequest = generateRandomHttpRequest();
        HttpRequest expectedRequest = new HttpRequest(httpRequest);
        expectedRequest.setRequestHeaders(new ArrayList<>());
        expectedRequest.setRequestParams(new ArrayList<>());
        expectedRequest.setHttpMethod(HttpMethod.POST);
        expectedRequest.setUrl("http://test.test/");
        expectedRequest.setBody(null);
        String curlString = "curl -X POST --data-binary \"@file.txt\" 'http://test.test/'";

        // when
        HttpRequest actualRequest = curlConverter.convertCurlStringToRequest(httpRequest, curlString);

        // then
        compareRequestsParameters(expectedRequest, actualRequest);
    }

    private void compareRequestsParameters(HttpRequest expectedRequest, HttpRequest actualRequest) {
        assertEquals(expectedRequest.getProjectId(), actualRequest.getProjectId());
        assertEquals(expectedRequest.getFolderId(), actualRequest.getFolderId());
        assertEquals(expectedRequest.getTransportType(), actualRequest.getTransportType());
        assertEquals(expectedRequest.getHttpMethod(), actualRequest.getHttpMethod());
        assertEquals(expectedRequest.getUrl(), actualRequest.getUrl());
        assertEquals(expectedRequest.getRequestHeaders().size(), actualRequest.getRequestHeaders().size());
        assertEquals(expectedRequest.getRequestParams().size(), actualRequest.getRequestParams().size());
        if (Objects.nonNull(expectedRequest.getBody())) {
            assertEquals(expectedRequest.getBody().getType(), actualRequest.getBody().getType());
            if (RequestBodyType.GraphQL.equals(expectedRequest.getBody().getType())) {
                assertEquals(expectedRequest.getBody().getContent().replace("\r\n", "\n"),
                        actualRequest.getBody().getContent().replace("\r\n", "\n"));
                assertEquals(expectedRequest.getBody().getQuery().replace("\r\n", "\n"),
                        actualRequest.getBody().getQuery().replace("\r\n", "\n"));
                assertEquals(expectedRequest.getBody().getVariables().replace("\r\n", "\n"),
                        actualRequest.getBody().getVariables().replace("\r\n", "\n"));
            } else {
                assertEquals(expectedRequest.getBody(), actualRequest.getBody());
            }
        } else {
            assertNull(actualRequest.getBody());
        }
    }

}
