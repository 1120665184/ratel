package org.quyq.gwsu.common.database.enums;

import cn.hutool.core.util.ArrayUtil;
import com.baomidou.mybatisplus.annotation.DbType;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
public enum DatabaseType {

    POSTGRESQL("postgresql", "PostgreSQL", DbType.POSTGRE_SQL, "org.postgresql.Driver"),
    MYSQL("mysql", "MySQL", DbType.MYSQL, "com.mysql.cj.jdbc.Driver"),
    ORACLE("oracle", "Oracle", DbType.ORACLE, "oracle.jdbc.OracleDriver"),
    SQL_SERVER("sqlserver", "SQL Server", DbType.SQL_SERVER, "com.microsoft.sqlserver.jdbc.SQLServerDriver"),
    H2("h2", "H2", DbType.H2, "org.h2.Driver"),
    DB2("db2", "DB2", DbType.DB2, "com.ibm.db2.jcc.DB2Driver"),
    SQLITE("sqlite", "SQLite", DbType.SQLITE, "org.sqlite.JDBC"),
    DM("dm", "达梦数据库", DbType.DM, "dm.jdbc.driver.DmDriver"),
    KINGBASE("kingbase", "人大金仓", DbType.KINGBASE_ES, "com.kingbase8.Driver");

    private final String code;
    private final String name;
    private final DbType dbType;
    private final List<String> driverClassName;

    DatabaseType(String code, String name,
                 @Nullable
                 DbType dbType, String... driverClassName) {
        this.code = code;
        this.name = name;
        this.dbType = dbType;
        this.driverClassName = ArrayUtil.isEmpty(driverClassName) ? Collections.emptyList() : Arrays.asList(driverClassName);
    }

    public static DatabaseType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (DatabaseType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }

    public static DatabaseType fromDriverClassName(String driverClassName) {
        if (driverClassName == null) {
            return null;
        }
        for (DatabaseType type : values()) {
            if (type.driverClassName.contains(driverClassName)) {
                return type;
            }
        }
        return null;
    }

    public static DatabaseType fromJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null) {
            return null;
        }
        String lowerUrl = jdbcUrl.toLowerCase();
        for (DatabaseType type : values()) {
            if (lowerUrl.contains(":" + type.code + ":")) {
                return type;
            }
        }
        return null;
    }
}
