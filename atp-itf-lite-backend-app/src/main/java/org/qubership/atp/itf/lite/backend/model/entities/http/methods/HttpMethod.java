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

package org.qubership.atp.itf.lite.backend.model.entities.http.methods;

import org.apache.commons.lang3.EnumUtils;
import org.apache.http.client.methods.HttpRequestBase;
import org.springframework.lang.Nullable;

public enum HttpMethod {
    GET(HttpGet.class),
    HEAD(HttpHead.class),
    POST(HttpPost.class),
    PUT(HttpPut.class),
    PATCH(HttpPatch.class),
    DELETE(HttpDelete.class),
    OPTIONS(HttpOptions.class),
    TRACE(HttpTrace.class),
    COPY(HttpCopy.class),
    LOCK(HttpLock.class),
    UNLOCK(HttpUnlock.class),
    PROPFIND(HttpPropfind.class),
    PURGE(HttpPurge.class),
    LINK(HttpLink.class),
    UNLINK(HttpUnlink.class),
    VIEW(HttpView.class);

    private final Class<? extends HttpRequestBase> methodClass;

    HttpMethod(Class<? extends HttpRequestBase> httpMethodClass) {
        this.methodClass = httpMethodClass;
    }

    public HttpRequestBase getHttpRequest(String url) throws Exception {
        return this.methodClass.getConstructor(String.class).newInstance(url);
    }

    @Nullable
    public static HttpMethod resolve(@Nullable String method) {
        return EnumUtils.getEnum(HttpMethod.class, method, null);
    }

    public String toString() {
        return this.name();
    }
}
