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

package org.qubership.atp.itf.lite.backend.service.context;

import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.qubership.atp.itf.lite.backend.service.UserService;
import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExecutorContextEnricher {

    private static final String UNKNOWN_USER = "Unknown User";

    private final Provider<UserInfo> userInfoProvider;
    private final UserService userService;

    /**
     * Enriches target context with executor identity and executed request name.
     * Does not overwrite keys if they are already present.
     * Keys added:
     * - {@link Constants#EXECUTOR_NAME_ITF_LITE}: full name (fallback: username, then "Unknown User")
     * - {@link Constants#EXECUTION_REQUEST_NAME_ITF_LITE}: executed request name (if provided)
     */
    public void enrich(Map<String, Object> targetContext,
                       @Nullable String bearerToken,
                       @Nullable String executedRequestName) {
        doEnrich(targetContext, bearerToken, executedRequestName);
    }

    private void doEnrich(Map<String, Object> targetContext,
                          @Nullable String bearerToken,
                          @Nullable String executedRequestName) {
        UserInfo userInfo = safeGetFromProvider();
        if (userInfo == null && StringUtils.isNotBlank(bearerToken)) {
            userInfo = userService.getUserInfoByToken(bearerToken);
        }

        String username = resolveUsername(userInfo);

        if (targetContext == null) {
            return;
        }
        targetContext.putIfAbsent(Constants.EXECUTOR_NAME_ITF_LITE, username);
        if (StringUtils.isNotBlank(executedRequestName)) {
            targetContext.putIfAbsent(Constants.EXECUTION_REQUEST_NAME_ITF_LITE, executedRequestName);
        }
    }

    @Nullable
    private UserInfo safeGetFromProvider() {
        try {
            return userInfoProvider.get();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveUsername(@Nullable UserInfo userInfo) {
        if (userInfo == null) {
            return UNKNOWN_USER;
        }
        String username = userInfo.getUsername();
        return StringUtils.isNotBlank(username) ? username : UNKNOWN_USER;
    }
}

