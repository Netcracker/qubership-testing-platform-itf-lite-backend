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

package org.qubership.atp.itf.lite.backend.model.api.request;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.logging.log4j.util.Strings;
import org.qubership.atp.itf.lite.backend.feign.dto.HeaderDto;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanPostmanRequestDto;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanUrlDto;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpHeaderSaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpParamSaveRequest;
import org.qubership.atp.itf.lite.backend.model.entities.Cookie;
import org.qubership.atp.itf.lite.backend.model.entities.RequestBody;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileData;
import org.qubership.atp.itf.lite.backend.model.entities.http.methods.HttpMethod;
import org.qubership.atp.itf.lite.backend.utils.RequestUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HttpRequestEntitySaveRequest extends RequestEntitySaveRequest {

    @NotNull
    private HttpMethod httpMethod;

    @NotEmpty
    private String url;

    private List<HttpParamSaveRequest> requestParams;

    private List<HttpHeaderSaveRequest> requestHeaders;

    private RequestBody body;
    private FileData file;
    private List<Cookie> cookies;

    @Override
    public void resolveTemplates(Function<String, String> evaluateFunction) {
        url = evaluateFunction.apply(url);
        if (Objects.nonNull(requestParams)) {
            requestParams.forEach(p -> {
                p.setKey(evaluateFunction.apply(p.getKey()));
                p.setValue(evaluateFunction.apply(p.getValue()));
            });
        }
        if (Objects.nonNull(requestHeaders)) {
            requestHeaders.forEach(h -> {
                h.setKey(evaluateFunction.apply(h.getKey()));
                h.setValue(evaluateFunction.apply(h.getValue()));
            });
        }
        if (!Objects.isNull(body) && !StringUtils.isEmpty(body.getContent())) {
            body.setContent(evaluateFunction.apply(body.getContent()));
        }
    }

    /**
     * Returns string represents not encoded url with query parameters.
     * <br/>
     * Url is not encoded as it could be a potentially invalid url.
     * <br/>
     * If you are sure that the url is valid, it is better to use
     * {@link RequestUtils#buildRequestWithParameters} instead of this one
     */
    public String getUrlWithQueryParameters() {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
        if (!CollectionUtils.isEmpty(requestParams)) {
            requestParams.forEach(parameter -> {
                if (!parameter.isDisabled()) {
                    uriComponentsBuilder.queryParam(parameter.getKey(), parameter.getValue());
                }
            });
        }
        return url + uriComponentsBuilder.build().toUri();
    }

    @Override
    public void normalize() {
        this.url = StringUtils.isEmpty(this.url) ? this.url : this.url.trim();
    }

    /**
     * Collect PostmanPostmanRequestDto model.
     */
    @JsonIgnore
    public PostmanPostmanRequestDto getPostmanRequest() {
        PostmanPostmanRequestDto pmRequest = new PostmanPostmanRequestDto();
        pmRequest.setId(this.getId());
        pmRequest.setName(this.getName());
        pmRequest.setMethod(this.getHttpMethod().toString());
        if (Objects.nonNull(this.getRequestParams())) {
            pmRequest.setUrl(generatePostmanUrlDto(this.getUrl(), this.getRequestParams()));
        } else {
            pmRequest.setUrl(generatePostmanUrlDto(this.getUrl(), null));
        }
        if (Objects.nonNull(this.getRequestHeaders())) {
            pmRequest.setHeader(this.getRequestHeaders()
                    .stream()
                    .filter(header -> !header.isDisabled())
                    .map(h -> new HeaderDto().key(h.getKey()).value(h.getValue()))
                    .collect(Collectors.toList()));
        }
        if (Objects.nonNull(this.getBody())) {
            pmRequest.setBody(this.getBody().getPostmanBody(this.getFile()));
        }
        return pmRequest;
    }

    /**
     * Update fields by PostmanPostmanRequestDto model.
     */
    public void updateFromPostmanRequest(PostmanPostmanRequestDto postmanRequest) {
        this.setHttpMethod(HttpMethod.resolve(postmanRequest.getMethod()));
        this.setUrl(urlWithoutQuery(postmanRequest.getUrl()));
        if (Objects.nonNull(postmanRequest.getUrl().getQuery())) {
            this.setRequestParams(postmanRequest.getUrl().getQuery()
                    .stream()
                    .map(q -> new HttpParamSaveRequest(q.getKey(), (String) q.getValue(), ""))
                    .collect(Collectors.toList()));
        }
        if (Objects.nonNull(postmanRequest.getHeader())) {
            this.setRequestHeaders(postmanRequest.getHeader()
                    .stream()
                    .map(h -> new HttpHeaderSaveRequest(h.getKey(), (String) h.getValue(), ""))
                    .collect(Collectors.toList()));
        }
        if (Objects.nonNull(postmanRequest.getBody())) {
            if (Objects.isNull(this.getBody())) {
                this.setBody(new RequestBody());
            }
            this.getBody().updateFromPostmanBody(postmanRequest.getBody());
        }
    }

    private PostmanUrlDto generatePostmanUrlDto(String url, List<HttpParamSaveRequest> queryParams) {
        PostmanUrlDto postmanUrlDto = new PostmanUrlDto();
        try {
            URL tmpUrl = new URL(url);
            postmanUrlDto
                    .protocol(tmpUrl.getProtocol())
                    .host(Collections.singletonList(tmpUrl.getHost()))
                    .port(tmpUrl.getPort() != -1 ? String.valueOf(tmpUrl.getPort()) : null)
                    .path(Collections.singletonList(tmpUrl.getPath()))
                    .query(parseQuery(tmpUrl.getQuery()));
        } catch (MalformedURLException ignore) {
            postmanUrlDto
                    .host(Collections.singletonList(url))
                    .query(new ArrayList<>());
        }
        if (queryParams != null) {
            List<HeaderDto> queryParameters = queryParams.stream()
                    .filter(q -> !q.isDisabled())
                    .map(q -> new HeaderDto().key(q.getKey()).value(q.getValue()))
                    .collect(Collectors.toList());
            if (postmanUrlDto.getQuery() == null) {
                postmanUrlDto.setQuery(queryParameters);
            } else {
                postmanUrlDto.getQuery().addAll(queryParameters);
            }
        }
        return postmanUrlDto;
    }

    private List<HeaderDto> parseQuery(String query) {
        List<HeaderDto> queryParameters = new ArrayList<>();
        if (Strings.isEmpty(query)) {
            return queryParameters;
        }
        String[] queryParams = query.split("&");
        for (String queryParam : queryParams) {
            String[] kv = queryParam.split("=");
            String key = kv[0];
            if (kv.length == 2) {
                queryParameters.add(new HeaderDto().key(key).value(kv[1]));
            } else {
                queryParameters.add(new HeaderDto().key(key).value(null));
            }
        }
        return queryParameters;
    }

    /**
     * Collect url without query.
     */
    private String urlWithoutQuery(PostmanUrlDto postmanUrlDto) {
        StringBuilder sb = new StringBuilder();
        if (Objects.nonNull(postmanUrlDto.getProtocol())) {
            sb.append(postmanUrlDto.getProtocol()).append("://");
        }
        sb.append(String.join(".", postmanUrlDto.getHost()));
        if (Objects.nonNull(postmanUrlDto.getPort())) {
            sb.append(":").append(postmanUrlDto.getPort());
        }
        sb.append(getPostmanUrlPath(postmanUrlDto));
        return sb.toString();
    }

    /**
     * Convert to string path by PostmanUrlDto.
     */
    public String getPostmanUrlPath(PostmanUrlDto postmanUrlDto) {
        if (Objects.isNull(postmanUrlDto.getPath())) {
            return "";
        }
        return String.join("/", postmanUrlDto.getPath());
    }
}
