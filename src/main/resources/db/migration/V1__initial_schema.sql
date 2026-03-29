-- V1__initial_schema.sql

-- Orders Module Tables
CREATE TABLE orders (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(100) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE order_items (
    id VARCHAR(36) PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL REFERENCES orders(id),
    product_id VARCHAR(36) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_price DECIMAL(10,2) NOT NULL
);

CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);

-- Inventory Module Tables
CREATE TABLE inventory_items (
    product_id VARCHAR(36) PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    available_quantity INT NOT NULL,
    reserved_quantity INT NOT NULL,
    last_updated TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_inventory_items_product_id ON inventory_items(product_id);

-- Spring Modulith Event Store
CREATE TABLE event_publication (
    id UUID PRIMARY KEY,
    event_type VARCHAR(512) NOT NULL,
    listener_id VARCHAR(512) NOT NULL,
    publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
    serialized_event TEXT NOT NULL,
    completion_date TIMESTAMP WITH TIME ZONE
);