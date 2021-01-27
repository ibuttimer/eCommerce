package com.example.demo.controllers;

import com.example.demo.misc.LogUtils;
import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.CreateUserRequest;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.atomic.AtomicReference;

import static com.example.demo.config.Config.*;
import static com.example.demo.misc.LogUtils.*;

@RestController
@RequestMapping(USER_URL)
public class UserController {

	Logger securityLogger = LogUtils.getLogger(UserController.class, LogUtils.SECURITY);
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	@Value("${app.min.password.length}")
	int minPasswordLength;

	@GetMapping(USER_GET_BY_ID_URL)
	public ResponseEntity<User> findById(@PathVariable Long id) {
		AtomicReference<User> user = new AtomicReference<>();
		userRepository.findById(id).ifPresent(user::set);
		return userResult(user.get(), "id", id);
	}
	
	@GetMapping(USER_GET_BY_USERNAME_URL)
	public ResponseEntity<User> findByUserName(@PathVariable String username) {
		return userResult(
				userRepository.findByUsername(username), "username", username
		);
	}
	
	private ResponseEntity<User> userResult(User user, String method, Object param) {
		ResponseEntity<User> response;
		boolean success = (user != null);

		if (success) {
			response = ResponseEntity.ok(user);
		} else {
			response = ResponseEntity.notFound().build();
			securityLogger.warn(
					logFailure("User not found: " + method + "'" + param + "'"));
		}
		securityLogger.trace(
				logSuccessFailure(success, "GetUser::" + method));

		return response;
	}

	@PostMapping(USER_CREATE_URL)
	public ResponseEntity<User> createUser(@RequestBody CreateUserRequest createUserRequest) {
		ResponseEntity<User> response = null;
		boolean success = false;
		final String username = createUserRequest.getUsername();
		final String password = createUserRequest.getPassword();

		if (password.length() < minPasswordLength) {
			securityLogger.warn(
					logFailure("Password less than minimum length: " + password.length() + " < " + minPasswordLength));
		} else if (!password.equals(createUserRequest.getConfirmPassword())) {
			securityLogger.warn(
					logFailure("Password not confirmed"));
		} else if (userRepository.findByUsername(username) != null) {
			securityLogger.warn(
					logFailure("User already exists"));
		} else {
			User user = new User();
			user.setUsername(username);
			Cart cart = new Cart();
			cartRepository.save(cart);
			user.setCart(cart);

			user.setPassword(
					bCryptPasswordEncoder.encode(password));
			userRepository.save(user);
			response = ResponseEntity.ok(user);
			success = true;

			securityLogger.info(
					logSuccess("User created"));
		}

		if (!success) {
			response = ResponseEntity.badRequest().build();
		}
		securityLogger.trace(
			logSuccessFailure(success, "CreateUser"));
		return response;
	}
	
}
