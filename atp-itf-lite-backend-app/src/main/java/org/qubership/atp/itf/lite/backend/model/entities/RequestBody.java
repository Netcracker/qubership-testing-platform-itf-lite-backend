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

package org.qubership.atp.itf.lite.backend.model.entities;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.javers.core.metamodel.annotation.DiffInclude;
import org.javers.core.metamodel.annotation.ValueObject;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.qubership.atp.itf.lite.backend.annotations.SerializableCheckable;
import org.qubership.atp.itf.lite.backend.enums.ValueType;
import org.qubership.atp.itf.lite.backend.enums.http.RequestBodyType;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanBodyDto;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanFormDataPartDescriptionDto;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanFormDataPartDto;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanFormDataPartTypeDto;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanGraphQlBodyDto;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanRequestBodyModeDto;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileData;
import org.qubership.atp.itf.lite.backend.model.entities.http.FormDataPart;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Embeddable
@Data
@ValueObject
@NoArgsConstructor
@SerializableCheckable
@Slf4j
public class RequestBody implements Serializable {

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper() {
            {
                this.registerModule(new JavaTimeModule());
                this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                this.configure(SerializationFeature.INDENT_OUTPUT, false);
                this.findAndRegisterModules();
            }
        };
    }

    @Column(name = "content", columnDefinition = "TEXT")
    @DiffInclude
    private String content;

    @Column(name = "query", columnDefinition = "TEXT")
    @DiffInclude
    private String query;

    @Column(name = "variables", columnDefinition = "TEXT")
    @DiffInclude
    private String variables;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    @DiffInclude
    private RequestBodyType type;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "request_id")
    @DiffInclude
    private List<FormDataPart> formDataBody;

    @Embedded
    @DiffInclude
    private FileBody binaryBody;

    public RequestBody(String content, RequestBodyType type) {
        this.content = content;
        this.type = type;
    }

    /**
     * Constructor RequestBody.
     */
    public RequestBody(String query, String variables, RequestBodyType type) {
        this.query = query;
        this.variables = variables;
        this.type = type;
    }

    public RequestBody(List<FormDataPart> formDataBody, RequestBodyType type) {
        this.formDataBody = formDataBody;
        this.type = type;
    }

    public RequestBody(FileBody fileBody, RequestBodyType type) {
        this.binaryBody = fileBody;
        this.type = type;
    }

    /**
     * Collect Postman body and set file.
     */
    @JsonIgnore
    public PostmanBodyDto getPostmanBody(FileData file) {
        PostmanBodyDto body = new PostmanBodyDto();
        if (RequestBodyType.FORM_DATA.equals(this.type)) {
            body.setMode(PostmanRequestBodyModeDto.FORMDATA);
            List<PostmanFormDataPartDto> postmanFdp = new ArrayList<>();
            if (!CollectionUtils.isEmpty(this.formDataBody)) {
                postmanFdp = this.formDataBody
                        .stream()
                        .map(fdp -> {
                            PostmanFormDataPartDto pfdp = new PostmanFormDataPartDto();
                            pfdp.setKey(fdp.getKey());
                            if (ValueType.TEXT.equals(fdp.getType())) {
                                pfdp.setType(PostmanFormDataPartTypeDto.TEXT);
                                pfdp.setValue(fdp.getValue());
                            } else {
                                pfdp.setType(PostmanFormDataPartTypeDto.FILE);
                                pfdp.setSrc(Objects.isNull(fdp.getFileId()) ? null : fdp.getFileId().toString());
                                pfdp.setFileName(fdp.getValue());
                            }
                            pfdp.setContentType(fdp.getContentType());
                            pfdp.setDescription(new PostmanFormDataPartDescriptionDto()
                                    .content(fdp.getDescription())
                                    .type(ContentType.TEXT_PLAIN.getMimeType()));
                            pfdp.setDisabled(fdp.isDisabled());
                            return pfdp;
                        })
                        .collect(Collectors.toList());
            }
            body.setMode(PostmanRequestBodyModeDto.FORMDATA);
            body.setFormdata(postmanFdp);
        } else if (RequestBodyType.GraphQL.equals(getType())) {
            body.setMode(PostmanRequestBodyModeDto.GRAPHQL);
            body.setGraphql(new PostmanGraphQlBodyDto()
                    .query(getQuery())
                    .variables(getVariables()));
        } else if (RequestBodyType.Binary.equals(getType()) && file != null) {
            body.setMode(PostmanRequestBodyModeDto.FILE);
            body.setFile(file.getFileName());
        } else {
            body.setMode(PostmanRequestBodyModeDto.RAW);
            body.setRaw(this.content);
        }
        return body;
    }

    /**
     * Update fields by type mode postman body.
     */
    public void updateFromPostmanBody(PostmanBodyDto postmanBody) {
        if (Objects.nonNull(postmanBody.getMode())) {
            switch (postmanBody.getMode()) {
                case GRAPHQL:
                    this.type = RequestBodyType.GraphQL;
                    this.setQuery(postmanBody.getGraphql().getQuery());
                    this.setVariables(postmanBody.getGraphql().getVariables());
                    break;
                case FILE:
                    this.type = RequestBodyType.Binary;
                    break;
                case FORMDATA:
                    this.type = RequestBodyType.FORM_DATA;
                    if (!CollectionUtils.isEmpty(postmanBody.getFormdata())) {
                        this.formDataBody = postmanBody.getFormdata()
                                .stream()
                                .map(pfdp -> {
                                    FormDataPart fdp = new FormDataPart();
                                    fdp.setKey(pfdp.getKey());
                                    if (PostmanFormDataPartTypeDto.TEXT.equals(pfdp.getType())) {
                                        fdp.setType(ValueType.TEXT);
                                        fdp.setValue(pfdp.getValue());
                                    } else {
                                        fdp.setType(ValueType.FILE);
                                        fdp.setFileId(StringUtils.isEmpty(pfdp.getSrc())
                                                ? null : UUID.fromString(pfdp.getSrc()));
                                        fdp.setValue(pfdp.getFileName());
                                    }
                                    fdp.setContentType(pfdp.getContentType());
                                    fdp.setDescription(pfdp.getDescription().getContent());
                                    fdp.setDisabled(pfdp.getDisabled());
                                    return fdp;
                                })
                                .collect(Collectors.toList());
                    }
                    break;
                case RAW:
                    this.type = RequestBodyType.JSON;
                    this.content = postmanBody.getRaw();
                    break;
                default:
            }
        } else {
            this.content = postmanBody.getRaw();
        }
    }

    /**
     * Detect GraphQl value in content.
     */
    public boolean detectAndFillGraphQlProperties(String content) {
        try {
            Map<String, String> contentParams = objectMapper.readValue(
                    content.replace("\r\n", "\\r\\n").replace("\n", "\\n"),
                    Map.class);
            if (contentParams.isEmpty() || !contentParams.containsKey("query")) {
                return false; // Json format, but map is empty or doesn't have 'query' property.
            } else {
                // TODO: Check how Postman imports curl if there are 'query', optional 'variables' and some other props.
                this.query = contentParams.get("query");
                this.variables = objectMapper.writeValueAsString(contentParams.getOrDefault("variables", ""));
                this.content = objectMapper.writeValueAsString(contentParams);
                this.type = RequestBodyType.GraphQL;
                return true;
            }
        } catch (final IOException e) {
            return false; // Not a Json format, and, of course, not a GraphQL.
        }
    }

    /**
     * Equals type GraphQL and compute body and return content.
     */
    public String computeAndGetContent() {
        return RequestBodyType.GraphQL.equals(type) ? composeGraphQlBody(query, variables)
                : StringUtils.defaultString(content);
    }

    /**
     * Compute and set content.
     */
    public void computeAndSetContent() {
        if (RequestBodyType.GraphQL.equals(this.type)) {
            this.content = composeGraphQlBody(this.query, this.variables);
        }
    }

    /**
     * Compose GraphQl body.
     */
    public String composeGraphQlBody(String query, String variables) {
        JSONObject jsonObject = new JSONObject();
        String escapedQuery = "";
        if (!StringUtils.isEmpty(query)) {
            escapedQuery = StringUtils.replaceEach(JSONObject.escape(query),
                    Arrays.asList("\\n", "\\r", "\\t", "\\\"").toArray(new String[0]),
                    Arrays.asList(" ", " ", " ", "\"").toArray(new String[0]));
        }
        jsonObject.put("query",  escapedQuery);
        JSONParser parser = new JSONParser();
        try {
            jsonObject.put("variables", !StringUtils.isEmpty(variables) ? parser.parse(variables) : new JSONObject());
        } catch (ParseException e) {
            log.warn("Can't parse variables");
            jsonObject.put("variables", new JSONObject());
        }
        return jsonObject.toString();
    }

    public String composeGraphQlBody() {
        return composeGraphQlBody(this.query, this.variables);
    }
}
