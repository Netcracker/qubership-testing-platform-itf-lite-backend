package org.qubership.atp.itf.lite.backend.service.history;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.javers.common.string.PrettyValuePrinter;
import org.javers.core.Changes;
import org.javers.core.Javers;
import org.javers.core.JaversCoreProperties;
import org.javers.core.commit.CommitId;
import org.javers.core.commit.CommitMetadata;
import org.javers.core.diff.Change;
import org.javers.core.diff.changetype.PropertyChangeMetadata;
import org.javers.core.diff.changetype.PropertyChangeType;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.javers.core.metamodel.object.GlobalId;
import org.javers.core.metamodel.object.InstanceId;
import org.javers.shadow.Shadow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.qubership.atp.itf.lite.backend.configuration.MapperConfiguration;
import org.qubership.atp.itf.lite.backend.converters.history.HttpRequestVersioningMapper;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.FolderRepository;
import org.qubership.atp.itf.lite.backend.feign.dto.history.CompareEntityResponseDto;
import org.qubership.atp.itf.lite.backend.feign.dto.history.HistoryItemDto;
import org.qubership.atp.itf.lite.backend.feign.dto.history.HistoryItemResponseDto;
import org.qubership.atp.itf.lite.backend.feign.dto.history.HttpRequestHistoryDto;
import org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator;
import org.qubership.atp.itf.lite.backend.model.entities.http.HttpRequest;
import org.qubership.atp.itf.lite.backend.service.GridFsService;
import org.qubership.atp.itf.lite.backend.service.history.impl.HttpRequestRetrieveHistoryService;

public class RetrieveHistoryServiceTest {
    private final ThreadLocal<Javers> javers = new ThreadLocal<>();
    private final ThreadLocal<GridFsService> gridFsService = new ThreadLocal<>();
    private final ThreadLocal<CommitMetadata> metadata = new ThreadLocal<>();
    private final ThreadLocal<HttpRequestRetrieveHistoryService> httpRequestRetrieveHistoryService = new ThreadLocal<>();
    private static final ModelMapper mapper = new MapperConfiguration().modelMapper();
    private static final HttpRequestVersioningMapper httpRequestVersioningMapper = new HttpRequestVersioningMapper(mapper);

    @BeforeEach
    public void setUp() {
        Javers javersMock = mock(Javers.class);
        GridFsService gridFsServiceMock = mock(GridFsService.class);
        CommitMetadata commitMetadataMock = mock(CommitMetadata.class);
        javers.set(javersMock);
        gridFsService.set(gridFsServiceMock);
        metadata.set(commitMetadataMock);
        httpRequestRetrieveHistoryService.set(
                new HttpRequestRetrieveHistoryService(javersMock, httpRequestVersioningMapper, mock(FolderRepository.class)));
    }

    @Test
    public void getEntitiesByVersionsTest_withHttpRequest_ReturnHistoryItem() {
        UUID cdoId = UUID.randomUUID();
        Shadow shadow = mock(Shadow.class);

        HttpRequest request1 = EntitiesGenerator.generateRandomHttpRequest();
        request1.setModifiedWhen(new Date());
        request1.setCreatedWhen(new Date());
        HttpRequest request2 = EntitiesGenerator.generateRandomHttpRequest();
        request2.setModifiedWhen(new Date());
        request2.setCreatedWhen(new Date());

        when(javers.get().findShadows(any())).thenReturn(Collections.singletonList(shadow));
        when(shadow.get()).thenReturn(request1);
        when(shadow.get()).thenReturn(request2);
        CommitMetadata commitMetadata = new CommitMetadata("author", new HashMap<>(), LocalDateTime.now(),
                null, CommitId.valueOf(BigDecimal.valueOf(800.0)));
        when(shadow.getCommitMetadata()).thenReturn(commitMetadata);
        CdoSnapshot snapshot = mock(CdoSnapshot.class);
        when(javers.get().findSnapshots(any())).thenReturn(Collections.singletonList(snapshot));
        when(snapshot.getCommitId()).thenReturn(commitMetadata.getId());
        when(snapshot.getCommitMetadata()).thenReturn(commitMetadata);

        List<CompareEntityResponseDto> result = httpRequestRetrieveHistoryService.get()
                .getEntitiesByVersions(cdoId, Arrays.asList("1", "2"));

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("1", result.get(0).getRevision());
        Assertions.assertEquals("2", result.get(1).getRevision());
        Assertions.assertTrue(result.get(0).getCompareEntity() instanceof HttpRequestHistoryDto);
        Assertions.assertTrue(result.get(1).getCompareEntity() instanceof HttpRequestHistoryDto);
        HttpRequestHistoryDto entity1 = (HttpRequestHistoryDto) result.get(0).getCompareEntity();
        HttpRequestHistoryDto entity2 = (HttpRequestHistoryDto) result.get(0).getCompareEntity();
        Assertions.assertEquals(2, entity1.getRequestParams().size());
        Assertions.assertEquals("author", entity1.getModifiedBy());
        Assertions.assertEquals(2, entity2.getRequestParams().size());
        Assertions.assertEquals(3, entity1.getRequestHeaders().size());
        Assertions.assertEquals(3, entity2.getRequestHeaders().size());
        Assertions.assertNotNull(entity1.getBody());
        Assertions.assertNotNull(entity2.getBody());
    }

    @Test
    public void getAllHistoryTest_withHttpRequest_ReturnAllHistory() {
        UUID cdoId = UUID.randomUUID();
        GlobalId globalId = new InstanceId(HttpRequest.class.getTypeName(), cdoId, cdoId.toString());
        Changes changes = new Changes(createChanges(globalId),
                new PrettyValuePrinter(new JaversCoreProperties.PrettyPrintDateFormats()));

        when(javers.get().findChanges(any())).thenReturn(changes);
        CdoSnapshot snapshot = mock(CdoSnapshot.class);
        when(javers.get().findSnapshots(any())).thenReturn(Collections.singletonList(snapshot));
        when(metadata.get().getId()).thenReturn(new CommitId(1L, 1));
        when(snapshot.getCommitId()).thenReturn(new CommitId(1L, 1));
        when(snapshot.getVersion()).thenReturn(1L);
        when(metadata.get().getCommitDate()).thenReturn(LocalDateTime.now());

        HistoryItemResponseDto response = httpRequestRetrieveHistoryService.get().getAllHistory(cdoId, 0, 10);

        HistoryItemDto historyItem = response.getHistoryItems().get(0);
        Assertions.assertEquals(2, historyItem.getChanged().size());
        Assertions.assertEquals("name", historyItem.getChanged().get(0));
        Assertions.assertEquals("order", historyItem.getChanged().get(1));
    }

    private List<Change> createChanges(GlobalId globalId) {
        ValueChange change1 = new ValueChange(
                new PropertyChangeMetadata(
                        globalId,
                        "name",
                        Optional.of(metadata.get()),
                        PropertyChangeType.PROPERTY_VALUE_CHANGED
                ),
                "name1",
                "name2"
        );
        ValueChange change2 = new ValueChange(
                new PropertyChangeMetadata(
                        globalId,
                        "order",
                        Optional.of(metadata.get()),
                        PropertyChangeType.PROPERTY_ADDED
                ),
                "",
                "1"
        );
        return Arrays.asList(change1, change2);
    }
}
