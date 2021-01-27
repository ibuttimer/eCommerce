package com.example.demo.controllers;

import com.example.demo.AbstractTest;
import com.example.demo.matcher.CartField;
import com.example.demo.matcher.OrderField;
import com.example.demo.matcher.UserField;
import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.UserOrder;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.persistence.repositories.OrderRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.ModifyCartRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.example.demo.config.Config.*;
import static com.example.demo.controllers.CartControllerTest.addItemOk;
import static com.example.demo.controllers.UserControllerTest.createAndLoginUser;
import static com.example.demo.matcher.OrderJsonPathResultMatchers.orderJsonPath;
import static com.example.demo.matcher.UserField.CHECK_ID_USERNAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerTest extends AbstractTest {

    @Autowired
    UserController userController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    static ObjectMapper objectMapper = new ObjectMapper();

    // user for access
    Pair<User, String> userAndAuth;

    // test items
    Item toothbrush;
    Item toothbrushHolder;

    @BeforeAll
    public static void beforeAll() {
        objectMapper = new ObjectMapper();
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        for (JpaRepository<?, Long> repository : List.of(orderRepository, userRepository, cartRepository, itemRepository)) {
            repository.deleteAll();
            assertEquals(0, repository.count(), () -> "Repository not empty: " + repository.getClass().getSimpleName());
        }
        // create user for access
        userAndAuth = createAndLoginUser(mockMvc, "username", "password");
        // create test item
        toothbrush = itemRepository.save(Item.of("Toothbrush", BigDecimal.valueOf(3.99), "Teeth cleaner"));
        toothbrushHolder = itemRepository.save(Item.of("Toothbrush holder", BigDecimal.valueOf(1.99), "Teeth cleaner holder"));
    }

    @DisplayName("Place order")
    @Test
    void placeOrder() {
        placeOrderOf(List.of(
                Pair.of(toothbrush, 1)));
    }

    UserOrder placeOrderOf(List<Pair<Item, Integer>> items) {
        final User user = userAndAuth.getFirst();
        final String username = user.getUsername();
        final String authorisation = userAndAuth.getSecond();

        Cart cart = Cart.of(user);
        for (Pair<Item, Integer> orderItem : items) {
            // update cart
            Item item = orderItem.getFirst();
            int quantity = orderItem.getSecond();
            cart.addItem(item, quantity);

            // add item
            addItemOk(mockMvc, List.of(CartField.ID), CHECK_ID_USERNAME,
                    cart, ModifyCartRequest.of(username, item.getId(), quantity), authorisation);
        }

        // expected order
        UserOrder order = UserOrder.createFromCart(cart);

        // place
        return placeOrderOk(mockMvc, List.of(OrderField.ID), CHECK_ID_USERNAME,
                                order, username, authorisation);
    }

    @DisplayName("Unable to place empty order")
    @Test
    void cannotPlaceEmptyOrder() {
        final User user = userAndAuth.getFirst();
        final String username = user.getUsername();
        final String authorisation = userAndAuth.getSecond();

        placeOrderNoContent(mockMvc, username, authorisation);
    }

    @DisplayName("Invalid user unable to place order")
    @Test
    void invalidUserPlaceOrder() {
        final User user = userAndAuth.getFirst();
        final String username = user.getUsername();
        final String authorisation = userAndAuth.getSecond();

        placeOrderNotFound(mockMvc, "scarlett_pimpernel", authorisation);
    }

    @DisplayName("Get order history")
    @Test
    void getOrderHistory() throws Exception {
        final User user = userAndAuth.getFirst();
        final String username = user.getUsername();
        final String authorisation = userAndAuth.getSecond();

        // place orders
        UserOrder toothbrushOrder = placeOrderOf(List.of(
                                                    Pair.of(toothbrush, 1)));
        UserOrder holderOrder = placeOrderOf(List.of(
                                                Pair.of(toothbrushHolder, 2)));

        getHistoryOk(mockMvc, username, authorisation, List.of(toothbrushOrder, holderOrder),
                List.of(), CHECK_ID_USERNAME);
    }

    @DisplayName("Invalid user unable to get order history")
    @Test
    void invalidUserGetOrderHistory() throws Exception {
        final User user = userAndAuth.getFirst();
        final String username = user.getUsername();
        final String authorisation = userAndAuth.getSecond();

        getHistoryNotFound(mockMvc, "scarlett_pimpernel", authorisation);
    }

    @DisplayName("Unable to place order/get history when not signed in")
    @Test
    void notSignedIn() {
        placeOrderForbidden(mockMvc, "scarlett_pimpernel", "");
        getHistoryForbidden(mockMvc, "scarlett_pimpernel", "");
    }

    static UserOrder placeOrderOk(MockMvc mockMvc, List<OrderField> excludes, List<UserField> userExcludes, UserOrder order,
                                  String username, String authorisation) {
        AtomicReference<UserOrder> result = new AtomicReference<>();
        try {
            expectOrder(
                    placeOrder(mockMvc, username, authorisation)
                            .andExpect(status().isOk()),
                    excludes, userExcludes, order, result);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return result.get();
    }

    static void placeOrderStatus(MockMvc mockMvc, String username, String authorisation, ResultMatcher matcher) {
        try {
            placeOrder(mockMvc, username, authorisation)
                    .andExpect(matcher);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    static void placeOrderNotFound(MockMvc mockMvc, String username, String authorisation) {
        placeOrderStatus(mockMvc, username, authorisation, status().isNotFound());
    }

    static void placeOrderNoContent(MockMvc mockMvc, String username, String authorisation) {
        placeOrderStatus(mockMvc, username, authorisation, status().isNoContent());
    }

    static void placeOrderForbidden(MockMvc mockMvc, String username, String authorisation) {
        placeOrderStatus(mockMvc, username, authorisation, status().isForbidden());
    }


    private static ResultActions placeOrder(MockMvc mockMvc, String username, String authorisation) throws Exception {
        return mockMvc.perform(
                post(new URI(
                            getOrderUri(List.of(ORDER_SUBMIT_URL), Map.of(PATH_USERNAME_PATTERN, username))))
                        .header(AUTHORIZATION, authorisation)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON));
    }


    private static ResultActions expectOrder(ResultActions resultActions,
                                             List<OrderField> excludes, List<UserField> userExcludes, UserOrder order,
                                             AtomicReference<UserOrder> result) throws Exception {
        return resultActions
                .andExpect(orderJsonPath(excludes, userExcludes, "$").value(order))
                .andDo(mvcResult -> {
                    result.set(
                            objectMapper.readValue(
                                    mvcResult.getResponse().getContentAsString(), UserOrder.class)
                    );
                });
    }

    private static ResultActions expectOrders(ResultActions resultActions,
                                             List<OrderField> excludes, List<UserField> userExcludes, List<UserOrder> orders) throws Exception {
        return resultActions
                .andExpect(orderJsonPath(excludes, userExcludes, "$").value(orders));
    }

    private static ResultActions getHistory(MockMvc mockMvc, String username, String authorisation) throws Exception {
        return mockMvc.perform(
                    get(getOrderUri(List.of(ORDER_HISTORY_URL), Map.of(PATH_USERNAME_PATTERN, username)))
                            .header(AUTHORIZATION, authorisation)
                );
    }

    private static void getHistoryStatus(MockMvc mockMvc, String username, String authorisation, ResultMatcher matcher) {
        try {
            getHistory(mockMvc, username, authorisation)
                .andExpect(matcher);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    private static void getHistoryNotFound(MockMvc mockMvc, String username, String authorisation) {
        getHistoryStatus(mockMvc, username, authorisation, status().isNotFound());
    }

    private static void getHistoryForbidden(MockMvc mockMvc, String username, String authorisation) {
        getHistoryStatus(mockMvc, username, authorisation, status().isForbidden());
    }

    private static void getHistoryOk(MockMvc mockMvc, String username, String authorisation, List<UserOrder> orders,
                                     List<OrderField> excludes, List<UserField> userExcludes) throws Exception {
        expectOrders(
                    getHistory(mockMvc, username, authorisation)
                            .andExpect(status().isOk()), excludes, userExcludes,
                orders);
    }

    public static String getOrderUri(List<String> parts, Map<String, Object> replaceMap) {
        List<Pair<String, Object>> replacements;
        if (replaceMap != null) {
            replacements = replaceMap.entrySet().stream()
                    .map(e -> Pair.of(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        } else {
            replacements = List.of();
        }
        return getUrl(ORDER_URL, parts, Map.of(), replacements);
    }

}