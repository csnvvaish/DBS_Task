import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class OracleMetadataDiagnostic {
    private static final String URL = "jdbc:oracle:thin:@localhost:1521/XEPDB1";

    public static void main(String[] args) throws Exception {
        String username = System.getenv("DB_USERNAME");
        String password = System.getenv("ORACLE_DIAG_PASSWORD");
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("DB_USERNAME must be set for the application datasource account.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("ORACLE_DIAG_PASSWORD must be set before running this diagnostic.");
        }

        try (Connection connection = DriverManager.getConnection(URL, username, password)) {
            printQuery(connection, "SELECT USER AS CURRENT_USER FROM DUAL");
            printQuery(connection, "SELECT SYS_CONTEXT('USERENV','CURRENT_SCHEMA') AS CURRENT_SCHEMA FROM DUAL");
            printQuery(connection, "SELECT OWNER, TABLE_NAME, COLUMN_ID, COLUMN_NAME, DATA_TYPE " +
                    "FROM ALL_TAB_COLUMNS WHERE TABLE_NAME = 'USERS' ORDER BY OWNER, COLUMN_ID");
            printQuery(connection, "SELECT OWNER, TABLE_NAME, COLUMN_ID, COLUMN_NAME, DATA_TYPE " +
                    "FROM ALL_TAB_COLUMNS WHERE OWNER = 'SYSTEM' AND TABLE_NAME = 'USERS' ORDER BY COLUMN_ID");
                printQuery(connection, "SELECT OWNER, TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE, " +
                    "STATUS, VALIDATED, DELETE_RULE, R_OWNER, R_CONSTRAINT_NAME " +
                    "FROM ALL_CONSTRAINTS WHERE OWNER IN ('SYSTEM','AUTHUSER') " +
                    "AND TABLE_NAME = 'USERS' ORDER BY OWNER, CONSTRAINT_TYPE, CONSTRAINT_NAME");
                printQuery(connection, "SELECT FK.OWNER AS FK_OWNER, FK.TABLE_NAME AS FK_TABLE_NAME, " +
                    "FK.CONSTRAINT_NAME AS FK_CONSTRAINT_NAME, FK.STATUS, FK.VALIDATED, FK.DELETE_RULE, " +
                    "REF.OWNER AS REFERENCED_OWNER, REF.TABLE_NAME AS REFERENCED_TABLE, " +
                    "REF.CONSTRAINT_NAME AS REFERENCED_CONSTRAINT " +
                    "FROM ALL_CONSTRAINTS FK JOIN ALL_CONSTRAINTS REF " +
                    "ON REF.OWNER = FK.R_OWNER AND REF.CONSTRAINT_NAME = FK.R_CONSTRAINT_NAME " +
                    "WHERE FK.CONSTRAINT_TYPE = 'R' AND REF.OWNER IN ('SYSTEM','AUTHUSER') " +
                    "AND REF.TABLE_NAME = 'USERS' " +
                    "ORDER BY REF.OWNER, FK.OWNER, FK.TABLE_NAME, FK.CONSTRAINT_NAME");
                printQuery(connection, "SELECT CC.OWNER, CC.TABLE_NAME, CC.CONSTRAINT_NAME, " +
                    "CC.COLUMN_NAME, CC.POSITION " +
                    "FROM ALL_CONS_COLUMNS CC " +
                    "WHERE (CC.OWNER IN ('SYSTEM','AUTHUSER') AND CC.TABLE_NAME = 'USERS') " +
                    "OR EXISTS (SELECT 1 FROM ALL_CONSTRAINTS FK " +
                    "JOIN ALL_CONSTRAINTS REF ON REF.OWNER = FK.R_OWNER " +
                    "AND REF.CONSTRAINT_NAME = FK.R_CONSTRAINT_NAME " +
                    "WHERE FK.OWNER = CC.OWNER AND FK.CONSTRAINT_NAME = CC.CONSTRAINT_NAME " +
                    "AND FK.CONSTRAINT_TYPE = 'R' AND REF.OWNER IN ('SYSTEM','AUTHUSER') " +
                    "AND REF.TABLE_NAME = 'USERS') " +
                    "ORDER BY CC.OWNER, CC.TABLE_NAME, CC.CONSTRAINT_NAME, CC.POSITION");
                printQuery(connection, "SELECT I.TABLE_OWNER, I.TABLE_NAME, I.OWNER AS INDEX_OWNER, " +
                    "I.INDEX_NAME, I.UNIQUENESS, I.STATUS, C.COLUMN_POSITION, C.COLUMN_NAME " +
                    "FROM ALL_INDEXES I LEFT JOIN ALL_IND_COLUMNS C " +
                    "ON C.INDEX_OWNER = I.OWNER AND C.INDEX_NAME = I.INDEX_NAME " +
                    "WHERE I.TABLE_OWNER IN ('SYSTEM','AUTHUSER') AND I.TABLE_NAME = 'USERS' " +
                    "ORDER BY I.TABLE_OWNER, I.OWNER, I.INDEX_NAME, C.COLUMN_POSITION");
                printQuery(connection, "SELECT 'SYSTEM.USERS' AS OBJECT_NAME, COUNT(*) AS ROW_COUNT " +
                    "FROM SYSTEM.USERS UNION ALL " +
                    "SELECT 'AUTHUSER.USERS' AS OBJECT_NAME, COUNT(*) AS ROW_COUNT FROM AUTHUSER.USERS");

            DatabaseMetaData metadata = connection.getMetaData();
            System.out.println("\nJDBC URL: " + metadata.getURL());
            System.out.println("JDBC user: " + metadata.getUserName());
            System.out.println("JDBC schema: " + connection.getSchema());
            System.out.println("Stores uppercase identifiers: " + metadata.storesUpperCaseIdentifiers());
            printColumns(metadata, "SYSTEM", "USERS");
            printColumns(metadata, null, "USERS");
            printColumns(metadata, "SYSTEM", "users");
        }
    }

    private static void printQuery(Connection connection, String sql) throws Exception {
        System.out.println("\nSQL: " + sql);
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            int count = result.getMetaData().getColumnCount();
            while (result.next()) {
                for (int index = 1; index <= count; index++) {
                    if (index > 1) System.out.print(" | ");
                    System.out.print(result.getMetaData().getColumnLabel(index) + "=" + result.getString(index));
                }
                System.out.println();
            }
        }
    }

    private static void printColumns(DatabaseMetaData metadata, String schema, String table) throws Exception {
        System.out.println("\nJDBC getColumns schema=" + schema + ", tablePattern=" + table);
        try (ResultSet columns = metadata.getColumns(null, schema, table, null)) {
            while (columns.next()) {
                System.out.printf("TABLE_SCHEM=%s TABLE_NAME=%s COLUMN_NAME=%s TYPE_NAME=%s DATA_TYPE=%s ORDINAL_POSITION=%s IS_AUTOINCREMENT=%s%n",
                        columns.getString("TABLE_SCHEM"), columns.getString("TABLE_NAME"),
                        columns.getString("COLUMN_NAME"), columns.getString("TYPE_NAME"),
                        columns.getString("DATA_TYPE"), columns.getString("ORDINAL_POSITION"),
                        columns.getString("IS_AUTOINCREMENT"));
            }
        }
    }
}