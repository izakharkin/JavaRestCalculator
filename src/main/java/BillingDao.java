import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository("BillingDao")
public class BillingDao {
    private static final Logger LOG = LoggerFactory.getLogger(BillingDao.class);

    @Autowired
    private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void postConstruct() {
        jdbcTemplate = new JdbcTemplate(dataSource, false);
        initSchema();
    }

    public void initSchema() {
        LOG.trace("Initializing schema..");
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS billing");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS billing.users " +
                "(username VARCHAR PRIMARY KEY, password VARCHAR, enabled BOOLEAN)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS billing.variables " +
                "(username VARCHAR, varname VARCHAR, value DOUBLE)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS billing.functions " +
                "(username VARCHAR, funcname VARCHAR, valence INTEGER, funcbody VARCHAR)");
        List isThereAdmin = jdbcTemplate.queryForList("SELECT username FROM billing.users WHERE username = 'username'");
        if (isThereAdmin.isEmpty()) {
            jdbcTemplate.update("INSERT INTO billing.users VALUES ('username', 'password', TRUE)");
        }
    }

    public BillingUser loadUser(String username) throws EmptyResultDataAccessException {
        LOG.trace("Querying for user " + username);
        return jdbcTemplate.queryForObject(
                "SELECT username, password, enabled FROM billing.users WHERE username = ?",
                new Object[]{username},
                new RowMapper<BillingUser>() {
                    @Override
                    public BillingUser mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return new BillingUser(
                                rs.getString("username"),
                                rs.getString("password"),
                                rs.getBoolean("enabled")
                        );
                    }
                }
        );
    }

    public void addNewUser(String username, String password) {
        LOG.trace("Adding new user with name [" + username + "] and password [" + password + "]..");
        jdbcTemplate.update("INSERT INTO billing.users VALUES ('" + username + "', '" + password + "', FALSE)");
    }

    public void upsertVariable(String userName, String varName, double value) throws EmptyResultDataAccessException {
        LOG.trace("Putting variable [" + varName + "] with value [" + value + "] of user [" + userName + "]..");
        dropVariable(userName, varName);
        jdbcTemplate.update("INSERT INTO billing.variables VALUES ('" + userName + "' , '" + varName + "', " + Double.toString(value) + ")");
    }

    public double getVariableValue(String userName, String varName) throws EmptyResultDataAccessException {
        LOG.trace("Showing the value of variable " + varName + " of user " + userName + "..");
        double value = jdbcTemplate.queryForObject("SELECT value FROM billing.variables WHERE username = '" + userName + "' AND varname = '" + varName + "'", new Double[]{},
                new RowMapper<Double>() {
                    @Override
                    public Double mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return new Double(
                                rs.getString("value").toString()
                        );
                    }
                });
        return value;
    }

    public void dropVariable(String userName, String varName) {
        LOG.trace("Deleting variable [" + varName + "] of user [" + userName + "]..");
        jdbcTemplate.execute("DELETE FROM billing.variables WHERE varname = '" + varName + "' AND username = '" + userName + "'");
    }

    public List getListOfAllVariables(String userName) {
        LOG.trace("Showing the list of all variables of user [" + userName + "]..");
        List varList = jdbcTemplate.queryForList("SELECT varname, value FROM billing.variables WHERE username = '" + userName + "'");
        return varList;
    }

    public String setParameterPositions(String userName,List<String> args, String expression) {
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilder varNameBuilder = new StringBuilder();
        Map<String, Integer> argMap = new HashMap();
        for (int i = 0; i < args.size(); ++i) {
            argMap.put(args.get(i), i);
        }
        for (int i = 0; i < expression.length(); ++i) {
            if (Character.isAlphabetic(expression.charAt(i))) {
                varNameBuilder.setLength(0);
                while (i < expression.length() && (Character.isAlphabetic(expression.charAt(i)) || Character.isDigit(expression.charAt(i)) || expression.charAt(i) == '_')){
                    varNameBuilder.append(expression.charAt(i));
                    i += 1;
                }
                if (i < expression.length() && expression.charAt(i) == '(') {
                    stringBuilder.append(varNameBuilder.toString() + '(');
                    continue;
                }
                String varName = varNameBuilder.toString();
                Integer pos = argMap.get(varName);
                if (pos != null) {
                    stringBuilder.append('$' + pos.toString());
                } else {
                    stringBuilder.append(varName);
                }
                i -= 1;
            } else if (expression.charAt(i) != ' ') {
                stringBuilder.append(expression.charAt(i));
            }
        }
        return stringBuilder.toString();
    }

    public void upsertFunction(String userName, String funcName, List<String> args, String bodyExpression) throws EmptyResultDataAccessException {
        LOG.trace("Putting function [" + funcName + "] with args [" + args.toString() + "] and body [" + bodyExpression + "] of user [" + userName + "]..");
        dropFunction(userName, funcName);
//        Helper helper = new Helper();
        String newBodyExpression = setParameterPositions(userName, args, bodyExpression);
        jdbcTemplate.update("INSERT INTO billing.functions VALUES ('" + userName + "', '" + funcName + "', " + Integer.toString(args.size()) + ", '" + newBodyExpression + "')");
    }

    public String getFunctionBody(String userName, String funcName) throws EmptyResultDataAccessException {
        LOG.trace("Showing the body of function [" + funcName + "] of user [" + userName + "]..");
        String body = jdbcTemplate.queryForObject("SELECT funcbody FROM billing.functions WHERE username = '" + userName + "' AND funcname = '" + funcName + "'", new String[]{},
                new RowMapper<String>() {
                    @Override
                    public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return new String(
                                rs.getString("funcbody")
                        );
                    }
                });
        return body;
    }

    public Integer getFunctionValence(String userName, String funcName) throws EmptyResultDataAccessException {
        LOG.trace("Showing the valence of function [" + funcName + "] of user [" + userName + "]..");
        Integer valence = jdbcTemplate.queryForObject("SELECT valence FROM billing.functions " + "WHERE username = '" + userName + "' AND funcname = '" + funcName + "'", new Integer[]{},
                new RowMapper<Integer>() {
                    @Override
                    public Integer mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return new Integer(
                                rs.getInt("valence")
                        );
                    }
                });
        return valence;
    }

    public void dropFunction(String userName, String funcName) {
        LOG.trace("Deleting function [" + funcName + "] of user [" + userName + "]..");
        jdbcTemplate.execute("DELETE FROM billing.functions WHERE funcname = '" + funcName + "' AND username = '" + userName + "'");
    }

    public List getListOfAllFunctions(String userName) {
        LOG.trace("Showing the list of all variables of user [" + userName + "]..");
        List varList = jdbcTemplate.queryForList("SELECT funcname, funcbody FROM billing.functions WHERE username = '" + userName + "'");
        return varList;
    }

}
