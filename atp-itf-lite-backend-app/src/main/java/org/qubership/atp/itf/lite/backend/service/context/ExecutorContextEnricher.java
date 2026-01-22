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
    private static final String UNKNOWN_USERNAME = "Unknown";

    private final Provider<UserInfo> userInfoProvider;
    private final UserService userService;

    /**
     * Enriches target context with executor identity.
     * Does not overwrite keys if they are already present.
     * Keys added:
     * - {@link Constants#EXECUTOR_FIRST_LAST_NAME}: full name (fallback: username, then "Unknown User")
     * - {@link Constants#USERNAME}: login/username (fallback: "Unknown")
     */
    public void enrich(Map<String, Object> targetContext, @Nullable String bearerToken) {
        doEnrich(targetContext, bearerToken, null);
    }

    /**
     * Enriches target context with executor identity and executed request name.
     * Does not overwrite keys if they are already present.
     * Keys added:
     * - {@link Constants#EXECUTOR_FIRST_LAST_NAME}: full name (fallback: username, then "Unknown User")
     * - {@link Constants#USERNAME}: login/username (fallback: "Unknown")
     * - {@link Constants#EXECUTION_REQUEST_NAME}: executed request name (if provided)
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
        String executorName = resolveExecutorName(userInfo, username);

        if (targetContext == null) {
            return;
        }
        targetContext.putIfAbsent(Constants.USERNAME, username);
        targetContext.putIfAbsent(Constants.EXECUTOR_FIRST_LAST_NAME, executorName);
        if (StringUtils.isNotBlank(executedRequestName)) {
            targetContext.putIfAbsent(Constants.EXECUTION_REQUEST_NAME, executedRequestName);
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
            return UNKNOWN_USERNAME;
        }
        String username = userInfo.getUsername();
        return StringUtils.isNotBlank(username) ? username : UNKNOWN_USERNAME;
    }

    private String resolveExecutorName(@Nullable UserInfo userInfo, String resolvedUsername) {
        if (userInfo == null) {
            return UNKNOWN_USER;
        }

        String fullName = null;
        try {
            fullName = userInfo.getFullName();
        } catch (Exception ignored) {
            // some UserInfo implementations may not have/get fullName
        }
        if (StringUtils.isNotBlank(fullName)) {
            return fullName;
        }

        String firstName = null;
        String lastName = null;
        try {
            firstName = userInfo.getFirstName();
            lastName = userInfo.getLastName();
        } catch (Exception ignored) {
            // ignore
        }
        String joined = (StringUtils.defaultString(firstName) + " " + StringUtils.defaultString(lastName)).trim();
        if (StringUtils.isNotBlank(joined)) {
            return joined;
        }

        if (StringUtils.isNotBlank(resolvedUsername) && !UNKNOWN_USERNAME.equals(resolvedUsername)) {
            return resolvedUsername;
        }
        return UNKNOWN_USER;
    }
}

