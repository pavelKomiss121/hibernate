package ru.mentee.power.config;

import lombok.Data;

/**
 * Конфигурация параметров подключения к БД.
 */
@Data
public class DatabaseConfig {
    private String jdbcUrl;
    private String username;
    private String password;
    private String driverClassName = "org.postgresql.Driver";
    private String hbm2ddlAuto = "update";
    private boolean showSql = false;
}
