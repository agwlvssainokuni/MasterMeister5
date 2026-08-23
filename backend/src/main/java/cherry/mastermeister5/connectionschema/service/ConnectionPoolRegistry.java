/*
 * Copyright 2026 agwlvssainokuni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cherry.mastermeister5.connectionschema.service;

import cherry.mastermeister5.connectionschema.entity.RdbmsType;
import cherry.mastermeister5.connectionschema.entity.TargetConnection;
import cherry.mastermeister5.platform.security.ConnectionSecretCipher;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

/**
 * nfr-design-plan.md Question 8 / Question 5: one HikariCP pool per
 * connection, lazily created and cached. Question 5 (NFR Design): 5s
 * connectionTimeout, maximumPoolSize 5, minimumIdle 1, 10s leak detection.
 */
@Component
public class ConnectionPoolRegistry {

    private final ConnectionSecretCipher secretCipher;
    private final Map<Long, HikariDataSource> pools = new ConcurrentHashMap<>();

    public ConnectionPoolRegistry(ConnectionSecretCipher secretCipher) {
        this.secretCipher = secretCipher;
    }

    public DataSource dataSourceFor(TargetConnection connection) {
        return pools.computeIfAbsent(connection.getId(), id -> newDataSource(connection));
    }

    /** Not pooled: used for the one-off connection test at registration time. */
    public HikariDataSource transientDataSourceFor(
            RdbmsType rdbmsType,
            String host,
            int port,
            String databaseName,
            String extraParams,
            String username,
            String rawPassword) {
        var config = baseConfig(rdbmsType, host, port, databaseName, extraParams, username, rawPassword);
        config.setMaximumPoolSize(1);
        config.setMinimumIdle(0);
        return new HikariDataSource(config);
    }

    public void evict(Long connectionId) {
        var pool = pools.remove(connectionId);
        if (pool != null) {
            pool.close();
        }
    }

    private HikariDataSource newDataSource(TargetConnection connection) {
        var password = secretCipher.decrypt(connection.getEncryptedPassword());
        var config =
                baseConfig(
                        connection.getRdbmsType(),
                        connection.getHost(),
                        connection.getPort(),
                        connection.getDatabaseName(),
                        connection.getExtraParams(),
                        connection.getUsername(),
                        password);
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setLeakDetectionThreshold(10_000);
        return new HikariDataSource(config);
    }

    private HikariConfig baseConfig(
            RdbmsType rdbmsType,
            String host,
            int port,
            String databaseName,
            String extraParams,
            String username,
            String password) {
        var config = new HikariConfig();
        config.setJdbcUrl(buildJdbcUrl(rdbmsType, host, port, databaseName, extraParams));
        config.setUsername(username);
        config.setPassword(password);
        config.setConnectionTimeout(5_000);
        return config;
    }

    /** extraParams is appended verbatim (already includes its own leading "?"/";" separator). */
    private String buildJdbcUrl(RdbmsType rdbmsType, String host, int port, String databaseName, String extraParams) {
        var base =
                switch (rdbmsType) {
                    case MYSQL -> "jdbc:mysql://" + host + ":" + port + "/" + databaseName;
                    case MARIADB -> "jdbc:mariadb://" + host + ":" + port + "/" + databaseName;
                    case POSTGRESQL -> "jdbc:postgresql://" + host + ":" + port + "/" + databaseName;
                    case H2 -> "jdbc:h2:tcp://" + host + ":" + port + "/" + databaseName;
                };
        return extraParams == null || extraParams.isBlank() ? base : base + extraParams;
    }
}
