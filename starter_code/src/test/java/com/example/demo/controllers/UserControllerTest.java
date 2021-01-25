package com.example.demo.controllers;

import com.example.demo.AbstractTest;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.CreateUserRequest;
import com.example.demo.model.requests.LoginUserRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.example.demo.config.Config.*;
import static com.example.demo.controllers.LoginControllerTest.loginUserOk;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest extends AbstractTest {

    @Autowired
    UserController userController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    static ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    public static void beforeAll() {
        objectMapper = new ObjectMapper();
    }

    @BeforeEach
    public void beforeEach() {
        for (JpaRepository<?, Long> repository : List.of(userRepository, cartRepository)) {
            repository.deleteAll();
            assertEquals(0, repository.count(), () -> "Repository not empty: " + repository.getClass().getSimpleName());
        }
    }

    @DisplayName("Create user")
    @Test
    void createUser() throws Exception {
        createUserOk(mockMvc, User.of("username", "password"));
    }

    private static final String GOOD_PASSWORD = "goodRequest";

    @DisplayName("No user created when password invalid or user already exists")
    @ParameterizedTest
    @ValueSource(strings = {
            "123456",       // too short
            "notConfirmed", // not confirmed
            GOOD_PASSWORD,  // will be created
            "badRequest"    // will not be created
    })
    void invalidCreateUser(String password) throws Exception {
        final String username = "username";
        if (password.equals(GOOD_PASSWORD)) {
            createUserOk(mockMvc, User.of(username, password));
        } else {
            createUserBadRequest(mockMvc, CreateUserRequest.of(username, password, password.toLowerCase()));
        }
    }

    @DisplayName("Non-existent user not found")
    @Test
    void non_existentNotFound() throws Exception {
        // create user for access
        Pair<User, String> userAndAuth = createAndLoginUser(mockMvc, "username", "password");

        getUserNotFound(mockMvc, List.of(USER_GET_BY_ID_URL), Map.of(PATH_ID_PATTERN, 100L),
                userAndAuth.getSecond());
        getUserNotFound(mockMvc, List.of(USER_GET_BY_USERNAME_URL), Map.of(PATH_USERNAME_PATTERN, "invisible-man"),
                userAndAuth.getSecond());
    }

    @DisplayName("Get user by id/username")
    @Test
    void getUser() throws Exception {
        // create user for access
        Pair<User, String> userAndAuth = createAndLoginUser(mockMvc, "username", "password");
        User user = userAndAuth.getFirst();

        getUserOk(mockMvc, List.of(USER_GET_BY_ID_URL), Map.of(PATH_ID_PATTERN, user.getId()),
                userAndAuth.getSecond(), user);
        getUserOk(mockMvc, List.of(USER_GET_BY_USERNAME_URL), Map.of(PATH_USERNAME_PATTERN, user.getUsername()),
                userAndAuth.getSecond(), user);
    }

    @DisplayName("Unable to get user when not signed in")
    @Test
    void notSignedIn() throws Exception {
        getUserForbidden(mockMvc, List.of(USER_GET_BY_ID_URL), Map.of(PATH_ID_PATTERN, 100L), "");
        getUserForbidden(mockMvc, List.of(USER_GET_BY_USERNAME_URL), Map.of(PATH_USERNAME_PATTERN, "invisible-man"), "");
    }



    static User createUserOk(MockMvc mockMvc, User user) throws Exception {
        AtomicReference<User> result = new AtomicReference<>();
        expectUser(
                    createUser(mockMvc,
                                CreateUserRequest.of(user.getUsername(), user.getPassword(), user.getPassword()))
                                    .andExpect(status().isOk()),
                user.getUsername(), result);
        return result.get();
    }

    private static ResultActions expectUser(ResultActions resultActions, String username, AtomicReference<User> result) throws Exception {
        return resultActions
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andDo(mvcResult -> {
                    result.set(
                            objectMapper.readValue(
                                    mvcResult.getResponse().getContentAsString(), User.class)
                    );
                });
    }

    private static ResultActions createUser(MockMvc mockMvc, CreateUserRequest request) throws Exception {
        return mockMvc.perform(
                post(new URI(
                        getUserUri(List.of(USER_CREATE_URL), Map.of())))
                    .content(objectMapper.writeValueAsString(request))
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON));
    }

    private static void createUserBadRequest(MockMvc mockMvc, CreateUserRequest request) throws Exception {
        createUser(mockMvc, request)
            .andExpect(status().isBadRequest());
    }

    private static ResultActions getUser(MockMvc mockMvc, List<String> parts, Map<String, Object> idMap, String authorisation) throws Exception {
        return mockMvc.perform(
                    get(getUserUri(parts, idMap))
                            .header(AUTHORIZATION, authorisation)
                );
    }

    private static void getUserStatus(MockMvc mockMvc, List<String> parts, Map<String, Object> idMap, String authorisation, ResultMatcher matcher) {
        try {
            getUser(mockMvc, parts, idMap, authorisation)
                .andExpect(matcher);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    private static void getUserNotFound(MockMvc mockMvc, List<String> parts, Map<String, Object> idMap, String authorisation) {
        getUserStatus(mockMvc, parts, idMap, authorisation, status().isNotFound());
    }

    private static void getUserForbidden(MockMvc mockMvc, List<String> parts, Map<String, Object> idMap, String authorisation) {
        getUserStatus(mockMvc, parts, idMap, authorisation, status().isForbidden());
    }

    private static User getUserOk(MockMvc mockMvc, List<String> parts, Map<String, Object> idMap, String authorisation, User user) throws Exception {
        AtomicReference<User> result = new AtomicReference<>();
        expectUser(
                    getUser(mockMvc, parts, idMap, authorisation)
                            .andExpect(status().isOk()),
                user.getUsername(), result)
            .andExpect(jsonPath("$.id").value(user.getId()));
        return result.get();
    }


    /**
     * Create & login a user
     * @param mockMvc - mockMvc
     * @param username - username for user
     * @param password - password for user
     * @return Pair of user id & authorisation
     * @throws Exception
     */
    public static Pair<User, String> createAndLoginUser(MockMvc mockMvc, String username, String password) throws Exception {
        User user = createUserOk(mockMvc, User.of(username, password));
        return Pair.of(user,
                loginUserOk(mockMvc, LoginUserRequest.of(user.getUsername(), password)));
    }


    public static String getUserUri(List<String> parts, Map<String, Object> replaceMap) {
        List<Pair<String, Object>> replacements;
        if (replaceMap != null) {
            replacements = replaceMap.entrySet().stream()
                    .map(e -> Pair.of(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        } else {
            replacements = List.of();
        }
        return getUrl(USER_URL, parts, Map.of(), replacements);
    }

}