package pl.task.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.task.Entities.HistoryLog;
import pl.task.Enum.Action;
import pl.task.Repo.HistoryLogRepository;

@Service
public class HistoryLogService {

    private final HistoryLogRepository historyLogRepository;

    @Autowired
    public HistoryLogService(HistoryLogRepository historyLogRepository) {
        this.historyLogRepository = historyLogRepository;
    }

    public void logHistory(Long id, Action action) {

        HistoryLog historyLog = new HistoryLog(action, id);
        historyLogRepository.save(historyLog);
    }

    public void logHistory(Long id, Action action, String info) {

        HistoryLog historyLog = new HistoryLog(action, id, info);
        historyLogRepository.save(historyLog);
    }
}
