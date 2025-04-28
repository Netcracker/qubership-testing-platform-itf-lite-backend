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

package org.qubership.atp.itf.lite.backend.dataaccess.repository;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.qubership.atp.itf.lite.backend.enums.SortType;
import org.qubership.atp.itf.lite.backend.enums.TransportType;
import org.qubership.atp.itf.lite.backend.model.entities.history.HistoryFilteringParams;
import org.qubership.atp.itf.lite.backend.model.entities.history.HistorySearchRequest;
import org.qubership.atp.itf.lite.backend.model.entities.history.PaginatedResponse;
import org.qubership.atp.itf.lite.backend.model.entities.history.RequestExecution;
import org.qubership.atp.itf.lite.backend.model.entities.history.SortParams;
import org.springframework.stereotype.Repository;

import clover.org.apache.commons.collections.CollectionUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@AllArgsConstructor
public class CustomRequestExecutionRepositoryImpl implements CustomRequestExecutionRepository {

    private static final String REQUEST_EXECUTION_FILTER_COLUMN_PROJECT_ID = "projectId";
    private static final String REQUEST_EXECUTION_FILTER_COLUMN_EXECUTOR = "executor";
    private static final String REQUEST_EXECUTION_FILTER_COLUMN_REQUEST_NAME = "name";
    private static final String REQUEST_EXECUTION_FILTER_COLUMN_TRANSPORT_TYPE = "transportType";
    private static final String REQUEST_EXECUTION_FILTER_COLUMN_EXECUTED_WHEN = "executedWhen";
    private static final String REQUEST_EXECUTION_FILTER_COLUMN_TIMESTAMP = "TIMESTAMP";

    EntityManager entityManager;

    @Override
    public PaginatedResponse<RequestExecution> findAllRequestExecutions(HistorySearchRequest request) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<RequestExecution> criteriaQuery = criteriaBuilder.createQuery(RequestExecution.class);

        Root<RequestExecution> requestExecution = criteriaQuery.from(RequestExecution.class);
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(criteriaBuilder.equal(requestExecution
                .get(REQUEST_EXECUTION_FILTER_COLUMN_PROJECT_ID), request.getProjectId()));

        HistoryFilteringParams historyFilteringParams = request.getFilters();

        List<String> executors = historyFilteringParams.getExecutor();
        if (CollectionUtils.isNotEmpty(executors)) {
            CriteriaBuilder.In<String> inClause = criteriaBuilder
                    .in(requestExecution.get(REQUEST_EXECUTION_FILTER_COLUMN_EXECUTOR));
            executors.forEach(inClause::value);
            predicates.add(inClause);
        }

        List<TransportType> types = historyFilteringParams.getType();
        if (CollectionUtils.isNotEmpty(types)) {
            CriteriaBuilder.In<TransportType> inClause = criteriaBuilder
                    .in(requestExecution.get(REQUEST_EXECUTION_FILTER_COLUMN_TRANSPORT_TYPE));
            types.forEach(inClause::value);
            predicates.add(inClause);
        }

        criteriaQuery.where(predicates.toArray(new Predicate[0]));

        List<SortParams> sortParams = request.getSort();
        if (CollectionUtils.isNotEmpty(sortParams)) {
            sortParams.forEach(param -> {
                if (REQUEST_EXECUTION_FILTER_COLUMN_TIMESTAMP.equals(param.getColumn())) {
                    SortType sortType = param.getSortType();
                    if (SortType.ASC.equals(sortType)) {
                        criteriaQuery.orderBy(criteriaBuilder.asc(requestExecution
                                .get(REQUEST_EXECUTION_FILTER_COLUMN_EXECUTED_WHEN)));
                    } else if (SortType.DESC.equals(sortType)) {
                        criteriaQuery.orderBy(criteriaBuilder.desc(requestExecution
                                .get(REQUEST_EXECUTION_FILTER_COLUMN_EXECUTED_WHEN)));
                    }
                }
            });
        }

        List<RequestExecution> requestExecutionList;
        try {
            requestExecutionList = entityManager.createQuery(criteriaQuery)
                    .setFirstResult(request.getOffset())
                    .setMaxResults(request.getLimit())
                    .getResultList();
        } finally {
            entityManager.clear();
        }

        CriteriaQuery<Long> criteriaQueryTotal = criteriaBuilder.createQuery(Long.class);
        criteriaQueryTotal.select(criteriaBuilder.count(criteriaQueryTotal.from(RequestExecution.class)));
        List<Predicate> criteriaQueryPredicates = new ArrayList<>();
        criteriaQueryPredicates.add(criteriaBuilder.equal(requestExecution
                .get(REQUEST_EXECUTION_FILTER_COLUMN_PROJECT_ID), request.getProjectId()));
        TypedQuery<Long> query = entityManager.createQuery(criteriaQueryTotal
                .where(criteriaQueryPredicates.toArray(new Predicate[0])));
        long total;
        try {
            total = query.getSingleResult();
        } finally {
            entityManager.clear();
        }

        return new PaginatedResponse<>(total, requestExecutionList);
    }
}