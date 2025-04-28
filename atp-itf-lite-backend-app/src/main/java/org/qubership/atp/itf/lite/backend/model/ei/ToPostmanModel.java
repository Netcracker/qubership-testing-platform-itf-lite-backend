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

import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
public class ToPostmanModel {

    private ToPostmanInfo info;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<ToPostmanItem> item;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ToPostmanAuth auth;

    /**
     * Constructor.
     * @param folder folderName
     */
    public ToPostmanModel(String folder) {
        this.info = new ToPostmanInfo(folder);
    }

    /**
     * Constructor.
     * @param folder folder
     */
    public ToPostmanModel(Folder folder) {
        this.info = new ToPostmanInfo(folder.getName());
        RequestAuthorization authorization = folder.getAuthorization();
        this.auth = new ToPostmanAuth(authorization);
    }

    /**
     * Check not null and return item.
     */
    @JsonIgnore
    public List<ToPostmanItem> getItems() {
        if (item == null) {
            item = new ArrayList<>();
        }
        return item;
    }
}
