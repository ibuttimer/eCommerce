package com.example.demo.model.requests;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LoginUserRequest {

	@JsonProperty
	private String username;

	@JsonProperty
	private String password;

	public LoginUserRequest() {
	}

	public LoginUserRequest(String username, String password) {
		this.username = username;
		this.password = password;
	}

	public static LoginUserRequest of(String username, String password) {
		return new LoginUserRequest(username, password);
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
