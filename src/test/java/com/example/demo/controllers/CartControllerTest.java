package com.example.demo.controllers;

import com.example.demo.AbstractTest;
import com.example.demo.matcher.CartField;
import com.example.demo.matcher.UserField;
import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.ItemRepository;
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

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.example.demo.config.Config.*;
import static com.example.demo.controllers.UserControllerTest.createAndLoginUser;
import static com.example.demo.matcher.CartJsonPathResultMatchers.cartJsonPath;
import static com.example.demo.matcher.UserField.CHECK_ID_USERNAME;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartControllerTest extends AbstractTest {

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
        for (JpaRepository<?, Long> repository : List.of(userRepository, cartRepository, itemRepository)) {
            repository.deleteAll();
            assertEquals(0, repository.count(), () -> "Repository not empty: " + repository.getClass().getSimpleName());
        }
        // create user for access
        userAndAuth = createAndLoginUser(mockMvc, "username", "password");
        // create test item
        toothbrush = itemRepository.save(Item.of("Toothbrush", BigDecimal.valueOf(3.99), "Teeth cleaner"));
        toothbrushHolder = itemRepository.save(Item.of("Toothbrush holder", BigDecimal.valueOf(1.99), "Teeth cleaner holder"));
    }

    @DisplayName("Add item/items to cart")
    @Test
    void addItem() {
        final User user = userAndAuth.getFirst();
        final String username = user.getUsername();
        final String authorisation = userAndAuth.getSecond();

        // expected cart
        Cart cart = Cart.of(user);
        cart.addItem(toothbrush);

        // add item
        addItemOk(mockMvc, List.of(CartField.ID), CHECK_ID_USERNAME,
                cart, ModifyCartRequest.of(username, toothbrush.getId(), 1), authorisation);

        // add additional item
        cart.addItem(toothbrushHolder);
        addItemOk(mockMvc, List.of(CartField.ID), CHECK_ID_USERNAME,
                cart, ModifyCartRequest.of(username, toothbrushHolder.getId(), 1), authorisation);

        // add additional multiple items
        cart.addItem(toothbrushHolder, 2);
        addItemOk(mockMvc, List.of(CartField.ID), CHECK_ID_USERNAME,
                cart, ModifyCartRequest.of(username, toothbrushHolder.getId(), 2), authorisation);
    }

    @DisplayName("Unable to add/remove item when not signed in")
    @Test
    void notSignedInAddRemoveItem() {
        addItemForbidden(mockMvc, ModifyCartRequest.of("suspicious", toothbrush.getId(), 1), "");
        removeItemForbidden(mockMvc, ModifyCartRequest.of("suspicious", toothbrush.getId(), 1), "");
    }

    @DisplayName("Unable to add invalid item to cart")
    @Test
    void addInvalidItem() {
        final User user = userAndAuth.getFirst();
        final String username = user.getUsername();
        final String authorisation = userAndAuth.getSecond();

        addItemNotFound(mockMvc, ModifyCartRequest.of(username, 100L, 1), authorisation);
    }

    @DisplayName("Invalid user unable to add item to cart")
    @Test
    void invalidUserAddItem() {
        final User user = userAndAuth.getFirst();
        final String username = user.getUsername();
        final String authorisation = userAndAuth.getSecond();

        addItemNotFound(mockMvc, ModifyCartRequest.of("scarlett_pimpernel", toothbrush.getId(), 1), authorisation);
    }

    @DisplayName("Remove item/items from cart")
    @Test
    @Transactional  // needed because of lazy loading
    void removeItem() {
        final User user = userAndAuth.getFirst();
        final String username = user.getUsername();
        final String authorisation = userAndAuth.getSecond();

        // expected cart
        List<CartField> exclude = List.of(CartField.ID, CartField.USER);
        int toothbrushCount = 1;
        int holderCount = 3;

        Cart cart = cartRepository.findByUserId(user.getId());
        assertNotNull(cart, "Cart not found");

        cart.addItem(toothbrush, toothbrushCount);
        cart.addItem(toothbrushHolder, holderCount);
        cart = cartRepository.save(cart);

        assertEquals(cart.getItems().size(), toothbrushCount + holderCount);

        BigDecimal price = new BigDecimal(0);
        price = price.add(toothbrush.getPrice().multiply(
                BigDecimal.valueOf(toothbrushCount)))
                .add(toothbrushHolder.getPrice().multiply(
                        BigDecimal.valueOf(holderCount)));
        assertEquals(price, cart.getTotal());

        // remove item
        cart.removeItem(toothbrush);
        removeItemOk(mockMvc, exclude, List.of(),
                cart, ModifyCartRequest.of(username, toothbrush.getId(), 1), authorisation);

        // remove additional item
        cart.removeItem(toothbrushHolder);
        removeItemOk(mockMvc, exclude, List.of(),
                cart, ModifyCartRequest.of(username, toothbrushHolder.getId(), 1), authorisation);

        // remove additional multiple items
        cart.removeItem(toothbrushHolder, 2);
        removeItemOk(mockMvc, exclude, List.of(),
                cart, ModifyCartRequest.of(username, toothbrushHolder.getId(), 2), authorisation);
    }

    @DisplayName("Unable to add invalid item to cart")
    @Test
    void removeInvalidItem() {
        final User user = userAndAuth.getFirst();
        final String username = user.getUsername();
        final String authorisation = userAndAuth.getSecond();

        removeItemNotFound(mockMvc, ModifyCartRequest.of(username, 100L, 1), authorisation);
    }

    @DisplayName("Invalid user unable to add item to cart")
    @Test
    void invalidUserRemoveItem() {
        final User user = userAndAuth.getFirst();
        final String username = user.getUsername();
        final String authorisation = userAndAuth.getSecond();

        removeItemNotFound(mockMvc, ModifyCartRequest.of("scarlett_pimpernel", toothbrush.getId(), 1), authorisation);
    }


    static Cart addItemOk(MockMvc mockMvc, List<CartField> excludes, List<UserField> userExcludes, Cart cart,
                          ModifyCartRequest request, String authorisation) {
        AtomicReference<Cart> result = new AtomicReference<>();
        try {
            expectCart(
                    modifyCart(mockMvc, CART_ADD_TO_URL, request, authorisation)
                            .andExpect(status().isOk()),
                    excludes, userExcludes, cart, result);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return result.get();
    }

    static void addItemNotFound(MockMvc mockMvc, ModifyCartRequest request, String authorisation) {
        try {
            modifyCart(mockMvc, CART_ADD_TO_URL, request, authorisation)
                    .andExpect(status().isNotFound());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    static void addItemForbidden(MockMvc mockMvc, ModifyCartRequest request, String authorisation) {
        try {
            modifyCart(mockMvc, CART_ADD_TO_URL, request, authorisation)
                    .andExpect(status().isForbidden());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    static Cart removeItemOk(MockMvc mockMvc, List<CartField> excludes, List<UserField> userExcludes, Cart cart,
                          ModifyCartRequest request, String authorisation) {
        AtomicReference<Cart> result = new AtomicReference<>();
        try {
            expectCart(
                    modifyCart(mockMvc, CART_REMOVE_FROM_URL, request, authorisation)
                            .andExpect(status().isOk()),
                    excludes, userExcludes, cart, result);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        return result.get();
    }

    static void removeItemNotFound(MockMvc mockMvc, ModifyCartRequest request, String authorisation) {
        try {
            modifyCart(mockMvc, CART_REMOVE_FROM_URL, request, authorisation)
                    .andExpect(status().isNotFound());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    static void removeItemForbidden(MockMvc mockMvc, ModifyCartRequest request, String authorisation) {
        try {
            modifyCart(mockMvc, CART_REMOVE_FROM_URL, request, authorisation)
                    .andExpect(status().isForbidden());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    private static ResultActions modifyCart(MockMvc mockMvc, String url, ModifyCartRequest request, String authorisation) throws Exception {
        return mockMvc.perform(
                post(new URI(
                            getCartUri(List.of(url), Map.of())))
                        .header(AUTHORIZATION, authorisation)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON));
    }


    private static ResultActions expectCart(ResultActions resultActions,
                                            List<CartField> excludes, List<UserField> userExcludes, Cart cart,
                                            AtomicReference<Cart> result) throws Exception {
        return resultActions
                .andExpect(cartJsonPath(excludes, userExcludes, "$").value(cart))
                .andDo(mvcResult -> {
                    result.set(
                            objectMapper.readValue(
                                    mvcResult.getResponse().getContentAsString(), Cart.class)
                    );
                });
    }

    private static ResultActions getItem(MockMvc mockMvc, List<String> parts, Map<String, Object> idMap, String authorisation) throws Exception {
        return mockMvc.perform(
                    get(getCartUri(parts, idMap))
                            .header(AUTHORIZATION, authorisation)
                );
    }

    private static void getItemNotFound(MockMvc mockMvc, List<String> parts, Map<String, Object> idMap, String authorisation) throws Exception {
        getItem(mockMvc, parts, idMap, authorisation)
            .andExpect(status().isNotFound());
    }

    private static Cart getItemOk(MockMvc mockMvc, List<String> parts, Map<String, Object> idMap, String authorisation, Cart cart, List<CartField> excludes, List<UserField> userExcludes) throws Exception {
        AtomicReference<Cart> result = new AtomicReference<>();
        expectCart(
                    getItem(mockMvc, parts, idMap, authorisation)
                            .andExpect(status().isOk()), excludes, userExcludes,
                cart, result);
        return result.get();
    }

    public static String getCartUri(List<String> parts, Map<String, Object> replaceMap) {
        List<Pair<String, Object>> replacements;
        if (replaceMap != null) {
            replacements = replaceMap.entrySet().stream()
                    .map(e -> Pair.of(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        } else {
            replacements = List.of();
        }
        return getUrl(CART_URL, parts, Map.of(), replacements);
    }

}