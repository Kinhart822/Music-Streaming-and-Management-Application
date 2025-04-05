package com.spring.constants;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum CommonStatus {
    LOCKED(-4),
    DELETED(-3),
    INACTIVE(-1),
    ACTIVE(1);

    // Explicitly define the getter
    private final int status;

    CommonStatus(int status) {
        this.status = status;
    }

    private static final Map<Integer, CommonStatus> STATUS_MAP = new HashMap<>();

    static {
        for (CommonStatus commonStatus : CommonStatus.values()) {
            STATUS_MAP.put(commonStatus.getStatus(), commonStatus);
        }
    }

    public static CommonStatus getByStatus(int status) {
        return STATUS_MAP.get(status);
    }
}
