package org.qubership.atp.itf.lite.backend.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(excludeAutoConfiguration = SecurityAutoConfiguration.class)
@TestPropertySource("classpath:application.properties")
public abstract class AbstractControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    protected static ObjectMapper objectMapper = new ObjectMapper();
}
