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

import org.qubership.atp.common.lock.LockManager;
import org.qubership.atp.itf.lite.backend.service.RequestExecutionHistoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RequestExecutionHistoryCleanup {

    @Value("${history.retention.in.days}")
    private int historyRetentionInDays;

    private final LockManager lockManager;
    private final RequestExecutionHistoryService requestExecutionHistoryService;

    public RequestExecutionHistoryCleanup(LockManager lockManager,
                                          RequestExecutionHistoryService requestExecutionHistoryService) {
        this.lockManager = lockManager;
        this.requestExecutionHistoryService = requestExecutionHistoryService;
    }

    /**
     * Scheduled task to clean up request execution history.
     */
    @Scheduled(cron = "${history.retention.cron.expression}")
    public void cleanUpRequestExecutionHistoryWithLockManager() {
        lockManager.executeWithLock("cleanUpRequestExecutionHistory", this::cleanUpRequestExecutionHistory);
    }

    /**
     * Perform clean up execution history.
     */
    public void cleanUpRequestExecutionHistory() {
        log.info("Start cleanUp of request execution history");
        log.info("CleanUp of request execution history {} records got deleted.",
                requestExecutionHistoryService.cleanUpRequestExecutionHistory(historyRetentionInDays));
        log.info("Finish cleanUp of request execution history");
    }
}
