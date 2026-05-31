/**
 * Orders module - Manages order lifecycle and orchestrates the order fulfillment saga.
 * 
 * <p>Published Events:</p>
 * <ul>
 *   <li>{@link com.showcase.ordersystem.shared.OrderCreatedEvent} - When a new order is placed</li>
 *   <li>{@link com.showcase.ordersystem.shared.OrderCompletedEvent} - When order is successfully completed</li>
 * </ul>
 * 
 * <p>Consumed Events:</p>
 * <ul>
 *   <li>{@link com.showcase.ordersystem.shared.InventoryReservedEvent} - From inventory module</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule
package com.showcase.ordersystem.orders;