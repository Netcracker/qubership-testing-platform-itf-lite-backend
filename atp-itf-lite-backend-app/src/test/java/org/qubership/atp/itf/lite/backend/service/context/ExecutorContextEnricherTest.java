package org.qubership.atp.itf.lite.backend.service.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.qubership.atp.itf.lite.backend.service.UserService;
import org.qubership.atp.itf.lite.backend.utils.Constants;

@ExtendWith(MockitoExtension.class)
class ExecutorContextEnricherTest {

    @Mock
    private Provider<UserInfo> userInfoProvider;

    @Mock
    private UserService userService;

    @InjectMocks
    private ExecutorContextEnricher enricher;

    @Test
    void enrich_prefersProviderUserInfo_andDoesNotCallUserService() {
        UserInfo userInfo = org.mockito.Mockito.mock(UserInfo.class);
        when(userInfoProvider.get()).thenReturn(userInfo);
        when(userInfo.getFullName()).thenReturn("John Doe");
        when(userInfo.getUsername()).thenReturn("jdoe");

        Map<String, Object> ctx = new HashMap<>();
        enricher.enrich(ctx, "Bearer any", "My Request");

        assertEquals("John Doe", ctx.get(Constants.EXECUTOR_FIRST_LAST_NAME));
        assertEquals("jdoe", ctx.get(Constants.USERNAME));
        assertEquals("My Request", ctx.get(Constants.EXECUTION_REQUEST_NAME));
    }

    @Test
    void enrich_usesTokenLookup_whenProviderIsNull() {
        when(userInfoProvider.get()).thenReturn(null);

        UserInfo userInfo = org.mockito.Mockito.mock(UserInfo.class);
        when(userService.getUserInfoByToken("Bearer token")).thenReturn(userInfo);
        when(userInfo.getFullName()).thenReturn("");
        when(userInfo.getFirstName()).thenReturn("Jane");
        when(userInfo.getLastName()).thenReturn("Roe");
        when(userInfo.getUsername()).thenReturn("jroe");

        Map<String, Object> ctx = new HashMap<>();
        enricher.enrich(ctx, "Bearer token", "Another Request");

        assertEquals("Jane Roe", ctx.get(Constants.EXECUTOR_FIRST_LAST_NAME));
        assertEquals("jroe", ctx.get(Constants.USERNAME));
        assertEquals("Another Request", ctx.get(Constants.EXECUTION_REQUEST_NAME));
    }

    @Test
    void enrich_fallsBackToUnknown_whenNoUserInfoAndNoToken() {
        when(userInfoProvider.get()).thenReturn(null);

        Map<String, Object> ctx = new HashMap<>();
        enricher.enrich(ctx, null, "Req");

        assertEquals("Unknown User", ctx.get(Constants.EXECUTOR_FIRST_LAST_NAME));
        assertEquals("Unknown", ctx.get(Constants.USERNAME));
        assertEquals("Req", ctx.get(Constants.EXECUTION_REQUEST_NAME));
    }

    @Test
    void enrich_doesNotOverwriteExistingValues() {
        UserInfo userInfo = org.mockito.Mockito.mock(UserInfo.class);
        when(userInfoProvider.get()).thenReturn(userInfo);
        when(userInfo.getFullName()).thenReturn("John Doe");
        when(userInfo.getUsername()).thenReturn("jdoe");

        Map<String, Object> ctx = new HashMap<>();
        ctx.put(Constants.EXECUTOR_FIRST_LAST_NAME, "Preset Executor");
        ctx.put(Constants.USERNAME, "preset");
        ctx.put(Constants.EXECUTION_REQUEST_NAME, "Preset Request");

        enricher.enrich(ctx, "Bearer token", "New Request");

        assertEquals("Preset Executor", ctx.get(Constants.EXECUTOR_FIRST_LAST_NAME));
        assertEquals("preset", ctx.get(Constants.USERNAME));
        assertEquals("Preset Request", ctx.get(Constants.EXECUTION_REQUEST_NAME));
    }
}

