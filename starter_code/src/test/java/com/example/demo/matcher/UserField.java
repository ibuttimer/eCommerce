package com.example.demo.matcher;

import java.util.List;

public enum UserField {
    ID, USERNAME, CART, PASSWORD;

    public static List<UserField> CHECK_ID_USERNAME = List.of(CART, PASSWORD);
}


