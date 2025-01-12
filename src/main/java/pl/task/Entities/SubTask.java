package pl.task.Entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Table(name = "subTasks")
@Entity
public class SubTask {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime started;
    private LocalDateTime ended;

    @ManyToOne
    @JoinColumn(name = "status_id")
    private Status status;

    @Column(length = 1000)
    private String comment;

    @ManyToOne
    @JoinColumn(name = "task_id")
    @JsonBackReference
    private Task task;

    private String name;

    @Column(length = 1000)
    private String description;

    private String problem;

    @OneToMany(mappedBy = "subTask", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<SubTaskFileLink> fileLinks = new ArrayList<>();
}
