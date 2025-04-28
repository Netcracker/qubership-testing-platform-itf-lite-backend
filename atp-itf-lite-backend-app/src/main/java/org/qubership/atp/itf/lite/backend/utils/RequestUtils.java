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

import static java.util.Objects.nonNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.Header;
import org.apache.logging.log4j.util.Strings;
import org.qubership.atp.auth.springbootstarter.exceptions.AtpException;
import org.qubership.atp.itf.lite.backend.exceptions.ItfLiteException;
import org.qubership.atp.itf.lite.backend.model.api.Parameter;
import org.qubership.atp.itf.lite.backend.model.api.response.ErrorResponseSerializable;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.model.entities.RequestBody;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.http.FormDataPart;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestParam;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RequestUtils {

    /**
     * Creates copy of provided request with new UUID.
     *
     * @param requestToCopy the request will be copied
     * @return copy of the request
     */
    public static Request copyRequestFromRequest(Request requestToCopy) {
        HttpRequest newRequest = new HttpRequest();
        newRequest.setName(requestToCopy.getName());
        newRequest.setProjectId(requestToCopy.getProjectId());
        newRequest.setFolderId(requestToCopy.getFolderId());
        newRequest.setTransportType(requestToCopy.getTransportType());
        newRequest.setHttpMethod(((HttpRequest) requestToCopy).getHttpMethod());
        newRequest.setUrl(((HttpRequest) requestToCopy).getUrl());
        newRequest.setOrder(requestToCopy.getOrder());
        List<RequestHeader> headers = new ArrayList<>();
        List<RequestHeader> copyRequestHeaders = ((HttpRequest) requestToCopy).getRequestHeaders();
        if (!CollectionUtils.isEmpty(copyRequestHeaders)) {
            for (RequestHeader header : copyRequestHeaders) {
                headers.add(new RequestHeader(header));
            }
        }
        newRequest.setRequestHeaders(headers);
        List<RequestParam> params = new ArrayList<>();
        List<RequestParam> copyRequestParam = ((HttpRequest) requestToCopy).getRequestParams();
        if (!CollectionUtils.isEmpty(copyRequestParam)) {
            for (RequestParam param : copyRequestParam) {
                params.add(new RequestParam(param));
            }
        }
        newRequest.setRequestParams(params);
        newRequest.setBody(copyRequestBody(((HttpRequest) requestToCopy).getBody()));
        RequestAuthorization requestAuthorization = requestToCopy.getAuthorization();
        if (nonNull(requestAuthorization)) {
            newRequest.setAuthorization(requestAuthorization.copy());
        }

        newRequest.setPreScripts(requestToCopy.getPreScripts());
        newRequest.setPostScripts(requestToCopy.getPostScripts());
        newRequest.setDisableSslCertificateVerification(requestToCopy.isDisableSslCertificateVerification());
        newRequest.setDisableSslClientCertificate(requestToCopy.isDisableSslClientCertificate());
        newRequest.setDisableFollowingRedirect(requestToCopy.isDisableFollowingRedirect());
        newRequest.setAutoCookieDisabled(requestToCopy.isAutoCookieDisabled());
        return newRequest;
    }

    /**
     * Creates copy of provided request body.
     *
     * @param body the request body will be copied
     * @return copy of the request
     */
    public static RequestBody copyRequestBody(RequestBody body) {
        RequestBody newBody = null;
        if (nonNull(body)) {
            newBody = new RequestBody(body.getContent(), body.getType());
            newBody.setQuery(body.getQuery());
            newBody.setVariables(body.getVariables());
            if (!CollectionUtils.isEmpty(body.getFormDataBody())) {
                List<FormDataPart> fdps = new ArrayList<>();
                body.getFormDataBody().forEach(fdp -> fdps.add(new FormDataPart(fdp)));
                newBody.setFormDataBody(fdps);
            }
        }
        return newBody;
    }

    /**
     * Creates uri component builder for request's url and params.
     *
     * @param url request url
     * @param requestParams request params
     * @return uri components builder
     */
    public static UriComponentsBuilder buildRequestWithParameters(String url, List<? extends Parameter> requestParams) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(url);
        if (!CollectionUtils.isEmpty(requestParams)) {
            requestParams.forEach(parameter -> {
                if (!parameter.isDisabled()) {
                    uriComponentsBuilder.queryParam(parameter.getKey(), parameter.getValue());
                }
            });
        }

        return uriComponentsBuilder;
    }

    /**
     * Generate ErrorResponse from exception.
     *
     * @param exception Exception
     * @return {@link ErrorResponseSerializable}
     */
    public static ErrorResponseSerializable getErrorResponse(Exception exception) {
        if (exception == null) {
            return null;
        }
        if (!(exception instanceof AtpException)) {
            exception = new ItfLiteException(exception.getMessage());
        }
        ResponseStatus responseStatus =
                AnnotatedElementUtils.findMergedAnnotation(exception.getClass(), ResponseStatus.class);
        HttpStatus status = (responseStatus == null) ? HttpStatus.INTERNAL_SERVER_ERROR : responseStatus.code();
        return ErrorResponseSerializable.serializableBuilder()
                .status(status.value())
                .timestamp(new Date())
                .message(exception.getMessage())
                .reason((responseStatus == null) ? Strings.EMPTY : responseStatus.reason())
                .build();
    }

    /**
     * Calculate size of header array in bytes.
     *
     * @param headers header array.
     * @return size of the headers in bytes
     */
    public static double calculateHeadersSize(Header[] headers) {
        double size = 0.0;
        if (headers == null) {
            return size;
        }
        for (Header header : headers) {
            size += calculateHeaderSize(header);
        }
        return size;
    }

    /**
     * Calculate size of the header in bytes.
     *
     * @param header header.
     * @return size of the header in bytes
     */
    public static double calculateHeaderSize(Header header) {
        if (header == null) {
            return 0.0;
        }
        return header.getName().getBytes(StandardCharsets.UTF_8).length
                + header.getValue().getBytes(StandardCharsets.UTF_8).length;

    }
}
