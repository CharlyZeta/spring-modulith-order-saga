/**
 * Inventory module - Manages product stock and reservations.
 * 
 * <p>Published Events:</p>
 * <ul>
 *   <li>{@link com.showcase.ordersystem.shared.events.InventoryReservedEvent} - Emitted indicating if a reservation succeeded or failed</li>
 * </ul>
 * 
 * <p>Consumed Events:</p>
 * <ul>
 *   <li>{@link com.showcase.ordersystem.shared.events.OrderCreatedEvent} - From orders module</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule
package com.showcase.ordersystem.inventory;