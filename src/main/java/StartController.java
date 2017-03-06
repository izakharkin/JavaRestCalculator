import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import vendor.Calculator;

import java.util.List;

/**
 * Created by ilya on 14.12.16.
 */
@RestController
public class StartController {
    private static final Logger LOG = LoggerFactory.getLogger(CalculatorController.class);
    @Autowired
    private BillingDao dbms;

    @RequestMapping(method = RequestMethod.GET, produces = "text/html")
    public String main(@RequestParam(value = "name", required = false) String name) {
        LOG.debug("Printing the greeting");
        if (name == null) {
            name = "world";
        }
        return "<html>" +
                "<head><title>IlyaApp</title></head>" +
                "<body><h1>Hello, " + name + "!</h1></body>" +
                "</html>";
    }

    @RequestMapping(path = "/ping", method = RequestMethod.GET, produces = "text/plain")
    public String echo() {
        return "OK\n";
    }

    @RequestMapping(path = "/register", method = RequestMethod.POST, produces = "text/plain")
    public void registerUser(@RequestParam(value = "name", required = false) String name,
                             @RequestParam(value = "pass", required = false) String pass) {
        LOG.debug("Registrering new user with username [" + name + "] and password [" + pass + "]");
        dbms.addNewUser(name, pass);
        LOG.debug("Registered successfully");
    }
}
