package pl.task.Entities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.Getter;
import lombok.Setter;
import pl.task.Controllers.UserController;

@Getter
@Setter
public class InternalResponse {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private String message;
    private boolean success;

    public InternalResponse() {
        this.message = "Domyślna wiadomość";
        this.success = false;

    }

    public InternalResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
        System.out.println("this.message = " + this.message);

    }

    public static InternalResponse success(String message) {
        logger.info("Sukces: {}", message);

        return new InternalResponse(message, true);
    }

    public static InternalResponse error(String message) {
        logger.error(message);

        return new InternalResponse(message, false);
    }
}
