package org.qubership.atp.itf.lite.backend.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.qubership.atp.auth.springbootstarter.entities.Operations;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.security.permissions.PolicyEnforcement;
import org.qubership.atp.auth.springbootstarter.services.UsersService;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;

public class WritePermissionsServiceTest {

    private final ThreadLocal<PolicyEnforcement> entityAccess = new ThreadLocal<>();
    private final ThreadLocal<UsersService> usersService = new ThreadLocal<>();
    private final ThreadLocal<Provider<UserInfo>> userInfoProvider = new ThreadLocal<>();
    private final ThreadLocal<WritePermissionsService> writePermissionsService = new ThreadLocal<>();


    @BeforeEach
    public void setUp() {
        PolicyEnforcement entityAccessMock = Mockito.mock(PolicyEnforcement.class);
        UsersService usersServiceMock = Mockito.mock(UsersService.class);
        Provider userInfoProviderMock = Mockito.mock(Provider.class);
        entityAccess.set(entityAccessMock);
        usersService.set(usersServiceMock);
        userInfoProvider.set(userInfoProviderMock);
        writePermissionsService.set(new WritePermissionsService(entityAccessMock, usersServiceMock, userInfoProviderMock));
    }

    @Test
    public void hasWritePermissionsTest_withFolderIdIsNull_ReturnTrue() {
        boolean result = writePermissionsService.get().hasWritePermissions(null, UUID.randomUUID());

        Assertions.assertTrue(result);
    }

    @Test
    public void hasWritePermissionsTest_isAdmin_ReturnTrue() {
        Map<String, Map<UUID, Operations>> permissions = new HashMap<>();
        Map<UUID, Operations> assignedUsers = new HashMap<>();
        assignedUsers.put(UUID.randomUUID(), new Operations());
        String objectName = "service-folder";
        permissions.put(objectName, assignedUsers);

        when(usersService.get().getPermissionsByObjectId(any(), any(), any())).thenReturn(permissions);
        when(usersService.get().getObjectName(any(), any())).thenReturn(objectName);
        when(entityAccess.get().isAdmin()).thenReturn(true);

        boolean result = writePermissionsService.get().hasWritePermissions(null, UUID.randomUUID());

        Assertions.assertTrue(result);
    }
}
