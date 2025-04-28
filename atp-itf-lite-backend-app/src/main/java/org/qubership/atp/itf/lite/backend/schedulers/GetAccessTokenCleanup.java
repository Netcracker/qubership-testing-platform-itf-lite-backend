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

import java.sql.Timestamp;
import java.util.Calendar;

import org.qubership.atp.common.lock.LockManager;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.GetAuthorizationCodeRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class GetAccessTokenCleanup {

    private final int retentionInDays = -1;
    private final LockManager lockManager;
    private final GetAuthorizationCodeRepository getAuthorizationCodeRepository;

    /**
     * Clean up access token with scheduler.
     */
    @Scheduled(cron = "${getaccesstoken.retention.cron.expression}")
    public void getAccessTokenCleanup() {
        lockManager.executeWithLock("getAccessTokenCleanup", () -> {
            log.info("Start cleanUp of 'Get new access token' records");
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, retentionInDays);
            log.info("getAccessTokenCleanup procedure delete {} records",
                    getAuthorizationCodeRepository.deleteByStartedAtBefore(new Timestamp(calendar.getTimeInMillis())));
            log.info("Finish cleanUp of 'Get new access token' records");
        });
    }
}
