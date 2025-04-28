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

package org.qubership.atp.itf.lite.backend.enums;

import java.util.concurrent.TimeUnit;

import lombok.Getter;

public enum CacheKeys {
    ENVIRONMENT_SYSTEMS(Constants.ENVIRONMENT_SYSTEMS, 20, TimeUnit.MINUTES, CacheGroups.SYSTEMS),
    PROJECT_CERT(Constants.PROJECT_CERT, 20, TimeUnit.MINUTES, CacheGroups.PROJECTS),
    AUTH_PROJECTS_KEY(Constants.AUTH_PROJECTS_KEY, 2, TimeUnit.MINUTES, CacheGroups.PROJECTS),
    AUTH_OBJECTS_KEY(Constants.AUTH_OBJECTS_KEY, 2, TimeUnit.MINUTES, CacheGroups.PROJECTS);

    @Getter
    private final String key;
    @Getter
    private final int timeToLive;
    @Getter
    private final TimeUnit timeUnit;
    @Getter
    private final CacheGroups cacheGroup;

    CacheKeys(String key, int timeToLive, TimeUnit timeUnit, CacheGroups cacheGroup) {
        this.key = key;
        this.timeToLive = timeToLive;
        this.timeUnit = timeUnit;
        this.cacheGroup = cacheGroup;
    }

    /**
     * Get Time To Leave value in seconds.
     *
     * @return Time To Leave value in seconds
     */
    public int getTtlInSeconds() {
        switch (timeUnit) {
            case SECONDS:
                return timeToLive;
            case MINUTES:
                return 60 * timeToLive;
            case HOURS:
                return 60 * 60 * timeToLive;
            case DAYS:
                return 24 * 60 * 60 * timeToLive;
            default:
                return 0;
        }
    }

    public enum CacheGroups {
        PROJECTS, SYSTEMS
    }

    public static class Constants {
        public static final String ENVIRONMENT_SYSTEMS = "ATP_ITF_LITE_ENVIRONMENT_SYSTEMS";
        public static final String AUTH_PROJECTS_KEY = "projects";
        public static final String PROJECT_CERT = "ATP_ITF_LITE_PROJECT_CERT";
        public static final String AUTH_OBJECTS_KEY = "auth_objects";
    }
}
