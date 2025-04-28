package org.qubership.atp.itf.lite.backend.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {PingController.class})
public class PingControllerTest extends AbstractControllerTest {

    @Test
    public void pingTest() throws Exception {
        this.mockMvc.perform(get("/atp-itf-lite/api/v1/ping"))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
