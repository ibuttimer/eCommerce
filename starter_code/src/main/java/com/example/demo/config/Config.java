package com.example.demo.config;

import org.springframework.data.util.Pair;

import java.util.List;
import java.util.Map;

public class Config {

    private Config() {
        // non-instantiatable
    }

    public static final String LOGIN_URL = "/login";
    public static final String LOGOUT_URL = "/logout";
    public static final String AUTHORIZATION = "Authorization";


    public static final String PATH_ID = "id";
    public static final String PATH_NAME = "name";
    public static final String PATH_USERNAME = "username";

    public static final String PATH_ID_PATTERN = "{" + PATH_ID + "}";
    public static final String PATH_NAME_PATTERN = "{" + PATH_NAME + "}";
    public static final String PATH_USERNAME_PATTERN = "{" + PATH_USERNAME + "}";
    public static final String ID_URL = "/" + PATH_ID_PATTERN;
    public static final String NAME_URL = "/" + PATH_NAME_PATTERN;
    public static final String USERNAME_URL = "/" + PATH_USERNAME_PATTERN;

    public static final String API_URL = "/api";

    public static final String CART_URL = API_URL + "/cart";
    public static final String CART_ADD_TO_URL = "/addToCart";
    public static final String CART_REMOVE_FROM_URL = "/removeFromCart";

    public static final String ITEM_URL = API_URL + "/item";
    public static final String ITEM_GET_BY_ID_URL = ID_URL;
    public static final String ITEM_GET_BY_NAME_URL = "/name" + NAME_URL;

    public static final String ORDER_URL = API_URL + "/order";
    public static final String ORDER_SUBMIT_URL = "/submit" + USERNAME_URL;
    public static final String ORDER_HISTORY_URL = "/history" + USERNAME_URL;

    public static final String USER_URL = API_URL + "/user";
    public static final String USER_GET_BY_ID_URL = "/id" + ID_URL;
    public static final String USER_GET_BY_USERNAME_URL = USERNAME_URL;
    public static final String USER_CREATE_URL = "/create";

    public static final String H2_CONSOLE_PATTERN = "/h2-console/**";


    public static String getUrl(String url, Map<String, Object> query) {
        StringBuilder sb = new StringBuilder(
                url.endsWith("/") ? url.substring(0, url.length() - 1) : url);
        boolean start = true;
        for (Map.Entry<String, Object> entry : query.entrySet()) {
            if (start) {
                sb.append('?');
                start = false;
            } else {
                sb.append('&');
            }
            sb.append(entry.getKey())
                    .append("=")
                    .append(entry.getValue().toString());
        }
        return sb.toString();
    }

    public static String getUrl(String baseUrl, List<String> parts, Map<String, Object> query,
                                List<Pair<String, Object>> replacements) {
        String url = baseUrl + String.join("", parts);
        if (replacements != null) {
            for (Pair<String, Object> replacement : replacements) {
                if (url.contains(replacement.getFirst())) {
                    url = url.replace(replacement.getFirst(), replacement.getSecond().toString());
                }
            }
        }
        return getUrl(url, query);
    }

    public static String getUrl(String baseUrl, List<String> parts) {
        return getUrl(baseUrl, parts, Map.of(), List.of());
    }
}
