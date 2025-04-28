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

package org.qubership.atp.itf.lite.backend.service.history.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.javers.core.Javers;
import org.javers.shadow.Shadow;
import org.modelmapper.ModelMapper;
import org.qubership.atp.itf.lite.backend.enums.http.RequestBodyType;
import org.qubership.atp.itf.lite.backend.feign.dto.history.HistoryItemTypeDto;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.model.entities.RequestBody;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.service.RequestService;
import org.springframework.stereotype.Service;

@Service
public class HttpRequestRestoreHistoryService extends AbstractRestoreHistoryService<Request> {

    public HttpRequestRestoreHistoryService(Javers javers, RequestService configurationService,
                                            ValidateReferenceExistsService validateReferenceExistsService,
                                            ModelMapper modelMapper) {
        super(javers, configurationService, validateReferenceExistsService, modelMapper);
    }

    @Override
    public HistoryItemTypeDto getItemType() {
        return HistoryItemTypeDto.REQUEST;
    }

    @Override
    public Class<HttpRequest> getEntityClass() {
        return HttpRequest.class;
    }

    @Override
    void updateObjectWithChild(Shadow<Request> object) {
        //no child here
    }

    @Override
    public List<Shadow<Object>> getChildShadows(Shadow<Request> parentShadow, Class targetObject) {
        //no child here
        return new ArrayList<>();
    }

    @Override
    protected void copyValues(Request shadow, Request actualObject) {
        modelMapper.map(shadow, actualObject);
        RequestBody body = ((HttpRequest) shadow).getBody();
        if (Objects.nonNull(body) && RequestBodyType.FORM_DATA.equals(body.getType())) {
            ((HttpRequest) actualObject).getBody()
                    .setFormDataBody(new ArrayList<>(body.getFormDataBody()));
        }
    }
}
