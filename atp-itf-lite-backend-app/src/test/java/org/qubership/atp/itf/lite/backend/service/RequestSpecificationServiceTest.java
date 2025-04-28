package org.qubership.atp.itf.lite.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
public class RequestSpecificationServiceTest {

    private static final RequestSpecificationService requestSpecificationService = new RequestSpecificationService();

    @Test
    public void generateSpecificationToFilterRequestsByProjectIdFolderIdsRequestIdsTest_successfullyCreated() {
        // given
        UUID projectId = UUID.randomUUID();
        Set<UUID> folderIds = Collections.singleton(UUID.randomUUID());
        Set<UUID> requestIds = Collections.singleton(UUID.randomUUID());
        Predicate expectedPredicateAnd = mock(Predicate.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        CriteriaQuery criteriaQuery = mock(CriteriaQuery.class);
        Root root = mock(Root.class);
        // when
        when(criteriaBuilder.equal(any(), any(Object.class))).thenReturn(mock(Predicate.class));
        when(criteriaBuilder.or(any())).thenReturn(mock(Predicate.class));
        when(criteriaBuilder.and(any())).thenReturn(expectedPredicateAnd);
        Specification<Request> expectedSpecification =
                requestSpecificationService.generateSpecificationToFilterRequestsByProjectIdFolderIdsRequestIds(
                        projectId, folderIds, requestIds);
        // then
        assertNotNull(expectedSpecification);
        Predicate actualPredicate = expectedSpecification.toPredicate(root, criteriaQuery, criteriaBuilder);
        assertEquals(expectedPredicateAnd, actualPredicate);
    }

    @Test
    public void generateSpecificationToFilterRequestsByProjectIdFolderIdsRequestIdsTest_emptyParameters_successfullyCreated() {
        // given
        Predicate expectedConjunctionPredicate = mock(Predicate.class);
        CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
        CriteriaQuery criteriaQuery = mock(CriteriaQuery.class);
        Root root = mock(Root.class);

        // when
        when(criteriaBuilder.conjunction()).thenReturn(expectedConjunctionPredicate);
        Specification<Request> expectedSpecification =
                requestSpecificationService.generateSpecificationToFilterRequestsByProjectIdFolderIdsRequestIds(
                        null, null, null);
        // then
        assertNotNull(expectedSpecification);

        Predicate actualPredicate = expectedSpecification.toPredicate(root, criteriaQuery, criteriaBuilder);
        assertEquals(expectedConjunctionPredicate, actualPredicate);
    }
}
