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
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.persistence.EntityNotFoundException;

import org.qubership.atp.itf.lite.backend.dataaccess.repository.CollectionRunNextRequestRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.CollectionRunRequestsCountRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.CollectionRunRequestsRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.CollectionRunStackRequestsRepository;
import org.qubership.atp.itf.lite.backend.model.entities.collection.run.CollectionRunNextRequest;
import org.qubership.atp.itf.lite.backend.model.entities.collection.run.CollectionRunRequest;
import org.qubership.atp.itf.lite.backend.model.entities.collection.run.CollectionRunRequestsCount;
import org.qubership.atp.itf.lite.backend.model.entities.collection.run.CollectionRunStackRequest;
import org.qubership.atp.itf.lite.backend.utils.StreamUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NextRequestService {

    private final CollectionRunNextRequestRepository collectionRunNextRequestRepository;
    private final CollectionRunRequestsCountRepository collectionRunRequestsCountRepository;
    private final CollectionRunRequestsRepository collectionRunRequestsRepository;
    private final CollectionRunStackRequestsRepository collectionRunStackRequestsRepository;

    @Value("${request.execution.count.max}")
    private int maxExecutionCount;

    /**
     * Checks if the next request exists for execute.
     *
     * @param testRunId collection execution testRunId
     */
    public boolean hasNextRequest(UUID testRunId) {
        log.debug("hasNextRequest (testRunId: {})", testRunId);
        return collectionRunNextRequestRepository.existsByCollectionRunId(testRunId);
    }

    /**
     * Set next request for collection execution.
     * @param testRunId test run id
     * @param nextRequest nextRequest name or id
     */
    public void setNextRequest(UUID testRunId, @Nullable String nextRequest) {
        log.debug("setNextRequest (testRunId: {}, nextRequest: {})", testRunId, nextRequest);
        collectionRunNextRequestRepository.save(new CollectionRunNextRequest(testRunId, nextRequest, new Date()));
    }

    /**
     * Return next request by testRunId.
     * @param testRunId test run id
     * @return next request name or id
     */
    public String getNextRequest(UUID testRunId) {
        log.debug("getNextRequest(testRunId: {})", testRunId);
        return collectionRunNextRequestRepository.getNextRequestByCollectionRunId(testRunId);
    }

    /**
     * Removes next request entry by testRunId.
     * @param testRunId test run id
     */
    @Transactional
    public void deleteNextRequest(UUID testRunId) {
        log.debug("deleteNextRequest(testRunId: {})", testRunId);
        collectionRunNextRequestRepository.removeByCollectionRunId(testRunId);
    }

    /**
     * Checks if the request execution limit for a single test run is exceeded.
     * @param testRunId test run id
     * @param requestId request id
     * @return true if limit exceeded
     */
    public boolean isExecutionLimitExceeded(UUID testRunId, UUID requestId) {
        log.debug("isExecutionLimitExceeded(testRunId: {}, requestId: {})", testRunId, requestId);
        int executionCount = getCountOfRequestExecution(testRunId, requestId);
        return executionCount > maxExecutionCount;
    }

    /**
     * Returns count of request executions for a single collection execution.
     * @param testRunId test run id
     * @param requestId request id
     * @return count of request executions
     */
    public int getCountOfRequestExecution(UUID testRunId, UUID requestId) {
        log.debug("getCountOfRequestExecution(testRunId: {}, requestId: {})", testRunId, requestId);
        try {
            Integer count = collectionRunRequestsCountRepository
                    .findCountByCollectionRunIdAndRequestId(testRunId, requestId);
            if (Objects.isNull(count)) {
                return 0;
            }
            return count;
        } catch (EntityNotFoundException ignore) {
            log.warn("Count not found by testRunId: {} and requestId: {}", testRunId, requestId);
        }
        return 0;
    }

    /**
     * Checks that request has been executed in collection.
     * @param testRunId test run id
     * @param requestId request id
     * @return true if request was executed
     */
    public boolean hasRequestInCollectionOrder(UUID testRunId, UUID requestId) {
        log.debug("hasRequestInCollectionOrder(testRunId: {}, requestId: {})", testRunId, requestId);
        return collectionRunRequestsRepository.existsByCollectionRunIdAndRequestId(testRunId, requestId);
    }

    /**
     * Increments execution count for request.
     * @param testRunId test run id
     * @param requestId request id
     */
    public void incrementExecutionCount(UUID testRunId, UUID requestId) {
        log.debug("incrementExecutionCount(testRunId: {}, requestId: {})", testRunId, requestId);
        CollectionRunRequestsCount requestExecutionCount = collectionRunRequestsCountRepository
                .findByCollectionRunIdAndRequestId(testRunId, requestId);
        if (Objects.isNull(requestExecutionCount)) {
            requestExecutionCount = new CollectionRunRequestsCount(testRunId, requestId, null, 0, new Date());
        }
        requestExecutionCount.setCount(requestExecutionCount.getCount() + 1);
        collectionRunRequestsCountRepository.save(requestExecutionCount);
    }

    /**
     * Adds request to collection execution order.
     * Request will not be added if it already exists
     * @param testRunId test run id
     * @param requestId request id
     */
    public void addRequestToCollectionOrder(UUID testRunId, UUID requestId, String requestName) {
        log.debug("addRequestToCollectionOrder(testRunId: {}, requestId: {})", testRunId, requestId);
        if (hasRequestInCollectionOrder(testRunId, requestId)) {
            log.debug("Request already in collection order table. (testRunId: {}, requestId: {})",
                    testRunId, requestId);
            return;
        }
        int order = collectionRunRequestsRepository.countByCollectionRunId(testRunId) + 1;
        CollectionRunRequest execOrder = new CollectionRunRequest();
        execOrder.setCollectionRunId(testRunId);
        execOrder.setRequestId(requestId);
        execOrder.setRequestName(requestName);
        execOrder.setOrder(order);
        collectionRunRequestsRepository.save(execOrder);
    }

    /**
     * Searches next request in collection order table and return result if found else null.
     * @param testRunId test run id
     * @return found collectionRunRequest or null
     */
    @Nullable
    public CollectionRunRequest findInCollectionOrderNextRequest(UUID testRunId) {
        log.debug("findInCollectionOrderNextRequest(testRunId: {})", testRunId);
        String request = getNextRequest(testRunId);
        if (Objects.isNull(request)) {
            return null;
        }
        // find last executed request in collection
        CollectionRunRequest collReq = collectionRunRequestsRepository
                .findFirstByCollectionRunIdAndRequestNameOrderByOrderDesc(testRunId, request);
        if (Objects.isNull(collReq)) {
            log.trace("CollectionRunRequest not found by name. RequestName: {}", request);
            try {
                log.trace("Try to cast next request to uuid. NextRequest: {}", request);
                UUID requestId = UUID.fromString(request);
                log.trace("Search CollectionRunRequest by requestId: {}", requestId);
                return collectionRunRequestsRepository
                        .findFirstByCollectionRunIdAndRequestIdOrderByOrderDesc(testRunId, requestId);
            } catch (IllegalArgumentException ex) {
                log.debug("Provided request string is not in UUID format");
                return null;
            }
        }
        return collReq;
    }

    /**
     * Creates new sub collection for execution.
     * Starts from provided collRun to last request
     * @param testRunId test run id
     * @param collRun collectionRunRequest to start sub collection
     */
    @Transactional
    public void createNewSubCollection(UUID testRunId, CollectionRunRequest collRun) {
        log.debug("createNewSubCollection(testRunId: {}, StartCollectionRequest: {})", testRunId, collRun);
        collectionRunStackRequestsRepository.removeByCollectionRunId(testRunId);
        List<CollectionRunRequest> subColl = collectionRunRequestsRepository
                .findAllByCollectionRunIdAndOrderGreaterThanEqualOrderByOrder(testRunId, collRun.getOrder());
        List<CollectionRunStackRequest> newSubCollStack = StreamUtils
                .mapToClazz(subColl, CollectionRunStackRequest.class);
        collectionRunStackRequestsRepository.saveAll(newSubCollStack);
    }

    /**
     * Gets and remove next request from sub collection.
     * @param testRunId test run id
     * @return collectionRunRequest from stack of sub collection
     */
    @Transactional
    public CollectionRunStackRequest pop(UUID testRunId) {
        log.debug("pop(testRunId: {})", testRunId);
        CollectionRunStackRequest firstInStack = collectionRunStackRequestsRepository
                .findFirstByCollectionRunIdOrderByOrder(testRunId);
        log.trace("Found first request in collection execution stack - {}", firstInStack);
        if (Objects.nonNull(firstInStack)) {
            collectionRunStackRequestsRepository.delete(firstInStack);
        }
        return firstInStack;
    }

    public boolean isSubCollectionExists(UUID testRunId) {
        return collectionRunStackRequestsRepository.existsByCollectionRunId(testRunId);
    }

}
