package com.example.demo.controllers;

import com.example.demo.misc.LogUtils;
import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.ModifyCartRequest;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.stream.IntStream;

import static com.example.demo.config.Config.*;
import static com.example.demo.misc.LogUtils.logSuccess;
import static com.example.demo.misc.LogUtils.logSuccessFailure;

@RestController
@RequestMapping(CART_URL)
public class CartController {

	private final Logger logger = LogUtils.getLogger(CartController.class);
	private final Logger securityLogger = LogUtils.getLogger(CartController.class, LogUtils.SECURITY);

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private CartRepository cartRepository;
	
	@Autowired
	private ItemRepository itemRepository;
	
	@PostMapping(CART_ADD_TO_URL)
	public ResponseEntity<Cart> addToCart(@RequestBody ModifyCartRequest request) {
		return modifyCart(request, Operation.ADD);
	}
	
	@PostMapping(CART_REMOVE_FROM_URL)
	public ResponseEntity<Cart> removeFromCart(@RequestBody ModifyCartRequest request) {
		return modifyCart(request, Operation.REMOVE);
	}

	private enum Operation {ADD, REMOVE};

	private ResponseEntity<Cart> modifyCart(ModifyCartRequest request, Operation operation) {
		ResponseEntity<Cart> response;
		boolean success = false;

		User user = userRepository.findByUsername(request.getUsername());
		if(user == null) {
			securityLogger.warn("Modify cart, user not found: " + request.getUsername());
			response = ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		} else {
			Optional<Item> item = itemRepository.findById(request.getItemId());
			if (item.isEmpty()) {
				logger.warn("Modify cart, Item not found: id " + request.getItemId());
				response = ResponseEntity.status(HttpStatus.NOT_FOUND).build();
			} else {
				Cart cart = user.getCart();
				IntStream.range(0, request.getQuantity())
						.forEach(i -> {
							if (operation == Operation.ADD) {
								cart.addItem(item.get());
							} else {
								cart.removeItem(item.get());
							}
						});
				response = ResponseEntity.ok(
								cartRepository.save(cart));
				success = true;
				logger.info("Cart updated: " + operation.name() + ", num_items=" + request.getQuantity());
			}
		}
		logger.trace(
				logSuccessFailure(success, "ModifyCart::" + operation.name().toLowerCase()));

		return response;
	}
}
