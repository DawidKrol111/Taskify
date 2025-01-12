package pl.task.Entities;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.*;
@Getter
@Setter
@Entity
@Table(name = "subtask_file_links")
public class SubTaskFileLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String filePath;

    @ManyToOne
    @JoinColumn(name = "subtask_id", nullable = false)
    @JsonBackReference
    private SubTask subTask;
}
