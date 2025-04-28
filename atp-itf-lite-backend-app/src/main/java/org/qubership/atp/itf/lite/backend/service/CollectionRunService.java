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

import java.sql.Timestamp;
import java.util.Calendar;

import org.qubership.atp.itf.lite.backend.dataaccess.repository.CollectionRunNextRequestRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.CollectionRunRequestsCountRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.CollectionRunRequestsRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.CollectionRunStackRequestsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionRunService {

    private final CollectionRunNextRequestRepository collectionRunNextRequestRepository;
    private final CollectionRunRequestsCountRepository collectionRunRequestsCountRepository;
    private final CollectionRunRequestsRepository collectionRunRequestsRepository;
    private final CollectionRunStackRequestsRepository collectionRunStackRequestsRepository;

    /**
     * Clean up overdue collection run records.
     *
     * @param collectionRunsRemoveDays records older than collectionRunsRemoveDays will be removed
     */
    @Transactional
    public void cleanUpRequestExecutionHistory(int collectionRunsRemoveDays) {
        Calendar calendar = Calendar.getInstance();
        int minusDays = collectionRunsRemoveDays > 0 ? collectionRunsRemoveDays * -1 : collectionRunsRemoveDays;
        calendar.add(Calendar.DAY_OF_MONTH, minusDays);
        Timestamp expirationTimestamp = new Timestamp(calendar.getTimeInMillis());
        log.info("[clean_up] COLLECTION_RUN_NEXT_REQUEST {} records got deleted.",
                collectionRunNextRequestRepository.deleteByCreatedWhenBefore(expirationTimestamp));
        log.info("[clean_up] COLLECTION_RUN_REQUESTS_COUNT {} records got deleted.",
                collectionRunRequestsCountRepository.deleteByCreatedWhenBefore(expirationTimestamp));
        log.info("[clean_up] COLLECTION_RUN_REQUESTS_ORDER {} records got deleted.",
                collectionRunRequestsRepository.deleteByCreatedWhenBefore(expirationTimestamp));
        log.info("[clean_up] COLLECTION_RUN_STACK_REQUESTS_ORDER {} records got deleted.",
                collectionRunStackRequestsRepository.deleteByCreatedWhenBefore(expirationTimestamp));
    }
}
