package com.cmbchina.phonebiz.enums;

import java.util.Arrays;
import java.util.List;

public enum PhoneStatusTransition {
    ASSIGN(PhoneStatus.UNASSIGNED, Arrays.asList(PhoneStatus.ASSIGNED)),
    ACTIVATE(PhoneStatus.ASSIGNED, Arrays.asList(PhoneStatus.IN_USE)),
    SUSPEND(PhoneStatus.IN_USE, Arrays.asList(PhoneStatus.SUSPENDED)),
    RESUME(PhoneStatus.SUSPENDED, Arrays.asList(PhoneStatus.IN_USE)),
    RECYCLE(Arrays.asList(PhoneStatus.UNASSIGNED, PhoneStatus.SUSPENDED), Arrays.asList(PhoneStatus.RECYCLED)),
    REASSIGN(Arrays.asList(PhoneStatus.ASSIGNED, PhoneStatus.IN_USE), Arrays.asList(PhoneStatus.UNASSIGNED));

    private final List<PhoneStatus> fromStatuses;
    private final List<PhoneStatus> toStatuses;

    PhoneStatusTransition(PhoneStatus fromStatus, List<PhoneStatus> toStatuses) {
        this.fromStatuses = Arrays.asList(fromStatus);
        this.toStatuses = toStatuses;
    }

    PhoneStatusTransition(List<PhoneStatus> fromStatuses, List<PhoneStatus> toStatuses) {
        this.fromStatuses = fromStatuses;
        this.toStatuses = toStatuses;
    }

    public List<PhoneStatus> getFromStatuses() {
        return fromStatuses;
    }

    public List<PhoneStatus> getToStatuses() {
        return toStatuses;
    }

    public static boolean isValidTransition(PhoneStatus from, PhoneStatus to) {
        for (PhoneStatusTransition transition : values()) {
            if (transition.fromStatuses.contains(from) && transition.toStatuses.contains(to)) {
                return true;
            }
        }
        return false;
    }
}
