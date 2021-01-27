package com.example.demo.matcher;

import com.example.demo.model.persistence.Item;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ItemJsonPathResultMatchers extends AbstractJsonPathResultMatchers<ItemField, Item> {

    public ItemJsonPathResultMatchers(List<ItemField> excludes, String expression, Object... args) {
        super(excludes, Item.class, expression, args);
    }

    public static ItemJsonPathResultMatchers itemJsonPath(List<ItemField> excludes, String expression, Object... args) {
        return new ItemJsonPathResultMatchers(excludes, expression, args);
    }

    public static ItemJsonPathResultMatchers itemJsonPath(String expression, Object... args) {
        return itemJsonPath(List.of(), expression, args);
    }

    @Override
    protected TypeReference<Item> getTypeReference() {
        return new DtoTypeReference();
    }

    @Override
    protected TypeReference<List<Item>> getListTypeReference() {
        return new ListDtoTypeReference();
    }

    private static class DtoTypeReference extends TypeReference<Item> {
        private DtoTypeReference() {
        }
    }

    private static class ListDtoTypeReference extends TypeReference<List<Item>> {
        private ListDtoTypeReference() {
        }
    }

    @Override
    protected void assertDto(Item expected, Item actual) {
        for (ItemField field : ItemField.values()) {
            if (!excludes.contains(field)) {
                String errorMsg = field.name() + " does not satisfy criteria";
                switch (field) {
                    case ID:
                        assertEquals(expected.getId(), actual.getId(), errorMsg);
                        break;
                    case NAME:
                        assertEquals(expected.getName(), actual.getName(), errorMsg);
                        break;
                    case PRICE:
                        assertEquals(expected.getPrice(), actual.getPrice(), errorMsg);
                        break;
                    case DESCRIPTION:
                        assertEquals(expected.getDescription(), actual.getDescription(), errorMsg);
                        break;
                }
            }
        }
    }
}
