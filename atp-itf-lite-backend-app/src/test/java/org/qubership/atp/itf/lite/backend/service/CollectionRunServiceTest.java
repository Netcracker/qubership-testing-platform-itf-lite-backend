package org.qubership.atp.itf.lite.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.CollectionRunNextRequestRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.CollectionRunRequestsCountRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.CollectionRunRequestsRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.CollectionRunStackRequestsRepository;

@ExtendWith(MockitoExtension.class)
public class CollectionRunServiceTest {

    @Mock
    private CollectionRunNextRequestRepository collectionRunNextRequestRepository;
    @Mock
    private CollectionRunRequestsCountRepository collectionRunRequestsCountRepository;
    @Mock
    private CollectionRunRequestsRepository collectionRunRequestsRepository;
    @Mock
    private CollectionRunStackRequestsRepository collectionRunStackRequestsRepository;
    @InjectMocks
    private CollectionRunService collectionRunService;

    @Test
    public void cleanUpCollectionRunsTest_shouldCallCollectionRunRepositoriesDeleteMethods() {
        // given
        final int collectionRunsRemoveDays = 1;
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, collectionRunsRemoveDays * -1);
        Timestamp timestamp = new Timestamp(calendar.getTimeInMillis());
        // when
        collectionRunService.cleanUpRequestExecutionHistory(collectionRunsRemoveDays);
        // then
        ArgumentCaptor<Timestamp> captureRequestTimestamp = ArgumentCaptor.forClass(Timestamp.class);
        verify(collectionRunNextRequestRepository, times(1)).deleteByCreatedWhenBefore(captureRequestTimestamp.capture());
        verify(collectionRunRequestsCountRepository, times(1)).deleteByCreatedWhenBefore(captureRequestTimestamp.capture());
        verify(collectionRunRequestsRepository, times(1)).deleteByCreatedWhenBefore(captureRequestTimestamp.capture());
        verify(collectionRunStackRequestsRepository, times(1)).deleteByCreatedWhenBefore(captureRequestTimestamp.capture());
        List<Timestamp> timestamps = captureRequestTimestamp.getAllValues();
        timestamps.forEach(actualTimestamp -> {
            if (!actualTimestamp.equals(timestamp)) {
                // 10 milliseconds for slow execution
                if (actualTimestamp.getTime() > timestamp.getTime() && actualTimestamp.getTime() - 10 < timestamp.getTime()) {
                    assertTrue(true);
                } else {
                    assertEquals(timestamp, actualTimestamp);
                }
            }
        });
        assertTrue(true);
    }
}
