package net.kroia.modutilities.networking.client_server.arrs;

import java.util.concurrent.TimeoutException;

/**
 * An ARRS request was sent, but no response arrived: it timed out (see {@link GenericRequest#getTimeoutMs()})
 * or the connection to the receiver was lost. The receiver may or may not have handled it, so the caller
 * must not refund or retry blindly; ask the receiver for the status first.
 * <p>
 * A request that was never sent fails with {@link RequestNotSentException} instead.
 */
public class RequestOutcomeUnknownException extends TimeoutException {
    public RequestOutcomeUnknownException(String message) {
        super(message);
    }
}
