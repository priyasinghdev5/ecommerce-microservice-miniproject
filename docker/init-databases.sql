-- This script runs automatically when PostgreSQL container first starts.
-- Creates all databases needed by every service upfront.

CREATE DATABASE user_db;
CREATE DATABASE inventory_db;
CREATE DATABASE order_db;
CREATE DATABASE payment_db;
CREATE DATABASE notification_db;
CREATE DATABASE batch_db;

-- auth_db already created by POSTGRES_DB env variable
-- Grant all permissions to ecom user
GRANT ALL PRIVILEGES ON DATABASE auth_db         TO ecom;
GRANT ALL PRIVILEGES ON DATABASE user_db         TO ecom;
GRANT ALL PRIVILEGES ON DATABASE inventory_db    TO ecom;
GRANT ALL PRIVILEGES ON DATABASE order_db        TO ecom;
GRANT ALL PRIVILEGES ON DATABASE payment_db      TO ecom;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO ecom;
GRANT ALL PRIVILEGES ON DATABASE batch_db        TO ecom;