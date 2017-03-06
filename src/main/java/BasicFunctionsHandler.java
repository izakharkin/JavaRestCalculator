import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ilya on 19.12.16.
 */
@Component("FunctionsHolder")
@Configurable
public class BasicFunctionsHandler {
    private static final Map<String, Integer> BASIC_FUNCTIONS;
    static {
        Map<String, Integer> aMap = new HashMap<>();
        aMap.put("sin", 1);
        aMap.put("cos", 1);
        aMap.put("tg", 1);
        aMap.put("sqrt", 1);
        aMap.put("pow", 2);
        aMap.put("abs", 1);
        aMap.put("sign", 1);
        aMap.put("log", 2);
        aMap.put("log2", 1);
        aMap.put("max", 2);
        aMap.put("min", 2);
        BASIC_FUNCTIONS = Collections.unmodifiableMap(aMap);
    }
    @Autowired
    private BillingDao dbms;

    public static boolean isBasicFunction(String funcName) {
        return BASIC_FUNCTIONS.containsKey(funcName);
    }

    public static double execBasicFunction(String funcName, List<Double> valuesOfArguments) throws IllegalArgumentException {
        if (valuesOfArguments.size() != BASIC_FUNCTIONS.get(funcName)) {
            throw new IllegalArgumentException("Wrong number of parameters in basic function");
        }
        double result = 0;
        if (funcName.equals("cos")) {
            result = Math.cos(valuesOfArguments.get(0));
        } else if (funcName.equals("sin")) {
            result = Math.sin(valuesOfArguments.get(0));
        } else if (funcName.equals("tg")) {
            result = Math.tan(valuesOfArguments.get(0));
        } else if (funcName.equals("sqrt")) {
            result = Math.sqrt(valuesOfArguments.get(0));
        } else if (funcName.equals("pow")) {
            result = Math.pow(valuesOfArguments.get(0), valuesOfArguments.get(1));
        } else if (funcName.equals("abs")) {
            result = Math.abs(valuesOfArguments.get(0));
        } else if (funcName.equals("sign")) {
            result = Math.signum(valuesOfArguments.get(0));
        } else if (funcName.equals("log")) {
            result = Math.log(valuesOfArguments.get(1)) / Math.log(valuesOfArguments.get(0));
        } else if (funcName.equals("log2")) {
            result = Math.log(valuesOfArguments.get(0)) / Math.log(2.0);
        } else if (funcName.equals("rnd")) {
            result = Math.random();
        } else if (funcName.equals("max")) {
            result = Math.max(valuesOfArguments.get(0), valuesOfArguments.get(1));
        } else if (funcName.equals("min")) {
            result = Math.min(valuesOfArguments.get(0), valuesOfArguments.get(1));
        }
        return result;
    }
}
