package net.kroia.modutilities.networking.multi_server;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Identity of this server for diagnostics. Persisted with the world; regenerated when slave_id changes.
 * <p>
 * Diagnostics only: the uids are self-reported over the wire and must never be used for authorization.
 * Obtain it via {@link MultiServerManager#getIdentity()}.
 */
public interface IServerIdentity {
    /** @return random id of this server; changes only when slave_id changes or the identity file is deleted. */
    UUID serverUid();
    /** @return STANDALONE, MASTER or SLAVE. */
    ServerRole role();
    /** @return configured slave name on a slave, otherwise null. */
    @Nullable String slaveId();
    /** @return on a slave, the master's serverUid once the handshake ran; otherwise null. */
    @Nullable UUID masterUid();
    /** @return on the master, the last known serverUid of that slave (from known_slaves), or null if never seen / old slave without uid. */
    @Nullable UUID slaveUid(String slaveId);
}
