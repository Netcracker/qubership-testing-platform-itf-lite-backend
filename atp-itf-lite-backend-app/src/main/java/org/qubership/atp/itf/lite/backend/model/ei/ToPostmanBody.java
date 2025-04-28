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

package org.qubership.atp.itf.lite.backend.model.ei;

import static org.qubership.atp.itf.lite.backend.utils.Constants.COLLECTION;

import java.util.ArrayList;
import java.util.List;

import org.qubership.atp.itf.lite.backend.model.entities.RequestBody;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.utils.Constants;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
public class ToPostmanBody {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ToPostmanMode mode;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String raw;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ToPostmanGraphql graphql;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ToPostmanMapDescriptionAndType> urlencoded;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ToPostmanFormData> formdata;
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ToPostmanFile file;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private boolean disabled;

    /**
     * Create Postman body by request.
     */
    public ToPostmanBody(HttpRequest request) {
        RequestBody body = request.getBody();
        disabled = false;
        if (body.getType() != null) {
            this.mode = ToPostmanMode.from(body.getType());
            switch (this.mode) {
                case RAW:
                    this.raw = body.getContent();
                    break;
                case URLENCODED:
                    this.urlencoded = new ArrayList<>(); //TODO?
                    break;
                case FORMDATA:
                    this.formdata = new ArrayList<>();
                    body.getFormDataBody().forEach(formDataPart -> {
                        switch (formDataPart.getType()) {
                            case FILE:
                                this.formdata.add(ToPostmanFormData.file(
                                        formDataPart.getKey(),
                                        formDataPart.getDescription(),
                                        COLLECTION + "/" + Constants.FILES + "/" + request.getId() + "/"
                                                + formDataPart.getValue()));
                                break;
                            case TEXT:
                                this.formdata.add(ToPostmanFormData.text(
                                        formDataPart.getKey(),
                                        formDataPart.getValue(),
                                        formDataPart.getDescription()));
                                break;
                            default:
                        }
                    });
                    break;
                case FILE:
                    break;
                case GRAPHQL:
                    this.graphql = new ToPostmanGraphql(body.getQuery(), body.getVariables());
                    break;
                default:
            }
        }
    }
}
