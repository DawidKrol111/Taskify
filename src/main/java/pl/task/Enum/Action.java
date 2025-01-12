package pl.task.Enum;

public enum Action {
    // Akcje związane z zadaniami
    TASK_CREATED,
    TASK_ASSIGNED,
    TASK_COMPLETED,
    TASK_REASSIGNED,
    TASK_STATUS_CHANGED,

    // Akcje związane z podzadaniami
    SUBTASK_STARTED,
    SUBTASK_COMPLETED,

    // Akcje związane z użytkownikami
    USER_CREATED,
    USER_UPDATED,
    USER_DEACTIVATED,
    USER_ROLE_UPDATED,
    USER_PASSWORD_CHANGED,

    // Akcje związane z notatkami użytkownika
    USER_NOTE_CREATED,
    USER_NOTE_DELETED,
    USER_ASSIGNED
}
