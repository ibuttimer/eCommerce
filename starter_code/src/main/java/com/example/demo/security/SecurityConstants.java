package com.example.demo.security;

import static com.example.demo.config.Config.USER_CREATE_URL;
import static com.example.demo.config.Config.USER_URL;

public class SecurityConstants {

	public static final String SECRET = "oursecretkey";
    public static final long EXPIRATION_TIME = 864_000_000; // 10 days
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";
    public static final String SIGN_UP_URL = USER_URL + USER_CREATE_URL;
}
