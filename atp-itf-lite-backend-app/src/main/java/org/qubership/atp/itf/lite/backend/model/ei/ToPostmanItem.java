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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Strings;
import lombok.Getter;

@Getter
public class ToPostmanItem {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UUID id;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String name;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String description;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ToPostmanEvent> event;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ToPostmanRequest request;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<String> response;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ToPostmanItem> item;

    /**
     * Create Postman entity by request.
     */
    public ToPostmanItem(HttpRequest request) {
        this.id = request.getId();
        this.name = request.getName();
        this.description = request.getName();
        if (!Strings.isNullOrEmpty(request.getPreScripts())) {
            if (this.event == null) {
                this.event = new ArrayList<>();
            }
            this.event.add(ToPostmanEvent.prerequest(request.getPreScripts()));
        }
        if (!Strings.isNullOrEmpty(request.getPostScripts())) {
            if (this.event == null) {
                this.event = new ArrayList<>();
            }
            this.event.add(ToPostmanEvent.test(request.getPostScripts()));
        }
        this.request = new ToPostmanRequest(request);
        this.response = new ArrayList<>();
    }

    public ToPostmanItem(Folder folder) {
        this.id = folder.getId();
        this.name = folder.getName();
    }

    /**
     * Check null and get items.
     */
    @JsonIgnore
    public List<ToPostmanItem> getItems() {
        if (item == null) {
            item = new ArrayList<>();
        }
        return item;
    }
}
