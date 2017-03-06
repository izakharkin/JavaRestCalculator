import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vendor.Calculator;
import vendor.ParsingException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ilya on 14.12.16.
 */
@RestController
@RequestMapping("function")
public class FunctionsController {
    private static final Logger LOG = LoggerFactory.getLogger(FunctionsController.class);
    @Autowired
    private BillingDao dbms;

    @RequestMapping(path = "/{functionName}", method = RequestMethod.GET, consumes = "text/plain", produces = "text/plain")
    public String getFunctionBody(Authentication account,
                                   @PathVariable("functionName") String funcName) throws ParsingException {
        LOG.debug("Function GET request: [" + funcName + "]");
        String body = dbms.getFunctionBody(account.getName(), funcName);
        LOG.trace("This function has body: " + body);
        return body;
    }

    @RequestMapping(path = "/{functionName}", method = RequestMethod.PUT, consumes = "text/plain", produces = "text/plain")
    public void upsertFunction(Authentication account,
                               @PathVariable("functionName") String funcName,
                               @RequestParam(value = "args") List<String> args,
                               @RequestBody String bodyExpression) {
        LOG.debug("Function PUT request: [" + funcName + "] with args [" + args.toString() + "] and body [" + bodyExpression + "]");
        dbms.upsertFunction(account.getName(), funcName, args, bodyExpression);
        LOG.trace("Function now has body: " + bodyExpression);
    }

    @RequestMapping(path = "/{functionName}", method = RequestMethod.DELETE, consumes = "text/plain", produces = "text/plain")
    public void deleteFunction(Authentication account,
                               @PathVariable("functionName") String funcName) {
        LOG.debug("Function DELETE request: [" + funcName + "]");
        dbms.dropFunction(account.getName(), funcName);
        LOG.trace("Deleted successfully");
    }

    @RequestMapping(method = RequestMethod.GET, consumes = "text/plain", produces = "text/plain")
    public String getListOfAllFunctions(Authentication account) throws ParsingException {
        LOG.debug("Printing the list of all functions:\n");
        List funcList = dbms.getListOfAllFunctions(account.getName());
        String response;
        if (funcList.isEmpty()) {
            LOG.trace("User " + account.getName() + " has no specified functions");
            response = "You have no specified functions for now";
        } else {
            LOG.trace("List of all functions: " + funcList.toString());
            response = funcList.toString();
        }
        return response;
    }
}
