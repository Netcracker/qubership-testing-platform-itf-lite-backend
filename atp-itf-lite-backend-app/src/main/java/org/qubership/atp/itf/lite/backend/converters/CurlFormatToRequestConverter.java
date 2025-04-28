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

import static org.openjdk.nashorn.tools.Shell.tokenizeString;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.qubership.atp.itf.lite.backend.converters.curl.CurlOptions;
import org.qubership.atp.itf.lite.backend.converters.curl.CurlUtils;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.enums.ValueType;
import org.qubership.atp.itf.lite.backend.enums.http.RequestBodyType;
import org.qubership.atp.itf.lite.backend.exceptions.converters.BadFormDataFormatException;
import org.qubership.atp.itf.lite.backend.model.entities.RequestBody;
import org.qubership.atp.itf.lite.backend.model.entities.http.FormDataPart;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.qubership.atp.itf.lite.backend.model.entities.http.methods.HttpMethod;
import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.qubership.atp.itf.lite.backend.utils.UrlParsingUtils;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CurlFormatToRequestConverter implements CurlOptions {

    /* curl request could contain slash as separator
    example:
        curl --request POST \
        --header 'Content-Type: text/html'
     */
    private static final String lineSeparatorPattern = "\\\\?(\\r\\n|\\r|\\n)";

    private static final String KEY_GROUP_NAME = "key";
    private static final String VALUE_GROUP_NAME = "value";
    private static final String IS_FILE_GROUP_NAME = "isFile";
    private static final String CONTENT_TYPE_GROUP_NAME = "contentType";
    private static final Pattern formDataMatcher = Pattern.compile("^(?<key>[^;]*)=(?<isFile>@)?(?<value>[^;]*)"
            + "((;type=(?<contentType>[^;]*))|(;filename=[^;]*)){0,2}$");

    /**
     * Convert curl string to request.
     * @param curlString curl string
     * @return request
     */
    public HttpRequest convertCurlStringToRequest(HttpRequest httpRequest, String curlString) {
        // need to replace line separators in request body for valid request tokenize
        curlString = curlString.replaceAll(lineSeparatorPattern, Constants.TEMPORARY_LINE_SEPARATOR);
        List<String> tokens = tokenizeString(curlString);
        clearRequest(httpRequest);
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (token.startsWith(Constants.TEMPORARY_LINE_SEPARATOR)) {
                // token could start with <tls>, need to remove
                token = token.replaceAll(Constants.TEMPORARY_LINE_SEPARATOR, Constants.EMPTY_STRING);
            }
            if (token.startsWith("http://") || token.startsWith("https://")) {
                UrlParsingUtils.parseUrlAndRequestParams(httpRequest, token);
            }
            switch (optionsMap.getOrDefault(token, -1)) {
                case 1: // --data or -d or --data-raw
                    parseBody(httpRequest, i, tokens);
                    break;
                case 2: // --header or -H
                    parseHeaders(httpRequest, i, tokens);
                    break;
                case 3: // --request or -X
                    parseHttpMethod(httpRequest, i, tokens);
                    break;
                case 4: // -F or --form
                    parseFormData(httpRequest, i, tokens);
                    break;
                case 5: // --data-binary
                    log.warn("Import request with binary body not allowed");
                    break;
                default:
                    log.debug("Unknown curl parameter - {}", token);
            }
        }
        if (hasXmlHeader(httpRequest.getRequestHeaders())) {
            httpRequest.getBody().setType(RequestBodyType.XML);
        }

        setDefaultValuesIfNull(httpRequest);

        return httpRequest;
    }

    /**
     * Clear request url, parameters, headers, http method, body before parsing curl string.
     *
     * @param httpRequest httpRequest
     */
    private void clearRequest(HttpRequest httpRequest) {
        httpRequest.setUrl("");
        httpRequest.setRequestHeaders(new ArrayList<>());
        httpRequest.setHttpMethod(HttpMethod.GET);
        httpRequest.setBody(null);
        httpRequest.setRequestParams(null);
    }

    /**
     * Check if headers list have xml header.
     * @param headers list of headers
     * @return true/false
     */
    private boolean hasXmlHeader(List<RequestHeader> headers) {
        return headers.stream()
                .anyMatch(CurlUtils::isHeaderXmlContentType);
    }

    /**
     * Fill request by defaults if curl string doesn't have info.
     * @param httpRequest httpRequest
     */
    private void setDefaultValuesIfNull(HttpRequest httpRequest) {
        if (httpRequest.getHttpMethod() == null) {
            httpRequest.setHttpMethod(HttpMethod.GET);
        }

        httpRequest.setTransportType(TransportType.REST);
    }

    /**
     * Check if current option has value.
     * @param currentOptionNumber current option number
     * @param tokens list of string tokens
     * @return true/false
     */
    private boolean isNextTokenValue(int currentOptionNumber, List<String> tokens) {
        return currentOptionNumber + 1 < tokens.size() && !tokens.get(currentOptionNumber + 1).startsWith("-");
    }

    /**
     * Fill request body by getting value from tokenized string (i.e. list of tokens).
     * @param httpRequest httpRequest
     * @param currentTokenNumber current token number
     * @param tokens list of tokens
     */
    private void parseBody(HttpRequest httpRequest, int currentTokenNumber, List<String> tokens) {
        if (isNextTokenValue(currentTokenNumber, tokens)) {
            RequestBody body = new RequestBody();
            String content = tokens.get(currentTokenNumber + 1)
                    .replaceAll(Constants.TEMPORARY_LINE_SEPARATOR, System.lineSeparator());
            body.setContent(content);
            if (!body.detectAndFillGraphQlProperties(content)) {
                body.setType(RequestBodyType.JSON);
            }
            httpRequest.setBody(body);
        }
    }

    /**
     * Fill request body formData by getting value from tokenized string (i.e. list of tokens).
     * @param httpRequest httpRequest
     * @param currentTokenNumber current token number
     * @param tokens list of tokens
     */
    private void parseFormData(HttpRequest httpRequest, int currentTokenNumber, List<String> tokens) {
        if (isNextTokenValue(currentTokenNumber, tokens)) {
            String content = tokens.get(currentTokenNumber + 1)
                    .replaceAll(Constants.TEMPORARY_LINE_SEPARATOR, System.lineSeparator());
            log.debug("Parse form data part {}", content);
            Matcher matcher = formDataMatcher.matcher(content);

            if (!matcher.matches()) {
                log.error("Bad form data format - {}", content);
                throw new BadFormDataFormatException(content);
            }

            RequestBody body = httpRequest.getBody();
            if (body == null) {
                body = new RequestBody();
                body.setType(RequestBodyType.FORM_DATA);
            }

            List<FormDataPart> formDataParts = body.getFormDataBody();
            if (body.getFormDataBody() == null) {
                formDataParts = new ArrayList<>();
            }

            String key = matcher.group(KEY_GROUP_NAME);
            String value = matcher.group(VALUE_GROUP_NAME);
            String isFile = matcher.group(IS_FILE_GROUP_NAME);
            String contentType = matcher.group(CONTENT_TYPE_GROUP_NAME);

            if (StringUtils.isNotEmpty(isFile)) {
                log.warn("Importing file in form data not allowed");
                return;
            }
            formDataParts.add(new FormDataPart(key, ValueType.TEXT, value, null, contentType, "", false));

            body.setFormDataBody(formDataParts);
            httpRequest.setBody(body);
        }
    }

    /**
     * Fill request headers by getting values from tokenized string (i.e. list of tokens).
     * @param httpRequest httpRequest
     * @param currentTokenNumber current token number
     * @param tokens list of tokens
     */
    private void parseHeaders(HttpRequest httpRequest, int currentTokenNumber, List<String> tokens) {
        if (isNextTokenValue(currentTokenNumber, tokens)) {
            // division into 2 parts
            String[] header = tokens.get(currentTokenNumber + 1).split(":", 2);
            // remove spaces and semicolon
            String name = StringUtils.strip(header[0].trim(), ";");
            if (header.length == 1) { // "X-Custom-Header"
                RequestHeader requestHeader = new RequestHeader();
                requestHeader.setKey(name);
                requestHeader.setValue("");
                requestHeader.setDescription("");
                httpRequest.getRequestHeaders().add(requestHeader);
            } else { // "Key: Value"
                String value = header[1].trim();
                RequestHeader requestHeader = new RequestHeader();
                requestHeader.setKey(name);
                requestHeader.setValue(value);
                requestHeader.setDescription("");
                httpRequest.getRequestHeaders().add(requestHeader);
            }
        }
    }

    /**
     * Fill request http method by getting value from tokenized string (i.e. list of tokens).
     * @param httpRequest httpRequest
     * @param currentTokenNumber current token number
     * @param tokens list of tokens
     */
    private void parseHttpMethod(HttpRequest httpRequest, int currentTokenNumber, List<String> tokens) {
        if (isNextTokenValue(currentTokenNumber, tokens)) {
            httpRequest.setHttpMethod(HttpMethod.valueOf(tokens.get(currentTokenNumber + 1)));
        }
    }
}
