package com.vinicius.prazos.persistence;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class EnumConstraintSynchronizerTest {

	@Mock
	private JdbcTemplate jdbcTemplate;

	@Mock
	private DataSource dataSource;

	@Mock
	private Connection connection;

	@Mock
	private DatabaseMetaData databaseMetaData;

	@Test
	void shouldSynchronizeEnumConstraintsForPostgreSql() throws Exception {
		EnumConstraintSynchronizer synchronizer = new EnumConstraintSynchronizer(jdbcTemplate, dataSource);

		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.getMetaData()).thenReturn(databaseMetaData);
		when(databaseMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");

		synchronizer.synchronizeEnumConstraints();

		verify(jdbcTemplate).execute("ALTER TABLE documents DROP CONSTRAINT IF EXISTS documents_status_check");
		verify(jdbcTemplate).execute(
			"ALTER TABLE documents ADD CONSTRAINT documents_status_check CHECK (status IN ('RASCUNHO', 'AGUARDANDO_ASSINATURA', 'PARCIALMENTE_ASSINADO', 'ASSINADO', 'VALIDADO', 'CANCELADO'))"
		);
		verify(jdbcTemplate).execute("ALTER TABLE signers DROP CONSTRAINT IF EXISTS signers_status_check");
		verify(jdbcTemplate).execute(
			"ALTER TABLE signers ADD CONSTRAINT signers_status_check CHECK (status IN ('PENDENTE', 'AGUARDANDO_ORDEM', 'VISUALIZADO', 'ASSINADO', 'EXPIRADO'))"
		);
		verify(jdbcTemplate).execute("ALTER TABLE signature_logs DROP CONSTRAINT IF EXISTS signature_logs_event_check");
		verify(jdbcTemplate).execute(
			"ALTER TABLE signature_logs ADD CONSTRAINT signature_logs_event_check CHECK (event IN ('SIGNATARIO_ADICIONADO', 'DOCUMENTO_ENVIADO', 'LINK_VISUALIZADO', 'ASSINATURA_ENVIADA', 'ASSINATURA_VALIDADA', 'LINK_EXPIRADO'))"
		);
		verify(connection).close();
	}

	@Test
	void shouldSkipSynchronizationForNonPostgreSql() throws Exception {
		EnumConstraintSynchronizer synchronizer = new EnumConstraintSynchronizer(jdbcTemplate, dataSource);

		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.getMetaData()).thenReturn(databaseMetaData);
		when(databaseMetaData.getDatabaseProductName()).thenReturn("H2");

		synchronizer.synchronizeEnumConstraints();

		verify(jdbcTemplate, never()).execute(org.mockito.ArgumentMatchers.anyString());
		verify(connection).close();
	}
}
