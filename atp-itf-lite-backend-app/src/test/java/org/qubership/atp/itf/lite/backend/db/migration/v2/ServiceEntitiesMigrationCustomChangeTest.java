package org.qubership.atp.itf.lite.backend.db.migration.v2;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.qubership.atp.auth.springbootstarter.entities.ServiceEntities;
import org.qubership.atp.auth.springbootstarter.services.UsersService;
import org.qubership.atp.itf.lite.backend.configuration.SpringLiquibaseBeanAware;
import org.qubership.atp.itf.lite.backend.utils.UserManagementEntities;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import liquibase.database.Database;

@DirtiesContext
@ExtendWith(SpringExtension.class)
public class ServiceEntitiesMigrationCustomChangeTest {

    private static String TOPIC_NAME = "service_entities";
    private ServiceEntitiesMigrationCustomChange serviceEntitiesMigrationCustomChange;
    @Mock
    private Database database;
    @MockBean
    private ApplicationContext applicationContext;
    private SpringLiquibaseBeanAware springLiquibaseBeanAware = new SpringLiquibaseBeanAware();

    @Mock
    private KafkaTemplate<UUID, String> kafkaTemplate;


    @BeforeEach
    public void setUp() {
        UsersService usersService = new UsersService(null, kafkaTemplate);
        ReflectionTestUtils.setField(usersService, "topicName", TOPIC_NAME);
        serviceEntitiesMigrationCustomChange = new ServiceEntitiesMigrationCustomChange();
        serviceEntitiesMigrationCustomChange.setServiceName("atp-itf-lite");
        Mockito.when(applicationContext.getBean(UsersService.class)).thenReturn(usersService);
        springLiquibaseBeanAware.setResourceLoader(applicationContext);
    }

    @Test
    public void execute_Test() throws JsonProcessingException {
        serviceEntitiesMigrationCustomChange.execute(database);

        ServiceEntities entities = new ServiceEntities();
        entities.setUuid(UUID.fromString("13f3c496-63af-4441-83bf-e2642b04bc94"));
        entities.setService("atp-itf-lite");
        entities.setEntities(Arrays.stream(UserManagementEntities.values())
                .map(UserManagementEntities::getName)
                .collect(Collectors.toList()));

        String expectedMessage = new ObjectMapper().writeValueAsString(entities);
        Mockito.verify(kafkaTemplate).send(TOPIC_NAME, expectedMessage);
    }
}
