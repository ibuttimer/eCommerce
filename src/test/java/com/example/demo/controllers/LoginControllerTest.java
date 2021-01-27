package com.example.demo.controllers;

import com.example.demo.AbstractTest;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.LoginUserRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.example.demo.config.Config.AUTHORIZATION;
import static com.example.demo.config.Config.LOGIN_URL;
import static com.example.demo.controllers.UserControllerTest.createUserOk;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoginControllerTest extends AbstractTest {

    @Autowired
    UserController userController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    static ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void beforeEach() {
        for (JpaRepository<?, Long> repository : List.of(userRepository, cartRepository)) {
            repository.deleteAll();
            assertEquals(0, repository.count(), () -> "Repository not empty: " + repository.getClass().getSimpleName());
        }
    }

    @DisplayName("Login user")
    @Test
    void loginUser() throws Exception {
        final String password = "password";
        User user = createUserOk(mockMvc, User.of("username", password));
        loginUserOk(mockMvc, LoginUserRequest.of(user.getUsername(), password));
    }

    @DisplayName("Login fail with incorrect password")
    @Test
    void badPasswordLoginFail() throws Exception {
        final String password = "password";
        User user = createUserOk(mockMvc, User.of("username", password));
        loginUserUnauthorized(mockMvc, LoginUserRequest.of(user.getUsername(), password + "typo"));
    }

    @DisplayName("Login fail with non-existent user")
    @Test
    void nonExistentUserLoginFail() throws Exception {
        loginUserUnauthorized(mockMvc, LoginUserRequest.of("username", "password"));
    }


    public static ResultActions loginUser(MockMvc mockMvc, LoginUserRequest request) throws Exception {
        return mockMvc.perform(
                    post(new URI(LOGIN_URL))
                .content(objectMapper.writeValueAsString(request))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));
    }

    public static void loginUserUnauthorized(MockMvc mockMvc, LoginUserRequest request) throws Exception {
        loginUser(mockMvc, request)
            .andExpect(status().isUnauthorized());
    }

    public static String loginUserOk(MockMvc mockMvc, LoginUserRequest request) throws Exception {
        AtomicReference<String> result = new AtomicReference<>();
        loginUser(mockMvc, request)
            .andExpect(status().isOk())
            .andDo(mvcResult -> {
                result.set(
                        mvcResult.getResponse().getHeader(AUTHORIZATION)
                );
            });
        return result.get();
    }

    public static String loginUserOk(MockMvc mockMvc, User user) throws Exception {
        return loginUserOk(mockMvc,
                LoginUserRequest.of(user.getUsername(), user.getPassword()));
    }
}