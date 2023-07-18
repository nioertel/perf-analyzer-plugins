package io.github.nioertel.perf.jdbc.tracker;

import java.lang.ref.WeakReference;

public class JdbcConnectionStats {

	private final long bindingThreadId;

	private final String bindingThreadName;

	private final long acquireStartTimestamp;

	private WeakReference<IdentifiableConnection> connection;

	private long connectionInstanceIdentifier;

	private long acquiredTimestamp;

	private long releaseStartTimestamp;

	private long releasedTimestamp;

	public JdbcConnectionStats(long bindingThreadId, String bindingThreadName, long acquireStartTimestamp) {
		this.bindingThreadId = bindingThreadId;
		this.bindingThreadName = bindingThreadName;
		this.acquireStartTimestamp = acquireStartTimestamp;
	}

	public long getBindingThreadId() {
		return bindingThreadId;
	}

	public String getBindingThreadName() {
		return bindingThreadName;
	}

	public long getAcquireStartTimestamp() {
		return acquireStartTimestamp;
	}

	public IdentifiableConnection getConnection() {
		return connection.get();
	}

	public void setConnection(IdentifiableConnection connection) {
		this.connection = new WeakReference<>(connection);
		this.connectionInstanceIdentifier = connection.getIdentifier();
	}

	public long getConnectionInstanceIdentifier() {
		return connectionInstanceIdentifier;
	}

	public long getAcquiredTimestamp() {
		return acquiredTimestamp;
	}

	public void setAcquiredTimestamp(long acquiredTimestamp) {
		this.acquiredTimestamp = acquiredTimestamp;
	}

	public long getReleaseStartTimestamp() {
		return releaseStartTimestamp;
	}

	public void setReleaseStartTimestamp(long releaseStartTimestamp) {
		this.releaseStartTimestamp = releaseStartTimestamp;
	}

	public long getReleasedTimestamp() {
		return releasedTimestamp;
	}

	public void setReleasedTimestamp(long releasedTimestamp) {
		this.releasedTimestamp = releasedTimestamp;
	}

	public JdbcConnectionStatsSnapshot snapshot() {
		return new JdbcConnectionStatsSnapshot(bindingThreadId, bindingThreadName, acquireStartTimestamp, connectionInstanceIdentifier,
				acquiredTimestamp, releaseStartTimestamp, releasedTimestamp);
	}

	private StringBuilder getCoreInfos() {
		return new StringBuilder()//
				.append("JdbcConnectionStats[id=")//
				.append(connectionInstanceIdentifier)//
				.append(", thread=")//
				.append(bindingThreadId)//
				.append("[")//
				.append(bindingThreadName)//
				.append("]")//
				.append(", acquire-start=")//
				.append(acquireStartTimestamp)//
				.append(", acquired=")//
				.append(acquiredTimestamp)//
				.append(", release-start=")//
				.append(releaseStartTimestamp)//
				.append(", released=")//
				.append(releasedTimestamp);
	}

	public String getDetails() {
		return getCoreInfos()//
				.append(", con-wait=")//
				.append(String.format("%,d", acquiredTimestamp - acquireStartTimestamp))//
				.append(", con-occupation=")//
				.append(String.format("%,d", releasedTimestamp - acquiredTimestamp))//
				.append("]")//
				.toString();
	}

	@Override
	public String toString() {
		return getCoreInfos()//
				.append("]")//
				.toString();
	}

}
