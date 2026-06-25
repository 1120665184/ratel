package org.quyq.gwsu.security.connect.enums;


import lombok.Getter;

import java.util.stream.Stream;

/**
 * @author Quyq
 * @date 2026/6/22
 * @description
 */
@Getter
public enum DingTalkMsgType {

    TEXT("text"),
    AUDIO("audio"),
    PICTURE("picture"),
    VIDEO("video"),
    FILE("file"),
    RICHTEXT("richText");

    private final String code;

    DingTalkMsgType(String code) {
        this.code = code;
    }

    public static DingTalkMsgType getByCode(String code) {
        return Stream.of(DingTalkMsgType.values()).filter(e -> e.code.equals(code)).findFirst().orElseThrow(() -> new RuntimeException("未知的消息类型"));
    }

}
