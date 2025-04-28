package org.qubership.atp.itf.lite.backend.model.entities.converters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.TreeMap;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.qubership.atp.itf.lite.backend.model.entities.PermissionEntity;

class PermissionEntityConverterTest {

    private static final PermissionEntityConverter permissionEntityConverter = new PermissionEntityConverter();
    private static final String userName = "FirstName LastName";
    private static final UUID userId1 = UUID.randomUUID();
    private static final UUID userId2 = UUID.randomUUID();

    @Test
    void convertToDbAndBack_null() {
        assertNull(permissionEntityConverter.convertToDatabaseColumn(null));
        assertEquals(new PermissionEntity(), permissionEntityConverter.convertToEntityAttribute(null));
    }

    @Test
    void convertToDbAndBack_emptyObject() {
        PermissionEntity permissionEntity = new PermissionEntity();
        String jsonValue = permissionEntityConverter.convertToDatabaseColumn(permissionEntity);
        assertNull(jsonValue);
        assertEquals(permissionEntity, permissionEntityConverter.convertToEntityAttribute(jsonValue));
    }
    @Test
    void convertToDbAndBack_nonEmptyObject() {
        TreeMap<UUID, String> users = new TreeMap<>();
        users.put(userId1, userName);
        PermissionEntity permissionEntity = new PermissionEntity(users);
        String jsonValue = permissionEntityConverter.convertToDatabaseColumn(permissionEntity);
        assertTrue(jsonValue.contains("\"enable\":true"));
        assertTrue(jsonValue.contains("\"userAccess\":{\"" + userId1 + "\":\"" + userName + "\"}"));
        assertEquals(permissionEntity, permissionEntityConverter.convertToEntityAttribute(jsonValue));
    }

    @Test
    void convertToDbAndBack_nonEmptyObjectWithNullName() {
        TreeMap<UUID, String> users = new TreeMap<>();
        users.put(userId1, userName);
        users.put(userId2, null);
        PermissionEntity permissionEntity = new PermissionEntity(users);
        String jsonValue = permissionEntityConverter.convertToDatabaseColumn(permissionEntity);
        assertTrue(jsonValue.contains("\"" + userId1 + "\":\"" + userName + "\""));
        assertTrue(jsonValue.contains("\"" + userId2 + "\":null"));
        assertTrue(jsonValue.contains("\"enable\":true"));
        assertEquals(permissionEntity, permissionEntityConverter.convertToEntityAttribute(jsonValue));
    }

    @Test
    void convertToDbAndBack_nonEmptyObjectWithNullNameWithSort() {
        UUID user1Id = new UUID( 0,9);
        UUID user2Id = new UUID( 0,5);
        TreeMap<UUID, String> users = new TreeMap<>();
        users.put(user1Id, userName);
        users.put(user2Id, null);
        PermissionEntity permissionEntity = new PermissionEntity(users);
        String jsonValue = permissionEntityConverter.convertToDatabaseColumn(permissionEntity);
        //assertEquals("{\"userAccess\":{\"" + userId2 + "\":null,\"" + userId1 + "\":\"" + userName + "\"},\"enable\":true}", jsonValue);
        assertTrue(jsonValue.contains("\"enable\":true"));
        assertTrue(jsonValue.contains("\"userAccess\":{\"" + user2Id + "\":null,\"" + user1Id + "\":\"" + userName + "\"}"));
        PermissionEntity fromDb = permissionEntityConverter.convertToEntityAttribute(jsonValue);
        assertEquals(permissionEntity, fromDb);
        assertEquals(user2Id, fromDb.getUserAccess().firstKey());
    }
}
