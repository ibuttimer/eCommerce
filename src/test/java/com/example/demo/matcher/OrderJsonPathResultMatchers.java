package com.example.demo.matcher;

import com.example.demo.model.persistence.UserOrder;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OrderJsonPathResultMatchers extends AbstractJsonPathResultMatchers<OrderField, UserOrder> {

    private List<UserField> userExcludes;

    public OrderJsonPathResultMatchers(List<OrderField> orderExcludes, List<UserField> userExcludes, String expression, Object... args) {
        super(orderExcludes, UserOrder.class, expression, args);
        this.userExcludes = userExcludes;
    }

    public static OrderJsonPathResultMatchers orderJsonPath(List<OrderField> cartExclude, List<UserField> userExcludes, String expression, Object... args) {
        return new OrderJsonPathResultMatchers(cartExclude, userExcludes, expression, args);
    }

    public static OrderJsonPathResultMatchers orderJsonPath(String expression, Object... args) {
        return orderJsonPath(List.of(), List.of(), expression, args);
    }

    @Override
    protected TypeReference<UserOrder> getTypeReference() {
        return new DtoTypeReference();
    }

    @Override
    protected TypeReference<List<UserOrder>> getListTypeReference() {
        return new ListDtoTypeReference();
    }

    private static class DtoTypeReference extends TypeReference<UserOrder> {
        private DtoTypeReference() {
        }
    }

    private static class ListDtoTypeReference extends TypeReference<List<UserOrder>> {
        private ListDtoTypeReference() {
        }
    }

    @Override
    protected void assertDto(UserOrder expected, UserOrder actual) {
        for (OrderField field : OrderField.values()) {
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
                    case PASSWORD:
                        assertEquals(expected.getPassword(), actual.getPassword(), errorMsg);
                        break;
                }
            }
        }
    }
}
