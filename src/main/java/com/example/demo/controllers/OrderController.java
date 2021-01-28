package com.example.demo.controllers;

import com.example.demo.misc.LogUtils;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.UserOrder;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.OrderRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.demo.config.Config.*;
import static com.example.demo.misc.LogUtils.*;

@RestController
@RequestMapping(ORDER_URL)
public class OrderController {

	private final Logger logger = LogUtils.getLogger(OrderController.class);
	private final Logger securityLogger = LogUtils.getLogger(OrderController.class, LogUtils.SECURITY);

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private OrderRepository orderRepository;
	
	@Autowired
	private CartRepository cartRepository;


	@PostMapping(ORDER_SUBMIT_URL)
	public ResponseEntity<UserOrder> submit(@PathVariable String username) {
		ResponseEntity<UserOrder> response;
		boolean success = false;

		User user = userRepository.findByUsername(username);
		if(user == null) {
			securityLogger.warn(
					logFailure("Order submit, user not found: " + username));
			response = ResponseEntity.notFound().build();
		} else {
			UserOrder order = UserOrder.createFromCart(user.getCart());
			if (order.getItems().size() == 0) {
				logger.warn(
						logFailure("Cart empty for user: " + username));
				response = ResponseEntity.noContent().build();
			} else {
				order = orderRepository.save(order);
				logger.info(
						logSuccess("Order submitted for user: " + username +
								", num_items=" + order.getItems().size()) +
								", total=" + order.getTotal().doubleValue());

				// empty card for next order
				user.getCart().empty();
				cartRepository.save(user.getCart());

				response = ResponseEntity.ok(order);
				success = true;
			}
		}

		logger.trace(
				logSuccessFailure(success, "SubmitOrder::username"));

		return response;
	}
	
	@GetMapping(ORDER_HISTORY_URL)
	public ResponseEntity<List<UserOrder>> getOrdersForUser(@PathVariable String username) {
		User user = userRepository.findByUsername(username);
		ResponseEntity<List<UserOrder>> response;
		boolean success = false;

		if(user == null) {
			securityLogger.warn(
					logFailure("Order history, user not found: " + username));
			response = ResponseEntity.notFound().build();
		} else {
			response = ResponseEntity.ok(orderRepository.findByUser(user));
			success = true;
		}

		logger.trace(
				logSuccessFailure(success, "OrderHistory::username"));

		return response;
	}
}
