package com.example.demo;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = SareetaApplication.class
)
@AutoConfigureMockMvc
public class AbstractTest {

    @Autowired
    protected MockMvc mockMvc;

    @LocalServerPort
    private Integer port;

    @BeforeAll
    public static void beforeAll() {
        // no-op
    }

    @AfterAll
    public static void afterAll() {
        // no-op
    }

}
