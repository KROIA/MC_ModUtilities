package net.kroia.modutilities.networking.client_server.arrs;

/**
 * An ARRS master/slave request was never sent (the link refused it, e.g. not connected).
 * Nothing happened on the receiver, so the caller may refund safely.
 *
 * @see RequestOutcomeUnknownException
 */
public class RequestNotSentException extends IllegalStateException {
    public RequestNotSentException(String message) {
        super(message);
    }
}
