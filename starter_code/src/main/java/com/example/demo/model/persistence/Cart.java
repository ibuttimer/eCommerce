package com.example.demo.model.persistence;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cart")
public class Cart {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@JsonProperty
	@Column
	private Long id;
	
	@ManyToMany
	@JsonProperty
	@Column
    private List<Item> items;
	
	@OneToOne(mappedBy = "cart")
	@JsonProperty
    private User user;
	
	@Column
	@JsonProperty
	private BigDecimal total;

	public Cart() {
	}

	public Cart(Long id, List<Item> items, User user, BigDecimal total) {
		this.id = id;
		this.items = items;
		this.user = user;
		this.total = total;
	}

	public static Cart of(List<Item> items, User user, BigDecimal total) {
		return new Cart(0L, items, user, total);
	}

	public static Cart of(User user) {
		return new Cart(0L, new ArrayList<>(), user, new BigDecimal(0));
	}

	public BigDecimal getTotal() {
		return total;
	}

	public void setTotal(BigDecimal total) {
		this.total = total;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public List<Item> getItems() {
		return items;
	}

	public void setItems(List<Item> items) {
		this.items = items;
	}
	
	public void addItem(Item item) {
		addItem(item, 1);
	}
	
	public void addItem(Item item, int count) {
		nullCheck();
		while (count > 0) {
			items.add(item);
			total = total.add(item.getPrice());
			--count;
		}
	}

	private void nullCheck() {
		if(items == null) {
			items = new ArrayList<>();
		}
		if(total == null) {
			total = new BigDecimal(0);
		}
	}

	public void removeItem(Item item) {
		removeItem(item, 1);
	}

	public void removeItem(Item item, int count) {
		nullCheck();
		while (count > 0) {
			items.remove(item);
			total = total.subtract(item.getPrice());
			--count;
		}
	}

	public void empty() {
		items.clear();
		total = new BigDecimal(0);
	}
}
