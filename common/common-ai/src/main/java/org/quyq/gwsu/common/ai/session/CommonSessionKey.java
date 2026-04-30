package org.quyq.gwsu.common.ai.session;


import io.agentscope.core.state.SessionKey;
import org.jspecify.annotations.NonNull;

/**
 * @author Quyq
 * @date 2026/4/23
 * @description 携带用户的sessionKey
 */
public record CommonSessionKey(
        String sessionId,
        String userId
) implements SessionKey {


    public static CommonSessionKey of(String sessionId, String userId) {
        return new CommonSessionKey(sessionId, userId);
    }

    @Override
    public String toIdentifier() {
        return this.sessionId;
    }

    @Override
    public @NonNull String toString() {
        return this.sessionId;
    }
}
