/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.modelgenerator.commons;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Manages SQLite database operations for searching functions in a package repository.
 *
 * <p>
 * This class follows the Singleton pattern and handles the initialization and querying of a SQLite database containing
 * package and function information.
 * </p>
 *
 * @since 2.0.0
 */
public class SearchDatabaseManager {

    private static final String INDEX_FILE_NAME = "search-index.sqlite";
    private static final Logger LOGGER = Logger.getLogger(SearchDatabaseManager.class.getName());
    private final String dbPath;

    private static class Holder {

        private static final SearchDatabaseManager INSTANCE = new SearchDatabaseManager();
    }

    public static SearchDatabaseManager getInstance() {
        return Holder.INSTANCE;
    }

    private SearchDatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load SQLite JDBC driver", e);
        }

        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("central-index");
        } catch (IOException e) {
            throw new RuntimeException("Failed to create a temporary directory", e);
        }

        URL dbUrl = getClass().getClassLoader().getResource(INDEX_FILE_NAME);
        if (dbUrl == null) {
            throw new RuntimeException("Database resource not found: " + INDEX_FILE_NAME);
        }
        Path tempFile = tempDir.resolve(INDEX_FILE_NAME);
        try {
            Files.copy(dbUrl.openStream(), tempFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy the database file to the temporary directory", e);
        }

        dbPath = "jdbc:sqlite:" + tempFile;
    }

    public List<SearchResult> searchFunctions(Map<String, String> queryMap) {
        List<SearchResult> results = new ArrayList<>();
        String sql = """
                SELECT
                    f.id,
                    f.name AS function_name,
                    f.description AS function_description,
                    f.package_id,
                    p.name AS package_name, 
                    p.org AS package_org,
                    p.version AS package_version,
                    fts.rank
                FROM FunctionFTS AS fts
                JOIN Function AS f ON fts.rowid = f.id
                JOIN Package AS p ON f.package_id = p.id
                WHERE fts.FunctionFTS MATCH ?
                ORDER BY fts.rank
                LIMIT ? 
                OFFSET ?;
                """;

        try (Connection conn = DriverManager.getConnection(dbPath);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sanitizeQuery(queryMap.get("q")) + "*");
            stmt.setInt(2, Integer.parseInt(queryMap.get("limit")));
            stmt.setInt(3, Integer.parseInt(queryMap.get("offset")));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String functionName = rs.getString("function_name");
                    String description = rs.getString("function_description");
                    String packageName = rs.getString("package_name");
                    String org = rs.getString("package_org");
                    String version = rs.getString("package_version");
                    SearchResult result = SearchResult.from(org, packageName, version, functionName, description);
                    results.add(result);
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("Error searching functions: " + e.getMessage());
            throw new RuntimeException("Failed to search functions", e);
        } catch (NumberFormatException e) {
            LOGGER.severe("Invalid number format in query parameters: " + e.getMessage());
            throw new RuntimeException("Invalid limit or offset value", e);
        }

        return results;
    }

    private static String sanitizeQuery(String q) {
        if (q == null || q.trim().isEmpty()) {
            return "";
        }
        // Escape quotes and remove special SQLite FTS operators, and only allow alphanumeric characters and spaces
        return "\"" + q.replace("\"", "\"\"")
                .replaceAll("(?i)(UNION|SELECT|FROM|OR|AND|WHERE|MATCH|NEAR|NOT)|[^a-zA-Z0-9\\s\"]", " ")
                .trim() + "\"";
    }

}