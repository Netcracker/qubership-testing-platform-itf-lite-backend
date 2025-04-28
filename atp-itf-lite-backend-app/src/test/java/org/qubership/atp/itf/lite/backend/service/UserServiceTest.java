package org.qubership.atp.itf.lite.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.itf.lite.backend.configuration.MapperConfiguration;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.UserSettingsRepository;
import org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator;
import org.qubership.atp.itf.lite.backend.model.entities.user.UserSettings;
import org.qubership.atp.itf.lite.backend.utils.JwtTokenUtils;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    private final ThreadLocal<RestTemplate> m2mRestTemplate = new ThreadLocal<>();
    private final ThreadLocal<UserSettingsRepository> repository = new ThreadLocal<>();
    private static final ModelMapper modelMapper = new MapperConfiguration().modelMapper();
    private final ThreadLocal<UserService> service = new ThreadLocal<>();

    @BeforeEach
    public void setUp() {
        RestTemplate m2mRestTemplateMock = mock(RestTemplate.class, "m2mRestTemplate");
        UserSettingsRepository repositoryMock = mock(UserSettingsRepository.class);
        m2mRestTemplate.set(m2mRestTemplateMock);
        repository.set(repositoryMock);
        service.set(new UserService(m2mRestTemplateMock, repositoryMock, modelMapper));
    }

    @Test
    public void saveUserSettings_whenSettingsSpecified_shouldSaveSettingsBySaveMethod() {
        // given
        final UserSettings userSettings = EntitiesGenerator.generateUserSettings();
        // when
        String token = Strings.EMPTY;
        service.get().saveUserSettings(userSettings, token);
        // then
        verify(repository.get()).save(userSettings);
    }

    @Test
    public void getSettingsByUser_whenIdSpecified_shouldReturnSettins() {
        // given
        String token = EntitiesGenerator.getToken();
        UUID userId = service.get().getUserIdFromToken(token);
        // when
        service.get().getSettingsByUser(token);
        //then
        verify(repository.get()).findByUserId(userId);
    }

    @Test
    public void saveUserSettings_whenExistingSettingsSpecified_shouldSaveSettings() {
        // given
        String token = EntitiesGenerator.getToken();
        UUID userId = service.get().getUserIdFromToken(token);
        final UserSettings userSettings = EntitiesGenerator.generateUserSettings();
        userSettings.setUserId(userId);
        // when
        when(repository.get().findByUserIdAndName(userId, userSettings.getName())).thenReturn(userSettings);
        service.get().saveUserSettings(userSettings, token);
        //then
        verify(repository.get()).save(userSettings);
    }

    @Test
    public void getUserInfoByTokenTest_successfullyGet() {
        // given
        UUID expectedUserId = UUID.randomUUID();
        String jwtToken = JwtTokenUtils.generateJwtTokenWithUserId(expectedUserId, service.get().sub);

        UserInfo expectedUserInfo = new UserInfo();
        expectedUserInfo.setId(UUID.randomUUID());
        expectedUserInfo.setUsername("username");
        // when
        when(m2mRestTemplate.get().getForObject(any(String.class), any())).thenReturn(expectedUserInfo);
        UserInfo actualUserInfo = service.get().getUserInfoByToken(jwtToken);

        // then
        assertEquals(expectedUserInfo, actualUserInfo);
    }

    @Test
    public void getUserInfoByTokenTest_failDuringGet() {
        // given
        UUID expectedUserId = UUID.randomUUID();
        String jwtToken = JwtTokenUtils.generateJwtTokenWithUserId(expectedUserId, service.get().sub);

        UserInfo expectedUserInfo = new UserInfo();
        expectedUserInfo.setId(UUID.randomUUID());
        expectedUserInfo.setUsername("username");
        // when
        doThrow(new RuntimeException("RuntimeExceptionMessage")).when(m2mRestTemplate.get()).getForObject(any(), any());
        UserInfo actualUserInfo = service.get().getUserInfoByToken(jwtToken);

        // then
        assertNull(actualUserInfo);
    }
}
