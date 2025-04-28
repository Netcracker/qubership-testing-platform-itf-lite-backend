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
import org.qubership.atp.itf.lite.backend.service.CollectionRunService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CollectionRunsCleaner {

    private static final String UTC_TIMEZONE = "UTC";

    @Value("${atp.itf.lite.collection.runs.remove.days}")
    private int collectionRunsRemoveDays;

    private final LockManager lockManager;
    private final CollectionRunService collectionRunService;

    public CollectionRunsCleaner(LockManager lockManager, CollectionRunService collectionRunService) {
        this.lockManager = lockManager;
        this.collectionRunService = collectionRunService;
    }

    /**
     * Scheduled task to clean up collection run tables.
     */
    @Scheduled(cron = "${atp.itf.lite.collection.runs.cron.expression}", zone = UTC_TIMEZONE)
    public void collectionRunsCleanupWithLockManager() {
        lockManager.executeWithLock("cleanUpCollectionRuns", this::cleanUpCollectionRuns);
    }

    /**
     * Perform collection runs clean up.
     */
    public void cleanUpCollectionRuns() {
        log.info("Start collection runs clean up");
        collectionRunService.cleanUpRequestExecutionHistory(collectionRunsRemoveDays);
        log.info("Finish collection runs clean up");
    }
}
