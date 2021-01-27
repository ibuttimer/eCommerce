package com.example.demo.controllers;

import com.example.demo.AbstractTest;
import com.example.demo.matcher.ItemField;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.util.Pair;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.example.demo.config.Config.*;
import static com.example.demo.controllers.UserControllerTest.createAndLoginUser;
import static com.example.demo.matcher.ItemJsonPathResultMatchers.itemJsonPath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ItemControllerTest extends AbstractTest {

    @Autowired
    UserController userController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    static ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    public static void beforeAll() {
        objectMapper = new ObjectMapper();
    }

    @BeforeEach
    public void beforeEach() {
        for (JpaRepository<?, Long> repository : List.of(userRepository, cartRepository, itemRepository)) {
            repository.deleteAll();
            assertEquals(0, repository.count(), () -> "Repository not empty: " + repository.getClass().getSimpleName());
        }
    }

    @DisplayName("Non-existent item not found")
    @Test
    void non_existentNotFound() throws Exception {
        // create user for access
        Pair<User, String> userAndAuth = createAndLoginUser(mockMvc, "username", "password");

        getItemNotFound(mockMvc, List.of(ITEM_GET_BY_ID_URL), Map.of(PATH_ID_PATTERN, 100L),
                userAndAuth.getSecond());
        // empty array for non-existent name
        getItemsOk(mockMvc, List.of(ITEM_GET_BY_NAME_URL), Map.of(PATH_NAME_PATTERN, "widget"),
                userAndAuth.getSecond(), List.of(), List.of());
    }

    @DisplayName("Get item by id/name")
    @Test
    void getAllItems() throws Exception {
        // create user for access
        Pair<User, String> userAndAuth = createAndLoginUser(mockMvc, "username", "password");
        User user = userAndAuth.getFirst();

        // create test item
        Item toothbrush = itemRepository.save(Item.of("Toothbrush", BigDecimal.valueOf(3.99), "Teeth cleaner"));

        // check find by id & name
        getItemOk(mockMvc, List.of(ITEM_GET_BY_ID_URL), Map.of(PATH_ID_PATTERN, toothbrush.getId()),
                userAndAuth.getSecond(), toothbrush, List.of());
        getItemsOk(mockMvc, List.of(ITEM_GET_BY_NAME_URL), Map.of(PATH_NAME_PATTERN, toothbrush.getName()),
                userAndAuth.getSecond(), List.of(toothbrush), List.of());

        // create similar name test item
        Item toothbrushHolder = itemRepository.save(Item.of("Toothbrush holder", BigDecimal.valueOf(1.99), "Teeth cleaner holder"));

        // check find by name
        getItemsOk(mockMvc, List.of(ITEM_GET_BY_NAME_URL), Map.of(PATH_NAME_PATTERN, toothbrush.getName()),
                userAndAuth.getSecond(), List.of(toothbrush), List.of());

        // create same name test item
        Item deluxeToothbrush = itemRepository.save(Item.of(toothbrush.getName(), BigDecimal.valueOf(1.99), "Gold-plated teeth cleaner"));

        // check find by name
        getItemsOk(mockMvc, List.of(ITEM_GET_BY_NAME_URL), Map.of(PATH_NAME_PATTERN, toothbrush.getName()),
                userAndAuth.getSecond(), List.of(toothbrush, deluxeToothbrush), List.of());
    }

    @DisplayName("Get all items")
    @Test
    void getItem() throws Exception {
        // create user for access
        Pair<User, String> userAndAuth = createAndLoginUser(mockMvc, "username", "password");
        User user = userAndAuth.getFirst();

        // create test item
        List<Item> items = IntStream.range(0, 5)
                .mapToObj(i ->
                        itemRepository.save(Item.of("Toothbrush" + i, BigDecimal.valueOf(0.99 + i), "Teeth cleaner " + i))
                )
                .collect(Collectors.toList());

        // get items
        getItemsOk(mockMvc, List.of(), Map.of(), userAndAuth.getSecond(), items, List.of());
    }

    @DisplayName("Unable to get item/items when not signed in")
    @Test
    void notSignedInGetItem() throws Exception {
        getItemForbidden(mockMvc, List.of(ITEM_GET_BY_ID_URL), Map.of(PATH_ID_PATTERN, 100L),"");
        getItemForbidden(mockMvc, List.of(ITEM_GET_BY_NAME_URL), Map.of(PATH_NAME_PATTERN, "widget"),"");
        getItemForbidden(mockMvc, List.of(), Map.of(),"");
    }


    private static ResultActions expectItem(ResultActions resultActions, List<ItemField> excludes, Item item, AtomicReference<Item> result) throws Exception {
        return resultActions
                .andExpect(itemJsonPath(excludes, "$").value(item))
                .andDo(mvcResult -> {
                    result.set(
                            objectMapper.readValue(
                                    mvcResult.getResponse().getContentAsString(), Item.class)
                    );
                });
    }

    private static ResultActions expectItems(ResultActions resultActions, List<ItemField> excludes,
                                             List<Item> items) throws Exception {
        return resultActions
                .andExpect(itemJsonPath(List.of(), "$").value(items));
    }

    private static ResultActions getItem(MockMvc mockMvc, List<String> parts, Map<String, Object> idMap, String authorisation) throws Exception {
        return mockMvc.perform(
                    get(getItemUri(parts, idMap))
                            .header(AUTHORIZATION, authorisation)
                );
    }

    private static void getItemNotFound(MockMvc mockMvc, List<String> parts, Map<String, Object> idMap, String authorisation) throws Exception {
        getItem(mockMvc, parts, idMap, authorisation)
            .andExpect(status().isNotFound());
    }

    private static void getItemForbidden(MockMvc mockMvc, List<String> parts, Map<String, Object> idMap, String authorisation) throws Exception {
        getItem(mockMvc, parts, idMap, authorisation)
            .andExpect(status().isForbidden());
    }

    private static Item getItemOk(MockMvc mockMvc, List<String> parts, Map<String, Object> idMap, String authorisation, Item item, List<ItemField> excludes) throws Exception {
        AtomicReference<Item> result = new AtomicReference<>();
        expectItem(
                    getItem(mockMvc, parts, idMap, authorisation)
                            .andExpect(status().isOk()), excludes,
                item, result);
        return result.get();
    }

    private static void getItemsOk(MockMvc mockMvc, List<String> parts, Map<String, Object> idMap, String authorisation, List<Item> items, List<ItemField> excludes) throws Exception {
        expectItems(
                    getItem(mockMvc, parts, idMap, authorisation)
                            .andExpect(status().isOk()),
                        excludes,
                        items);
    }

    public static String getItemUri(List<String> parts, Map<String, Object> replaceMap) {
        List<Pair<String, Object>> replacements;
        if (replaceMap != null) {
            replacements = replaceMap.entrySet().stream()
                    .map(e -> Pair.of(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        } else {
            replacements = List.of();
        }
        return getUrl(ITEM_URL, parts, Map.of(), replacements);
    }

}