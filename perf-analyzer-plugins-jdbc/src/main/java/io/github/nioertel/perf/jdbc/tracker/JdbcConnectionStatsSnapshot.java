package io.github.nioertel.perf.jdbc.tracker;

public class JdbcConnectionStatsSnapshot {

	private final long bindingThreadId;

	private final String bindingThreadName;

	private final long acquireStartTimestamp;

	private final long connectionInstanceIdentifier;

	private final long acquiredTimestamp;

	private final long releaseStartTimestamp;

	private final long releasedTimestamp;

	public JdbcConnectionStatsSnapshot(long bindingThreadId, String bindingThreadName, long acquireStartTimestamp, long connectionInstanceIdentifier,
			long acquiredTimestamp, long releaseStartTimestamp, long releasedTimestamp) {
		this.bindingThreadId = bindingThreadId;
		this.bindingThreadName = bindingThreadName;
		this.acquireStartTimestamp = acquireStartTimestamp;
		this.connectionInstanceIdentifier = connectionInstanceIdentifier;
		this.acquiredTimestamp = acquiredTimestamp;
		this.releaseStartTimestamp = releaseStartTimestamp;
		this.releasedTimestamp = releasedTimestamp;
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

	public long getConnectionInstanceIdentifier() {
		return connectionInstanceIdentifier;
	}

	public long getAcquiredTimestamp() {
		return acquiredTimestamp;
	}

	public long getReleaseStartTimestamp() {
		return releaseStartTimestamp;
	}

	public long getReleasedTimestamp() {
		return releasedTimestamp;
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
