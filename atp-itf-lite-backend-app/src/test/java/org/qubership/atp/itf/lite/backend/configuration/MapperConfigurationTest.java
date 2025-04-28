package org.qubership.atp.itf.lite.backend.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.AuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.entities.auth.RequestAuthorization;
import org.reflections.Reflections;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

class MapperConfigurationTest {

    private static final String AUTH_ENTITIES_PACKAGE_SCAN = "org.qubership.atp.itf.lite.backend.model.entities.auth";

    @Test
    public void should_check_presence_of_ModelMapper() {
        ApplicationContextRunner context = new ApplicationContextRunner()
                .withUserConfiguration(ModelMapper.class);
        context.run(t -> {
            assertThat(t).hasSingleBean(ModelMapper.class);
        });
    }
    @Test
    public void should_check_presence_of_ObjectMapper() {
        ApplicationContextRunner context = new ApplicationContextRunner()
                .withUserConfiguration(ObjectMapper.class);
        context.run(t -> {
            assertThat(t).hasSingleBean(ObjectMapper.class);
        });
    }

    // DO NOT UPDATE THIS TEST
    // ALL CHANGES ARE MADE IN THE MapperConfiguration.class
    @Test
    public void compareAuthTypeCountAndTypeMapEntryCount_shouldBeEquals() throws NoSuchFieldException, IllegalAccessException {
        Set<Class<? extends RequestAuthorization>> authClasses = lookupAuthEntities();
        MapperConfiguration mapperConfiguration = new MapperConfiguration();
        Field authTypeMapFiled = mapperConfiguration.getClass().getDeclaredField("authorizationToSaveAuthorizationMap");
        authTypeMapFiled.setAccessible(true);
        Map<Class<?extends RequestAuthorization>, Class<? extends AuthorizationSaveRequest>> authTypeMap =
                (Map<Class<?extends RequestAuthorization>, Class<? extends AuthorizationSaveRequest>>) authTypeMapFiled.get(mapperConfiguration);
        Assertions.assertEquals(authTypeMap.size() - 1, authClasses.size(),
                "If this test failed, it means you have added or removed an authorisation type. "
                + "Update the authorizationToSaveAuthorizationMap for object mapping in MapperConfiguration");
    }

    private Set<Class<? extends RequestAuthorization>> lookupAuthEntities() {
        Reflections reflections = new Reflections(AUTH_ENTITIES_PACKAGE_SCAN);
        return reflections.getSubTypesOf(RequestAuthorization.class);
    }

}