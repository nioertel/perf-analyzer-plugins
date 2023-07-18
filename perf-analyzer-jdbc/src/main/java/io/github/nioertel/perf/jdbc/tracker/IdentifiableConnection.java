package io.github.nioertel.perf.jdbc.tracker;

import java.sql.Connection;

import io.github.nioertel.perf.utils.Identifiable;

public interface IdentifiableConnection extends Connection, Identifiable {

}
