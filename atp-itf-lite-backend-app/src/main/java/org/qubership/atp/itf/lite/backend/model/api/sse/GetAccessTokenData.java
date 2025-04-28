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

package org.qubership.atp.itf.lite.backend.model.api.sse;

import org.qubership.atp.itf.lite.backend.model.api.response.ErrorResponseSerializable;
import org.qubership.atp.itf.lite.backend.model.api.response.auth.OAuth2AuthrizationResponse;
import org.qubership.atp.itf.lite.backend.utils.RequestUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GetAccessTokenData {
    @JsonProperty("data")
    private OAuth2AuthrizationResponse data;

    @JsonProperty("error")
    private ErrorResponseSerializable error;

    public GetAccessTokenData(OAuth2AuthrizationResponse data) {
        this.data = data;
    }

    /**
     * Constructor.
     *
     * @param exception Exception
     */
    public GetAccessTokenData(Exception exception) {
        error = RequestUtils.getErrorResponse(exception);
    }
}
