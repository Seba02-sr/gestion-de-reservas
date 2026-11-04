-- Script de referencia para crear las tablas en el esquema `users`.
-- Alineado con las entidades JPA de user-svc (incluye las columnas de AuditableEntity).
--
-- OJO: el docker-compose raíz NO monta este directorio en el contenedor de MySQL, por lo que el
-- esquema real lo crea Hibernate (spring.jpa.hibernate.ddl-auto=update). Este script es solo
-- referencia / para levantar la base a mano. En particular, el borrado en cascada de las tarjetas
-- de un huésped NO depende del ON DELETE CASCADE de abajo sino del cascade JPA de
-- Huesped.tarjetaCredito.
CREATE SCHEMA IF NOT EXISTS users;

CREATE TABLE users.bancos (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    activo BIT(1) NOT NULL DEFAULT 1,
    fecha_eliminado DATETIME(6),
    fecha_registro DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    fecha_modificacion DATETIME(6)
);

CREATE TABLE users.cuentas_bancarias (
    id INT AUTO_INCREMENT PRIMARY KEY,
    numero_cuenta VARCHAR(255) NOT NULL,
    cbu VARCHAR(255) NOT NULL,
    alias VARCHAR(255),
    banco_id INT,
    activo BIT(1) NOT NULL DEFAULT 1,
    fecha_eliminado DATETIME(6),
    fecha_registro DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    fecha_modificacion DATETIME(6),
    FOREIGN KEY (banco_id) REFERENCES users.bancos(id)
);

-- Tabla única para Huésped y Propietario (discriminador `tipo`)
CREATE TABLE users.usuarios (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    telefono VARCHAR(255) NOT NULL,
    dni VARCHAR(255),
    username VARCHAR(255),
    password VARCHAR(255),
    tipo VARCHAR(255) NOT NULL,
    fecha_nacimiento DATE,
    cuenta_bancaria_id INT,
    hotel_id INT,
    activo BIT(1) NOT NULL DEFAULT 1,
    fecha_eliminado DATETIME(6),
    fecha_registro DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    fecha_modificacion DATETIME(6),
    FOREIGN KEY (cuenta_bancaria_id) REFERENCES users.cuentas_bancarias(id)
);


CREATE TABLE users.tarjetas_credito (
    id INT AUTO_INCREMENT PRIMARY KEY,
    numero_tarjeta VARCHAR(22) NOT NULL,
    nombre_titular VARCHAR(255) NOT NULL,
    -- MM/AA (5) o MM/AAAA (7)
    fecha_vencimiento VARCHAR(7) NOT NULL,
    codigo_seguridad VARCHAR(4) NOT NULL,
    es_principal BIT(1),
    usuario_id INT NOT NULL,
    banco_id INT NOT NULL,
    activo BIT(1) NOT NULL DEFAULT 1,
    fecha_eliminado DATETIME(6),
    fecha_registro DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
    fecha_modificacion DATETIME(6),
    FOREIGN KEY (banco_id) REFERENCES users.bancos(id),
    FOREIGN KEY (usuario_id) REFERENCES users.usuarios(id) ON DELETE CASCADE
);
