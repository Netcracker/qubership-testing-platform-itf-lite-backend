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

import org.qubership.atp.itf.lite.backend.service.history.iface.DeleteHistoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class HistoryOldRevisionsCleaner {

    private static final String UTC_TIMEZONE = "UTC";

    private final DeleteHistoryService deleteHistoryService;

    @Value("${atp.itf.lite.history.clean.job.revision.max.count}")
    private long maxRevisionCount;

    @Value("${atp.itf.lite.history.clean.job.page-size}")
    private Integer pageSize;


    @Scheduled(cron = "${atp.itf.lite.history.clean.job.expression}", zone = UTC_TIMEZONE)
    public void run() {
        deleteHistoryService.deleteTerminatedSnapshots(pageSize);
        deleteHistoryService.deleteOldSnapshots(maxRevisionCount);
    }
}
