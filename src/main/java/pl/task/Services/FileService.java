package pl.task.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pl.task.Entities.FileLink;
import pl.task.Entities.SubTask;
import pl.task.Entities.SubTaskFileLink;
import pl.task.Entities.Task;
import pl.task.Repo.FileLinkRepository;
import pl.task.Repo.SubTaskFileLinkRepository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileService {

    @Autowired
    FileLinkRepository fileLinkRepository;

    @Autowired
    SubTaskFileLinkRepository subTaskFileLinkRepository;

    private final String TASK_PATH = "upload/tasks/input/";
    private final String SUBTASK_PATH = "upload/subtasks/input/";

    public boolean saveTaskFiles(List<MultipartFile> files, Task task) throws IOException {
        if (files == null && files.isEmpty()) {
            return false;
        }

        for (MultipartFile file : files) {

            String path = TASK_PATH + task.getId() + File.separator;
            String fileName = file.getOriginalFilename();
            String nameCleaned = FileService.sanitizeFileName(fileName);
            saveFile(file, path);
            FileLink fileLink = new FileLink();
            String pathDatabase = path.replace("upload", "");

            fileLink.setFilePath(pathDatabase + nameCleaned);
            fileLink.setTask(task);
            fileLink.setOutput(false);
            task.getFileLinks().add(fileLink);
            fileLinkRepository.save(fileLink);
        }
        return true;
    }

    public boolean saveSubtaskFiles(List<MultipartFile> files, SubTask subtask) throws IOException {
        if (files == null) {
            return false;
        }
        for (MultipartFile file : files) {
            String path = SUBTASK_PATH + subtask.getId() + File.separator;
            String fileName = file.getOriginalFilename();
            String nameCleaned = FileService.sanitizeFileName(fileName);
            saveFile(file, path);
            SubTaskFileLink subTaskFileLink = new SubTaskFileLink();
            String pathDatabase = path.replace("upload", "");

            subTaskFileLink.setFilePath(pathDatabase + nameCleaned);
            subTaskFileLink.setSubTask(subtask);
            subtask.getFileLinks().add(subTaskFileLink);
            subTaskFileLinkRepository.save(subTaskFileLink);
        }
        return true;
    }


    public String saveFile(MultipartFile multipartFile, String subdirectory) throws IOException {
        String appDirectory = System.getProperty("user.dir");

        String directoryPath = appDirectory + File.separator + subdirectory;

        File directory = new File(directoryPath);
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new IOException("Nie można utworzyć katalogu: " + directoryPath);
            }
        }

        String originalFileName = multipartFile.getOriginalFilename();
        String sanitizedFileName = sanitizeFileName(originalFileName);
        String filePath = directoryPath + File.separator + sanitizedFileName;

        File fileToSave = new File(filePath);
        multipartFile.transferTo(fileToSave);

        return fileToSave.getAbsolutePath();
    }


    public static String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9_\\.]", "");
    }


}
