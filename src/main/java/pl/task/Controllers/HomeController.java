package pl.task.Controllers;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class HomeController {


    @RequestMapping({"/view/*"})
    public String index() {
        return "forward:/index.html";
    }

}


