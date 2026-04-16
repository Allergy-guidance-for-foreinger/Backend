package com.mealguide.mealguide_api.mealcrawl.infrastructure.persistence.adapter;

import com.mealguide.mealguide_api.global.base.exception.ErrorCode;
import com.mealguide.mealguide_api.global.base.exception.ServiceException;
import com.mealguide.mealguide_api.mealcrawl.application.port.MealCrawlSchedulerLockPort;
import com.mealguide.mealguide_api.mealcrawl.infrastructure.config.MealCrawlProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
@Component
@RequiredArgsConstructor
public class MealCrawlSchedulerLockAdapter implements MealCrawlSchedulerLockPort {

    private static final String TRY_LOCK_SQL = "select pg_try_advisory_lock(?)";
    private static final String UNLOCK_SQL = "select pg_advisory_unlock(?)";

    private final DataSource dataSource;
    private final MealCrawlProperties mealCrawlProperties;
    private final ThreadLocal<Connection> lockConnectionHolder = new ThreadLocal<>();

    @Override
    public boolean tryAcquireLock() {
        if (lockConnectionHolder.get() != null) {
            return true;
        }

        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            try (PreparedStatement statement = connection.prepareStatement(TRY_LOCK_SQL)) {
                statement.setLong(1, mealCrawlProperties.getSchedulerLockKey());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next() && resultSet.getBoolean(1)) {
                        lockConnectionHolder.set(connection);
                        return true;
                    }
                }
            }
        } catch (SQLException exception) {
            closeQuietly(connection);
            throw new ServiceException(ErrorCode.UNEXPECTED_SERVER_ERROR, exception);
        }

        closeQuietly(connection);
        return false;
    }

    @Override
    public void releaseLock() {
        Connection connection = lockConnectionHolder.get();
        if (connection == null) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(UNLOCK_SQL)) {
            statement.setLong(1, mealCrawlProperties.getSchedulerLockKey());
            statement.executeQuery();
        } catch (SQLException exception) {
            log.warn("Failed to release meal crawl scheduler advisory lock", exception);
        } finally {
            closeQuietly(connection);
            lockConnectionHolder.remove();
        }
    }

    private void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException ignore) {
            log.debug("Failed to close scheduler lock connection", ignore);
        }
    }
}
