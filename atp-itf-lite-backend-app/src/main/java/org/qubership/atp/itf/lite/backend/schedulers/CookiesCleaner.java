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

package org.qubership.atp.itf.lite.backend.schedulers;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.qubership.atp.common.lock.LockManager;
import org.qubership.atp.itf.lite.backend.model.entities.Cookie;
import org.qubership.atp.itf.lite.backend.service.CookieService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CookiesCleaner {

    private final CookieService cookieService;
    private final LockManager lockManager;

    @Scheduled(cron = "${atp.itf.lite.clean.cookie.cron.expression}")
    public void cookiesCleanUpJob() {
        lockManager.executeWithLock("cleanupCookies", this::cleanupCookies);
    }

    private void cleanupCookies() {
        log.info("Start cookies cleanup");
        cookieService.deleteIfErIdOrTrIdSpecified();
        List<Cookie> cookies = cookieService.getAll();
        List<UUID> cookiesToDelete = new ArrayList<>();
        cookies.forEach(cookie -> {
            List<HttpCookie> parsedCookies = HttpCookie.parse(cookie.getValue());
            for (HttpCookie parsedCookie : parsedCookies) {
                if (parsedCookie.hasExpired()) {
                    cookiesToDelete.add(cookie.getId());
                    break;
                }
            }
        });
        cookieService.deleteAllByIdIn(cookiesToDelete);
        log.info("Finish cookies cleanup");
    }
}
