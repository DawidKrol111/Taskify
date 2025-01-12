package pl.task.Services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import pl.task.Entities.Status;
import pl.task.Repo.StatusRepository;

import java.util.Map;
import java.util.Optional;


@Service
public class StatusService {


    private final StatusRepository statusRepository;

    public StatusService(StatusRepository statusRepository) {
        this.statusRepository = statusRepository;
    }

    public Status isStatusExistingFromName(String statusName) {

        Status status = statusRepository.findStatusByName(statusName);
        return status;
    }

    public Status parseStatusFromJson(String json){

        ObjectMapper objectMapper = new ObjectMapper();
        Status statusStatus = null;

        try {
            statusStatus = objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception e) {
            e.getMessage();
        }
        if(statusRepository.findById(statusStatus.getId()).isPresent())
        {return statusStatus;}
        else{
            return null;
        }
    }

}
