package ru.mentee.power.hybrid.impl;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Функциональный интерфейс для маппинга результатов JDBC запросов.
 */
@FunctionalInterface
public interface RowMapper<T> {
    T mapRow(ResultSet rs, int rowNum) throws SQLException;
}
