package com.example.demo.controllers;

import com.example.demo.misc.LogUtils;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.repositories.ItemRepository;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.example.demo.config.Config.*;
import static com.example.demo.misc.LogUtils.logFailure;
import static com.example.demo.misc.LogUtils.logSuccessFailure;

@RestController
@RequestMapping(ITEM_URL)
public class ItemController {

	private final Logger logger = LogUtils.getLogger(ItemController.class);

	@Autowired
	private ItemRepository itemRepository;
	
	@GetMapping
	public ResponseEntity<List<Item>> getItems() {
		List<Item> items = itemRepository.findAll();
		logger.trace(
				logSuccessFailure(true, "GetAllItems"));
		return ResponseEntity.ok(items);
	}
	
	@GetMapping(ITEM_GET_BY_ID_URL)
	public ResponseEntity<Item> getItemById(@PathVariable Long id) {
		ResponseEntity<Item> response;
		boolean success = false;
		AtomicReference<Item> item = new AtomicReference<>();

		itemRepository.findById(id).ifPresent(item::set);
		if (item.get() == null) {
			logger.warn("Item not found: id " + id);
			response = ResponseEntity.notFound().build();
		} else {
			response = ResponseEntity.ok(item.get());
			success = true;
		}
		logger.trace(
				logSuccessFailure(success, "Item::id"));
		return response;
	}
	
	@GetMapping(ITEM_GET_BY_NAME_URL)
	public ResponseEntity<List<Item>> getItemsByName(@PathVariable String name) {
		ResponseEntity<List<Item>> response;
		boolean success = false;
		List<Item> items = itemRepository.findByName(name);
		if (items == null) {
			logger.warn(
					logFailure("Item(s) not found: name '" + name + "'"));
			response = ResponseEntity.notFound().build();
		} else {
			response = ResponseEntity.ok(items);
			success = true;
		}
		logger.trace(
				logSuccessFailure(success, "Item::name"));
		return response;
	}
}
