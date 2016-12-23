/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.blockly.utils;

import com.google.blockly.model.Connection;
import com.google.common.truth.FailureStrategy;
import com.google.common.truth.IntegerSubject;
import com.google.common.truth.Subject;
import com.google.common.truth.SubjectFactory;

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
