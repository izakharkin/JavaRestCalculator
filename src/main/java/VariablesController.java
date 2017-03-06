import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vendor.Calculator;
import vendor.ParsingException;

import java.util.List;

/**
 * Created by ilya on 14.12.16.
 */
@RestController
@RequestMapping("variable")
public class VariablesController {
    private static final Logger LOG = LoggerFactory.getLogger(VariablesController.class);
    @Autowired
    private RestfulCalculator calculator;
    @Autowired
    private BillingDao dbms;

    @RequestMapping(path = "/{variableName}", method = RequestMethod.GET, consumes = "text/plain", produces = "text/plain")
    public String getVariableValue(Authentication account,
                                   @PathVariable("variableName") String varName) throws ParsingException {
        LOG.debug("Variable GET request: [" + varName + "]");
        double value = dbms.getVariableValue(account.getName(), varName);
        LOG.trace("This variable has value: " + value);
        return Double.toString(value);
    }

    @RequestMapping(path = "/{variableName}", method = RequestMethod.PUT, consumes = "text/plain", produces = "text/plain")
    public void upsertVariable(Authentication account,
                               @PathVariable("variableName") String varName,
                               @RequestBody String valueExpression) {
        LOG.debug("Variable PUT request: [" + varName + ", " + valueExpression + "]");
        double value = Double.POSITIVE_INFINITY;
        try {
            value = calculator.calculate(account.getName(), valueExpression);
        } catch (ParsingException e) {
            LOG.trace("Wrong expression");
        }
        dbms.upsertVariable(account.getName(), varName, value);
        LOG.trace("Variable now has value: " + value);
    }

    @RequestMapping(path = "/{variableName}", method = RequestMethod.DELETE, consumes = "text/plain", produces = "text/plain")
    public void deleteVariable(Authentication account,
                               @PathVariable("variableName") String varName) {
        LOG.debug("Variable DELETE request: [" + varName + "]");
        dbms.dropVariable(account.getName(), varName);
        LOG.trace("Deleted successfully");
    }

    @RequestMapping(method = RequestMethod.GET, consumes = "text/plain", produces = "text/plain")
    public String getListOfAllVariables(Authentication account) throws ParsingException {
        LOG.debug("Printing the list of all variables:\n");
        List varList = dbms.getListOfAllVariables(account.getName());
        String response;
        if (varList.isEmpty()) {
            LOG.trace("User " + account.getName() + " has no variables");
            response = "You have no variables for now";
        } else {
            LOG.trace("List of all variables: " + varList.toString());
            response = varList.toString();
        }
        return response;
    }
}
