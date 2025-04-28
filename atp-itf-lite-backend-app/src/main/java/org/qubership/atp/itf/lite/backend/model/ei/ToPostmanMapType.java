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

import lombok.Getter;

@Getter
public class ToPostmanMapType extends ToPostmanMap {

    private String type = "string";

    ToPostmanMapType(String key, String value) {
        super(key, value);
    }

    /**
     * Token.
     *
     * @param value value
     * @return {@link  ToPostmanMapType} instance
     */
    public static ToPostmanMapType token(String value) {
        return new ToPostmanMapType("token", value);
    }

    /**
     * UserName.
     *
     * @param value value
     * @return {@link  ToPostmanMapType} instance
     */
    public static ToPostmanMapType userName(String value) {
        return new ToPostmanMapType("username", value);
    }

    /**
     * Password.
     *
     * @param value password value
     * @return {@link  ToPostmanMapType} instance
     */
    public static ToPostmanMapType password(String value) {
        return new ToPostmanMapType("password", value);
    }

    /**
     * GrantType.
     *
     * @param value value
     * @return {@link  ToPostmanMapType} instance
     */
    public static ToPostmanMapType grantType(String value) {
        return new ToPostmanMapType("grant_type", value);
    }

    /**
     * AuthUrl.
     *
     * @param value value
     * @return {@link  ToPostmanMapType} instance
     */
    public static ToPostmanMapType authUrl(String value) {
        return new ToPostmanMapType("authUrl", value);
    }

    /**
     * AccessTokenUrl.
     *
     * @param value value
     * @return {@link  ToPostmanMapType} instance
     */
    public static ToPostmanMapType accessTokenUrl(String value) {
        return new ToPostmanMapType("accessTokenUrl", value);
    }

    /**
     * ClientId.
     *
     * @param value value
     * @return {@link  ToPostmanMapType} instance
     */
    public static ToPostmanMapType clientId(String value) {
        return new ToPostmanMapType("clientId", value);
    }

    /**
     * Scope.
     *
     * @param value value
     * @return {@link  ToPostmanMapType} instance
     */
    public static ToPostmanMapType scope(String value) {
        return new ToPostmanMapType("scope", value);
    }

    /**
     * State.
     *
     * @param value value
     * @return {@link  ToPostmanMapType} instance
     */
    public static ToPostmanMapType state(String value) {
        return new ToPostmanMapType("state", value);
    }

    /**
     * addTokenTo.
     *
     * @return {@link  ToPostmanMapType} instance
     */
    public static ToPostmanMapType addTokenToHeader() {
        return new ToPostmanMapType("addTokenTo", "header");
    }

    /**
     * headerPrefix.
     *
     * @param value value
     * @return {@link  ToPostmanMapType} instance
     */
    public static ToPostmanMapType headerPrefix(String value) {
        return new ToPostmanMapType("headerPrefix", value);
    }
}
