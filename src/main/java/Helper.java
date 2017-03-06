import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ilya on 19.12.16.
 */
@Component
public class Helper {
    private static final Logger LOG = LoggerFactory.getLogger(BillingDao.class);

    public static List<String> getArgumentsFromExpression(String argExpression, int startPos, AtomicInteger endPos) {
        List<String> args = new ArrayList<>();
        int balance = 0;
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = startPos + 1; i < argExpression.length(); ++i) {
            char curChar = argExpression.charAt(i);
            if (!(balance == 0 && (curChar == ')' || curChar == ','))) {
                stringBuilder.append(curChar);
            }
            if (curChar == ',') {
                if (balance == 0) {
                    args.add(stringBuilder.toString());
                    stringBuilder.setLength(0);
                }
            } else if (curChar == '(') {
                balance += 1;
            } else if (curChar == ')') {
                if (balance == 0) {
                    endPos.set(i);
                    args.add(stringBuilder.toString());
                    break;
                }
                balance -= 1;
            }
        }
        return args;
    }
}
