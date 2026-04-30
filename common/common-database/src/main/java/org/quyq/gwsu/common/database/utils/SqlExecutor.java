/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.quyq.gwsu.common.database.utils;

import org.quyq.gwsu.common.database.enums.DatabaseType;
import org.springframework.util.StringUtils;

import java.sql.*;
import java.util.List;

/**
 * Responsible for executing SQL and returning structured results.
 */
public class SqlExecutor {

    public static final Integer RESULT_SET_LIMIT = 1000;

    public static final Integer STATEMENT_TIMEOUT = 30;


    /**
     * Execute SQL query and return string two-dimensional array format result
     *
     * @param connection database connection
     * @param sql        SQL statement
     * @return two-dimensional array result
     * @throws SQLException SQL execution exception
     */
    public static String[][] executeSqlAndReturnArr(Connection connection, String sql) throws SQLException {
        List<String[]> list = executeQuery(connection, sql);
        return list.toArray(new String[0][]);
    }

    public static String[][] executeSqlAndReturnArr(Connection connection, String databaseOrSchema, String sql)
            throws SQLException {
        List<String[]> list = executeQuery(connection, databaseOrSchema, sql);
        return list.toArray(new String[0][]);
    }

    private static List<String[]> executeQuery(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {

            return ResultSetConverter.convert(rs);
        }
    }

    private static List<String[]> executeQuery(Connection connection, String databaseOrSchema, String sql)
            throws SQLException {
        String originalDb = connection.getCatalog();
        DatabaseMetaData metaData = connection.getMetaData();
        String dialect = metaData.getDatabaseProductName();

        try (Statement statement = connection.createStatement()) {

            if (dialect.equalsIgnoreCase(DatabaseType.MYSQL.getCode())) {
                if (StringUtils.hasText(databaseOrSchema)) {
                    statement.execute("use `" + databaseOrSchema + "`;");
                }
            } else if (dialect.equalsIgnoreCase(DatabaseType.POSTGRESQL.getCode())) {
                if (StringUtils.hasText(databaseOrSchema)) {
                    statement.execute("set search_path = '" + databaseOrSchema + "';");
                }
            }

            ResultSet rs = statement.executeQuery(sql);

            List<String[]> result = ResultSetConverter.convert(rs);

            if (StringUtils.hasText(databaseOrSchema) && dialect.equalsIgnoreCase(DatabaseType.MYSQL.getCode())) {
                statement.execute("use `" + originalDb + "`;");
            }

            return result;
        }
    }

}
