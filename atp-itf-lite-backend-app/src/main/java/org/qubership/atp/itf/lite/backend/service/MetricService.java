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

import java.util.UUID;

import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricService {

    private final MeterRegistry meterRegistry;
    public static final String ITF_LITE_REQUESTS_COUNTER = "itf.lite.run.collections.requests.count";
    public static final String ITF_LITE_RUN_COLLECTIONS_COUNTER = "itf.lite.run.collections.count";
    public static final String ITF_LITE_REQUESTS_SIZE = "itf.lite.run.requests.size";
    public static final String ITF_LITE_RESPONSE_SIZE = "itf.lite.run.response.size";
    public static final String PROJECT_ID = "projectId";
    public static final String TRANSPORT_TYPE = "transportType";
    private final Counter.Builder itfLiteRequestsCounter = Counter.builder(ITF_LITE_REQUESTS_COUNTER)
            .description("Counter for all requests");
    private final Counter.Builder itfLiteRunCollectionCounter =
            Counter.builder(ITF_LITE_RUN_COLLECTIONS_COUNTER)
                    .description("Counter for run collections");
    private final Counter.Builder itfLiteRequestsSizeCounter = Counter.builder(ITF_LITE_REQUESTS_SIZE)
            .description("Counter for requests size");
    private final Counter.Builder itfLiteResponseSizeCounter = Counter.builder(ITF_LITE_RESPONSE_SIZE)
            .description("Counter for response size");

    public Timer timer(String name, String... tags) {
        return meterRegistry.timer(name, tags);
    }

    public void registerCountCollectionRequests(UUID projectId) {
        incrementByTypeTag(projectId.toString(), ITF_LITE_REQUESTS_COUNTER);
    }

    public void registerCountRunCollections(UUID projectId) {
        incrementByTypeTag(projectId.toString(), ITF_LITE_RUN_COLLECTIONS_COUNTER);
    }

    /**
     * Increment request size by project and transportType.
     * @param size add created counter.
     * @param projectId Project ID.
     * @param transportType Request Type.
     */
    public void incrementRequestSizePerProject(Double size, UUID projectId, TransportType transportType) {
        incrementByBuilderCounterAndTags(itfLiteRequestsSizeCounter, size, PROJECT_ID, projectId.toString(),
                TRANSPORT_TYPE, transportType.getName());
    }

    /**
     * Increment response size by project and transportType.
     * @param size add created counter.
     * @param projectId Project ID.
     * @param transportType Request Type.
     */
    public void incrementResponseSizePerProject(Double size, UUID projectId, TransportType transportType) {
        incrementByBuilderCounterAndTags(itfLiteResponseSizeCounter, size, PROJECT_ID, projectId.toString(),
                TRANSPORT_TYPE, transportType.getName());
    }

    private void incrementByTypeTag(@NonNull String project, @NonNull String requestType) {
        switch (requestType) {
            case ITF_LITE_REQUESTS_COUNTER:
                incrementByBuilderCounterAndTags(itfLiteRequestsCounter,
                        PROJECT_ID, project);
                break;
            case ITF_LITE_RUN_COLLECTIONS_COUNTER:
                incrementByBuilderCounterAndTags(itfLiteRunCollectionCounter,
                        PROJECT_ID, project);
                break;
            default:
                break;
        }
    }

    /**
     * Increment counter by tags.
     * @param counter add created counter.
     * @param amount counter amount.
     * @param tags add tags by type key\value.
     */
    private void incrementByBuilderCounterAndTags(Counter.Builder counter, double amount, String... tags) {
        counter.tags(tags)
                .register(meterRegistry)
                .increment(amount);
    }

    /**
     * Increment counter by tags.
     * @param counter add created counter.
     * @param tags add tags by type key\value.
     */
    private void incrementByBuilderCounterAndTags(Counter.Builder counter, String... tags) {
        incrementByBuilderCounterAndTags(counter, 1.0, tags);
    }
}
