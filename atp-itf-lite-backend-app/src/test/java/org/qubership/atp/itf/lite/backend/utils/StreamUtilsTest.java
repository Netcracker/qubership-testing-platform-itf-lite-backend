package org.qubership.atp.itf.lite.backend.utils;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.itf.lite.backend.model.entities.AbstractNamedEntity;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;

import lombok.Data;

@ExtendWith(MockitoExtension.class)
public class StreamUtilsTest {

    public static final int FIRST_LIST_ELEMENT = 0;

    @Data
    static class TestEntity extends AbstractNamedEntity {
        UUID projectId;
        UUID serialId;
        UUID groupId;
        List<UUID> childrenIds;

        public TestEntity(UUID id, String name, UUID projectId, UUID serialId) {
            this(id, name, projectId, serialId, null, null);
        }

        public TestEntity(UUID id, String name, UUID projectId, UUID serialId, UUID groupId) {
            this(id, name, projectId, serialId, groupId, null);
        }

        public TestEntity(UUID id, String name, UUID projectId, UUID serialId,
                          UUID groupId, List<UUID> childrenIds) {
            this.id = id;
            this.name = name;
            this.serialId = serialId;
            this.projectId = projectId;
            this.groupId = groupId;
            this.childrenIds = childrenIds;
        }
    }

    private static List<TestEntity> entities;

    private static UUID projectId;

    private static UUID groupOneId;
    private static UUID groupTwoId;

    private static TestEntity entity1;
    private static TestEntity entity2;
    private static TestEntity entity3;
    private static TestEntity entity4;
    private static TestEntity entity5;
    private static TestEntity entity6;

    @BeforeAll
    public static void setUp() {
        projectId = UUID.randomUUID();

        groupOneId = UUID.randomUUID();
        groupTwoId = UUID.randomUUID();

        entity1 = new TestEntity(UUID.randomUUID(), "Entity 1", projectId, UUID.randomUUID(), groupTwoId);
        entity2 = new TestEntity(UUID.randomUUID(), "Entity 2", projectId, UUID.randomUUID(), groupOneId);
        entity3 = new TestEntity(UUID.randomUUID(), "Entity 3", projectId, UUID.randomUUID(), groupOneId);
        entity4 = new TestEntity(UUID.randomUUID(), "Entity 4", projectId, UUID.randomUUID());
        entity5 = new TestEntity(UUID.randomUUID(), "Entity 5", projectId, UUID.randomUUID());
        entity6 = new TestEntity(UUID.randomUUID(), "Entity 6", projectId, UUID.randomUUID());

        entity1.childrenIds = asList(entity4.getId(), entity5.getId());
        entity2.childrenIds = singletonList(entity6.getId());

        entities = asList(entity1, entity2, entity3);
    }

    @Test
    public void extractIds_whenCollectionIsSpecified_shouldExtractIds() {
        Set<UUID> entityIds = StreamUtils.extractIds(entities);

        Assertions.assertEquals(entities.size(), entityIds.size());
        Assertions.assertTrue(entityIds.contains(entity1.getId()));
        Assertions.assertTrue(entityIds.contains(entity2.getId()));
        Assertions.assertTrue(entityIds.contains(entity3.getId()));
    }

    @Test
    public void extractIds_whenCollectionIsSpecifiedWithExtractFunction_shouldExtractIds() {
        Set<UUID> projectIds = StreamUtils.extractIds(entities, TestEntity::getProjectId);

        final Integer EXPECTED_ENTITY_ID_SIZE = 1;
        Assertions.assertEquals(EXPECTED_ENTITY_ID_SIZE, projectIds.size());
        Assertions.assertTrue(projectIds.contains(entity1.getProjectId()));
    }

    @Test
    public void extractIds_whenNullCollectionIsSpecified_shouldReturnEmptyResult() {
        Set<UUID> projectIds = StreamUtils.extractIds(null, TestEntity::getProjectId);

        Assertions.assertNotNull(projectIds);
        Assertions.assertTrue(projectIds.isEmpty());
    }

    @Test
    public void extractFlatIds_whenCollectionIsSpecifiedWithExtractFunction_shouldExtractFlatIds() {
        Set<UUID> childrenIds = StreamUtils.extractFlatIds(entities, TestEntity::getChildrenIds);

        final Integer EXPECTED_CHILDREN_IDS_SIZE = asList(entity4, entity5, entity6).size();
        Assertions.assertEquals(EXPECTED_CHILDREN_IDS_SIZE, childrenIds.size());
    }

    @Test
    public void extractFlatIds_whenNullCollectionIsSpecified_shouldReturnEmptyResult() {
        Set<UUID> projectIds = StreamUtils.extractFlatIds(null, TestEntity::getChildrenIds);

        Assertions.assertNotNull(projectIds);
        Assertions.assertTrue(projectIds.isEmpty());
    }

    @Test
    public void toIdEntityMap_whenCollectionIsSpecifiedWithExtractFunction_shouldReturnSerialIdEntityMap() {
        Map<UUID, TestEntity> entityToSerialIdMap = StreamUtils.toIdEntityMap(entities, TestEntity::getSerialId);

        Assertions.assertEquals(entities.size(), entityToSerialIdMap.size());

        Assertions.assertTrue(entityToSerialIdMap.containsKey(entity1.getSerialId()));
        Assertions.assertTrue(entityToSerialIdMap.containsValue(entity1));

        Assertions.assertTrue(entityToSerialIdMap.containsKey(entity2.getSerialId()));
        Assertions.assertTrue(entityToSerialIdMap.containsValue(entity2));

        Assertions.assertTrue(entityToSerialIdMap.containsKey(entity3.getSerialId()));
        Assertions.assertTrue(entityToSerialIdMap.containsValue(entity3));
    }

    @Test
    public void toIdEntityMap_whenCollectionIsSpecifiedWithoutExtractFunction_shouldReturnIdEntityMap() {
        Map<UUID, TestEntity> entityKeyMap = StreamUtils.toIdEntityMap(entities);

        Assertions.assertEquals(entities.size(), entityKeyMap.size());

        Assertions.assertTrue(entityKeyMap.containsKey(entity1.getId()));
        Assertions.assertTrue(entityKeyMap.containsValue(entity1));

        Assertions.assertTrue(entityKeyMap.containsKey(entity2.getId()));
        Assertions.assertTrue(entityKeyMap.containsValue(entity2));

        Assertions.assertTrue(entityKeyMap.containsKey(entity3.getId()));
        Assertions.assertTrue(entityKeyMap.containsValue(entity3));
    }

    @Test
    public void toIdNameEntityMap_whenCollectionIsSpecified_shouldReturnIdToNameEntityMap() {
        Map<UUID, String> idToNameEntityMap = StreamUtils.toIdNameEntityMap(entities);

        Assertions.assertEquals(entities.size(), idToNameEntityMap.size());

        Assertions.assertTrue(idToNameEntityMap.containsKey(entity1.getId()));
        Assertions.assertTrue(idToNameEntityMap.containsValue(entity1.getName()));

        Assertions.assertTrue(idToNameEntityMap.containsKey(entity2.getId()));
        Assertions.assertTrue(idToNameEntityMap.containsValue(entity2.getName()));

        Assertions.assertTrue(idToNameEntityMap.containsKey(entity3.getId()));
        Assertions.assertTrue(idToNameEntityMap.containsValue(entity3.getName()));
    }

    @Test
    public void toEntityListMap_whenCollectionIsSpecified_shouldReturnGroupMapByEntityGroupId() {
        Map<UUID, List<TestEntity>> groupToEntitiesMap =
                StreamUtils.toEntityListMap(entities, TestEntity::getGroupId);

        final Integer EXPECTED_GROUP_SIZE = 2;
        Assertions.assertEquals(EXPECTED_GROUP_SIZE, groupToEntitiesMap.size());

        Assertions.assertTrue(groupToEntitiesMap.containsKey(groupOneId));
        Assertions.assertTrue(groupToEntitiesMap.containsKey(groupTwoId));

        List<TestEntity> groupOneEntities = groupToEntitiesMap.get(groupOneId);
        final Integer EXPECTED_GROUP_ONE_SIZE = 2;
        Assertions.assertEquals(EXPECTED_GROUP_ONE_SIZE, groupOneEntities.size());

        List<TestEntity> groupTwoEntities = groupToEntitiesMap.get(groupTwoId);
        final Integer EXPECTED_GROUP_TWO_SIZE = 1;
        Assertions.assertEquals(EXPECTED_GROUP_TWO_SIZE, groupTwoEntities.size());
    }

    @Test
    public void mapToClazz_whenCollectionIsSpecified_shouldReturnMappedFolders() {
        List<TestEntity> entities = singletonList(entity1);
        List<Folder> folders = StreamUtils.mapToClazz(entities, Folder.class);

        final Integer EXPECTED_FOLDERS_SIZE = entities.size();
        Assertions.assertEquals(EXPECTED_FOLDERS_SIZE, folders.size());

        Folder folder = folders.get(FIRST_LIST_ELEMENT);
        Assertions.assertEquals(entity1.getId(), folder.getId());
        Assertions.assertEquals(entity1.getName(), folder.getName());
        Assertions.assertEquals(entity1.getProjectId(), folder.getProjectId());
    }

    @Test
    public void mapToClazz_whenSingleEntityIsSpecified_shouldReturnMappedFolder() {
        Folder folder = StreamUtils.mapToClazz(entity1, Folder.class);

        Assertions.assertEquals(entity1.getId(), folder.getId());
        Assertions.assertEquals(entity1.getName(), folder.getName());
        Assertions.assertEquals(entity1.getProjectId(), folder.getProjectId());
    }

    @Test
    public void extractFields_whenCollectionIsSpecified_shouldExtractEntityNames() {
        List<TestEntity> entities = singletonList(entity1);
        Set<String> entityNames = StreamUtils.extractFields(entities, AbstractNamedEntity::getName);

        final Integer EXPECTED_ENTITY_NAMES_SIZE = entities.size();
        Assertions.assertEquals(EXPECTED_ENTITY_NAMES_SIZE, entityNames.size());

        Assertions.assertTrue(entityNames.contains(entity1.getName()));
    }

    @Test
    public void extractFields_whenNullCollectionIsSpecified_shouldReturnEmptyResult() {
        Set<String> entityNames = StreamUtils.extractFields(null, AbstractNamedEntity::getName);

        Assertions.assertNotNull(entityNames);
        Assertions.assertTrue(entityNames.isEmpty());
    }

    @Test
    public void filterList_whenCollectionIsSpecifiedWithIdsFilter_shouldFilterEntities() {
        List<TestEntity> entities = asList(entity1, entity2);

        List<TestEntity> filteredEntities = StreamUtils.filterList(entities, singletonList(entity1.getId()));

        final Integer EXPECTED_FILTERED_ENTITIES_SIZE = 1;
        Assertions.assertEquals(EXPECTED_FILTERED_ENTITIES_SIZE, filteredEntities.size());

        TestEntity entity = filteredEntities.get(FIRST_LIST_ELEMENT);
        Assertions.assertEquals(entity1.getId(), entity.getId());
    }

    @Test
    public void filterList_whenCollectionIsSpecifiedWithFilterFunction_shouldFilterEntities() {
        List<TestEntity> entities = asList(entity1, entity2);

        List<TestEntity> filteredEntities =
                StreamUtils.filterList(entities, TestEntity::getGroupId, singletonList(groupOneId));

        final Integer EXPECTED_FILTERED_ENTITIES_SIZE = 1;
        Assertions.assertEquals(EXPECTED_FILTERED_ENTITIES_SIZE, filteredEntities.size());

        TestEntity entity = filteredEntities.get(FIRST_LIST_ELEMENT);
        Assertions.assertEquals(groupOneId, entity.getGroupId());
    }

    @Test
    public void getEntitiesFromMap() {
        Map<UUID, TestEntity> entityMap = StreamUtils.toEntityMap(entities, TestEntity::getId);

        HashSet<UUID> filteredIds = newHashSet(entity1.getId(), entity2.getId());
        List<TestEntity> filteredEntities =
                StreamUtils.getEntitiesFromMap(filteredIds, entityMap);

        final Integer EXPECTED_FILTERED_ENTITIES_SIZE = filteredIds.size();
        Assertions.assertEquals(EXPECTED_FILTERED_ENTITIES_SIZE, filteredEntities.size());

        Assertions.assertTrue(filteredEntities.contains(entity1));
        Assertions.assertTrue(filteredEntities.contains(entity2));
    }
}