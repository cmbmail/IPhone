package com.cmbchina.phonebiz.enums;

import java.util.Arrays;
import java.util.List;

public enum WorkOrderStatusTransition {
    ACCEPT(WorkOrderStatus.PENDING, Arrays.asList(WorkOrderStatus.PROCESSING)),
    APPROVE(WorkOrderStatus.PROCESSING, Arrays.asList(WorkOrderStatus.APPROVED)),
    REJECT(WorkOrderStatus.PROCESSING, Arrays.asList(WorkOrderStatus.REJECTED)),
    COMPLETE(WorkOrderStatus.APPROVED, Arrays.asList(WorkOrderStatus.COMPLETED)),
    CANCEL(Arrays.asList(WorkOrderStatus.PENDING, WorkOrderStatus.PROCESSING), Arrays.asList(WorkOrderStatus.CANCELLED)),
    REOPEN(Arrays.asList(WorkOrderStatus.REJECTED, WorkOrderStatus.CANCELLED), Arrays.asList(WorkOrderStatus.PENDING));

    private final List<WorkOrderStatus> fromStatuses;
    private final List<WorkOrderStatus> toStatuses;

    WorkOrderStatusTransition(WorkOrderStatus fromStatus, List<WorkOrderStatus> toStatuses) {
        this.fromStatuses = Arrays.asList(fromStatus);
        this.toStatuses = toStatuses;
    }

    WorkOrderStatusTransition(List<WorkOrderStatus> fromStatuses, List<WorkOrderStatus> toStatuses) {
        this.fromStatuses = fromStatuses;
        this.toStatuses = toStatuses;
    }

    public List<WorkOrderStatus> getFromStatuses() {
        return fromStatuses;
    }

    public List<WorkOrderStatus> getToStatuses() {
        return toStatuses;
    }

    public static boolean isValidTransition(WorkOrderStatus from, WorkOrderStatus to) {
        for (WorkOrderStatusTransition transition : values()) {
            if (transition.fromStatuses.contains(from) && transition.toStatuses.contains(to)) {
                return true;
            }
        }
        return false;
    }
}
