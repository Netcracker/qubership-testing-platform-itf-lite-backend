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

package org.qubership.atp.itf.lite.backend.service;

import java.util.Date;
import java.util.List;

import org.qubership.atp.itf.lite.backend.model.entities.key.RequestSnapshotKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestSnapshotCleaner {

    @Value("${atp.itf.lite.clean.snapshot.expiration.period.seconds:86400}")
    private String snapshotExpirationPeriod;

    private final RequestSnapshotService requestSnapshotService;

    /**
     * TODO.
     * */
    @Scheduled(cron = "${atp.itf.lite.clean.snapshot.cron.expression}")
    public void deleteOldSnapshots() {
        List<RequestSnapshotKey> expiredSnapshotIds =
                requestSnapshotService.getByCreatedWhenDifferenceGreaterThanReferenceDate(new Date(),
                        Long.parseLong(snapshotExpirationPeriod));
        expiredSnapshotIds.forEach(key -> requestSnapshotService.deleteSnapshotByRequestSnapshotKey(
                key.getSessionId(),
                key.getRequestId())
        );
    }
}
