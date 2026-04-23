package com.vinicius.prazos.persistence;

import com.vinicius.prazos.documents.domain.enums.DocumentStatus;
import com.vinicius.prazos.signatures.domain.enums.SignatureLogEvent;
import com.vinicius.prazos.signatures.domain.enums.SignerStatus;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class EnumConstraintSynchronizer {

	private final JdbcTemplate jdbcTemplate;
	private final DataSource dataSource;

	public EnumConstraintSynchronizer(JdbcTemplate jdbcTemplate, DataSource dataSource) {
		this.jdbcTemplate = jdbcTemplate;
		this.dataSource = dataSource;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void synchronizeEnumConstraints() {
		if (!isPostgreSql()) {
			return;
		}

		synchronizeConstraint("documents", "status", "documents_status_check", enumValues(DocumentStatus.values()));
		synchronizeConstraint("signers", "status", "signers_status_check", enumValues(SignerStatus.values()));
		synchronizeConstraint("signature_logs", "event", "signature_logs_event_check", enumValues(SignatureLogEvent.values()));
	}

	private boolean isPostgreSql() {
		try (Connection connection = dataSource.getConnection()) {
			return "PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
		} catch (SQLException exception) {
			throw new IllegalStateException("Não foi possível identificar o banco de dados para sincronizar constraints de enum", exception);
		}
	}

	private void synchronizeConstraint(String tableName, String columnName, String constraintName, String allowedValues) {
		jdbcTemplate.execute("ALTER TABLE " + tableName + " DROP CONSTRAINT IF EXISTS " + constraintName);
		jdbcTemplate.execute(
			"ALTER TABLE " + tableName
				+ " ADD CONSTRAINT " + constraintName
				+ " CHECK (" + columnName + " IN (" + allowedValues + "))"
		);
	}

	private String enumValues(Enum<?>[] values) {
		return Arrays.stream(values)
			.map(Enum::name)
			.map(value -> "'" + value.replace("'", "''") + "'")
			.collect(Collectors.joining(", "));
	}
}
