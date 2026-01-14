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

package org.qubership.atp.itf.lite.backend.converters;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.itf.lite.backend.converters.curl.CurlOptions;
import org.qubership.atp.itf.lite.backend.enums.ValueType;
import org.qubership.atp.itf.lite.backend.model.api.request.HttpRequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpHeaderSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpParamSaveRequest;
import org.qubership.atp.itf.lite.backend.model.entities.FileBody;
import org.qubership.atp.itf.lite.backend.model.entities.RequestBody;
import org.qubership.atp.itf.lite.backend.model.entities.http.FormDataPart;
import org.qubership.atp.itf.lite.backend.model.entities.http.methods.HttpMethod;
import org.qubership.atp.itf.lite.backend.utils.RequestUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RequestToCurlFormatConverter implements CurlOptions {

    /**
     * Convert request to curl string builder.
     *
     * @param httpRequest httpRequest
     * @return curl string builder
     */
    public String convertRequestToCurlStringBuilder(HttpRequestEntitySaveRequest httpRequest) {
        StringBuilder curlString = initialCurlRequest();

        addRequestMethod(curlString, httpRequest.getHttpMethod());
        addRequestHeaders(curlString, httpRequest.getRequestHeaders());

        if (httpRequest.getBody() != null) {
            addRequestBody(curlString, httpRequest.getBody());
        }

        addUrlWithParameters(curlString, httpRequest.getUrl(), httpRequest.getRequestParams());
        return curlString.toString();
    }

    /**
     * Create new string builder with "curl ".
     *
     * @return string builder
     */
    public StringBuilder initialCurlRequest() {
        return new StringBuilder(CURL + " ");
    }

    /**
     * Add request headers to curl string builder request.
     *
     * @param currentCurlRequest curl string builder
     * @param headers            list of headers
     */
    public void addRequestHeaders(StringBuilder currentCurlRequest, List<HttpHeaderSaveRequest> headers) {
        if (CollectionUtils.isEmpty(headers)) {
            return;
        }

        headers.forEach(header -> {
            if (!header.isDisabled() && !StringUtils.isEmpty(header.getKey())) {
                if (StringUtils.isEmpty(header.getValue())) {
                    currentCurlRequest.append(String.format("-H \"%s;\" ", header.getKey()));
                } else {
                    currentCurlRequest.append(String.format("-H \"%s: %s\" ", header.getKey(), header.getValue()));
                }
            }
        });
    }

    /**
     * Add request http method to curl string builder request.
     *
     * @param currentCurlRequest curl string builder
     * @param method             http method
     */
    public void addRequestMethod(StringBuilder currentCurlRequest, HttpMethod method) {
        currentCurlRequest
                .append(SHORT_REQUEST)
                .append(" ")
                .append(method.name())
                .append(" ");
    }

    /**
     * Add request body to curl string builder request.
     *
     * @param currentCurlRequest curl string builder
     * @param body               request body
     */
    public void addRequestBody(StringBuilder currentCurlRequest, RequestBody body) {
        if (Objects.nonNull(body) && Objects.nonNull(body.getType())) {
            switch (body.getType()) {
                case FORM_DATA:
                    addFormDataRequestBody(currentCurlRequest, body.getFormDataBody());
                    break;
                case Binary:
                    FileBody fileBody = body.getBinaryBody();
                    if (Objects.nonNull(fileBody) && !StringUtils.isEmpty(fileBody.getFileName())) {
                        currentCurlRequest.append(String.format("--data-binary \"@%s\" ", fileBody.getFileName()));
                    }
                    break;
                case GraphQL:
                    currentCurlRequest
                            .append(SHORT_DATA)
                            .append(" '")
                            .append(body.composeGraphQlBody())
                            .append("' ");
                    break;
                default:
                    if (!StringUtils.isEmpty(body.getContent())) {
                        currentCurlRequest
                                .append(SHORT_DATA)
                                .append(" '")
                                .append(body.getContent())
                                .append("' ");
                    }
                    break;
            }
        }
    }

    private void addFormDataRequestBody(StringBuilder curlBuilder, List<FormDataPart> formDataParts) {
        if (!CollectionUtils.isEmpty(formDataParts)) {
            for (FormDataPart formDataPart : formDataParts) {
                if (formDataPart.isDisabled()) {
                    continue;
                }
                curlBuilder.append(String.format("-F '%s=", formDataPart.getKey()));
                if (ValueType.FILE.equals(formDataPart.getType())) {
                    curlBuilder.append("@");
                }
                curlBuilder.append(formDataPart.getValue());
                if (!StringUtils.isEmpty(formDataPart.getContentType())) {
                    curlBuilder.append(";type=").append(formDataPart.getContentType());
                }
                curlBuilder.append("' ");
            }
        }
    }

    /**
     * Add request url and request parameters to curl string builder request.
     * Need to add url at the end of string.
     *
     * @param currentCurlRequest curl string builder
     * @param url                url
     * @param requestParams      request parameters
     */
    public void addUrlWithParameters(StringBuilder currentCurlRequest, String url,
                                     List<HttpParamSaveRequest> requestParams) {
        UriComponentsBuilder uriComponentsBuilder = RequestUtils.buildRequestWithParameters(url, requestParams);
        currentCurlRequest
                .append("'")
                .append(uriComponentsBuilder.encode().toUriString())
                .append("'");
    }
}
