package com.google.blockly.utils;

import com.google.blockly.model.Connection;
import com.google.common.truth.FailureStrategy;
import com.google.common.truth.IntegerSubject;
import com.google.common.truth.Subject;
import com.google.common.truth.SubjectFactory;
import com.google.common.truth.Truth;

import static com.google.common.truth.Truth.assertAbout;

/**
 * Fluent subject for {@link Connection} objects.
 *
 * <code>assertAbout(ConnectionSubject.connection()).that(testConnection).is...</code>
 */
public class ConnectionSubject extends Subject<ConnectionSubject, Connection> {
    private static final SubjectFactory<ConnectionSubject, Connection> FACTORY =
            new SubjectFactory<ConnectionSubject, Connection>() {
                @Override
                public ConnectionSubject getSubject(FailureStrategy fs, Connection target) {
                    return new ConnectionSubject(fs, target);
                }
            };

    public static SubjectFactory<ConnectionSubject, Connection> connection() {
        return FACTORY;
    }

    public static ConnectionSubject assertThat(Connection connection) {
        return assertAbout(ConnectionSubject.connection()).that(connection);
    }

    // Must be public and non-final for generated subclasses (wrappers)
    public ConnectionSubject(FailureStrategy failureStrategy, Connection subject) {
        super(failureStrategy, subject);
    }

    public ConnectionAttempt connectingTo(Connection other) {
        return new ConnectionAttempt(other);
    }

    public class ConnectionAttempt {
        final Connection mOther;
        final int mResult;

        public ConnectionAttempt(Connection other) {
            mOther = other;
            mResult = actual().canConnectWithReason(other);
        }

        public void isSuccessful() {
            new IntegerSubject(failureStrategy, mResult).named("connection result")
                    .isEqualTo(Connection.CAN_CONNECT);
        }

        public void returnsReason(int expectedReason) {
            new IntegerSubject(failureStrategy, mResult).named("connection result")
                    .isEqualTo(expectedReason);
        }
    }
}
