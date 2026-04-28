-- Runs automatically when the MySQL container starts for the first time.
-- Creates all four service databases if they don't already exist.

CREATE DATABASE IF NOT EXISTS userdb    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS productdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS orderdb   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS paymentdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant the app user full access to all service databases
GRANT ALL PRIVILEGES ON userdb.*    TO 'ecommerce'@'%';
GRANT ALL PRIVILEGES ON productdb.* TO 'ecommerce'@'%';
GRANT ALL PRIVILEGES ON orderdb.*   TO 'ecommerce'@'%';
GRANT ALL PRIVILEGES ON paymentdb.* TO 'ecommerce'@'%';
FLUSH PRIVILEGES;