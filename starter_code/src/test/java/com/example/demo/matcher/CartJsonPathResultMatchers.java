package com.example.demo.matcher;

import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CartJsonPathResultMatchers extends AbstractJsonPathResultMatchers<CartField, Cart> {

    private final List<UserField> userExcludes;

    public CartJsonPathResultMatchers(List<CartField> cartExcludes, List<UserField> userExcludes, String expression, Object... args) {
        super(cartExcludes, Cart.class, expression, args);
        this.userExcludes = userExcludes;
    }

    public static CartJsonPathResultMatchers cartJsonPath(List<CartField> cartExclude, List<UserField> userExcludes, String expression, Object... args) {
        return new CartJsonPathResultMatchers(cartExclude, userExcludes, expression, args);
    }

    public static CartJsonPathResultMatchers cartJsonPath(String expression, Object... args) {
        return cartJsonPath(List.of(), List.of(), expression, args);
    }

    @Override
    protected TypeReference<Cart> getTypeReference() {
        return new DtoTypeReference();
    }

    @Override
    protected TypeReference<List<Cart>> getListTypeReference() {
        return new ListDtoTypeReference();
    }

    private static class DtoTypeReference extends TypeReference<Cart> {
        private DtoTypeReference() {
        }
    }

    private static class ListDtoTypeReference extends TypeReference<List<Cart>> {
        private ListDtoTypeReference() {
        }
    }

    @Override
    protected void assertDto(Cart expected, Cart actual) {
        for (CartField field : CartField.values()) {
            if (!excludes.contains(field)) {
                String errorMsg = field.name() + " does not satisfy criteria";
                switch (field) {
                    case ID:
                        assertEquals(expected.getId(), actual.getId(), errorMsg);
                        break;
                    case ITEMS:
                        assertArrayEquals(
                                expected.getItems().toArray(Item[]::new), actual.getItems().toArray(Item[]::new), errorMsg);
                        break;
                    case USER:
                        assertUser(expected.getUser(), actual.getUser());
                        break;
                    case TOTAL:
                        assertEquals(expected.getTotal(), actual.getTotal(), errorMsg);
                        break;
                }
            }
        }
    }

    protected void assertUser(User expected, User actual) {
        for (UserField field : UserField.values()) {
            if (!userExcludes.contains(field)) {
                String errorMsg = "User::" + field.name() + " does not satisfy criteria";
                switch (field) {
                    case ID:
                        assertEquals(expected.getId(), actual.getId(), errorMsg);
                        break;
                    case USERNAME:
                        assertEquals(expected.getUsername(), actual.getUsername(), errorMsg);
                        break;
                    case CART:
                        if (excludes.contains(CartField.USER)) {
                            assertDto(expected.getCart(), actual.getCart());
                        }
                        break;
                    case PASSWORD:
                        assertEquals(expected.getPassword(), actual.getPassword(), errorMsg);
                        break;
                }
            }
        }
    }
}
