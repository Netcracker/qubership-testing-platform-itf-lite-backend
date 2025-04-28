package org.qubership.atp.itf.lite.backend.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.itf.lite.backend.model.entities.key.RequestSnapshotKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {RequestSnapshotCleaner.class, RequestSnapshotService.class},
        properties = {"atp.itf.lite.clean.snapshot.expiration.period.seconds=86400"})
class RequestSnapshotCleanerTest {

    @Autowired
    private RequestSnapshotCleaner requestSnapshotCleaner;

    @MockBean
    private RequestSnapshotService requestSnapshotService;


    @Test
    public void test_deleteOldSnapshots_deleted() {
        String snapshotExpirationPeriod = "86400";
        List<RequestSnapshotKey> expiredSnapshotIds = Arrays.asList(
                new RequestSnapshotKey(UUID.randomUUID(), UUID.randomUUID()),
                new RequestSnapshotKey(UUID.randomUUID(), UUID.randomUUID())
        );

        when(requestSnapshotService.getByCreatedWhenDifferenceGreaterThanReferenceDate(any(Date.class),
                eq(Long.parseLong(snapshotExpirationPeriod))))
                .thenReturn(expiredSnapshotIds);

        requestSnapshotCleaner.deleteOldSnapshots();

        for (RequestSnapshotKey key : expiredSnapshotIds) {
            verify(requestSnapshotService).deleteSnapshotByRequestSnapshotKey(key.getSessionId(), key.getRequestId());
        }
    }
}
