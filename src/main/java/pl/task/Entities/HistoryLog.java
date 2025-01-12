package pl.task.Entities;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.task.Enum.Action;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class HistoryLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Action action;
    private Long aboutId;
    private LocalDateTime time;
    private String info;

    public HistoryLog(Action action, Long aboutId, String info) {
        this.action = action;
        this.aboutId = aboutId;
        this.time = LocalDateTime.now(); // Ustaw czas przy tworzeniu instancji
        this.info = info;
    }
    public HistoryLog(Action action, Long aboutId) {
        this.action = action;
        this.aboutId = aboutId;
        this.time = LocalDateTime.now(); // Ustaw czas przy tworzeniu instancji
    }

    public String getActionAsString(){
        return action.toString();
    }
}
