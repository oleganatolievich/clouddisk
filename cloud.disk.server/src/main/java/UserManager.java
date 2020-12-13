import java.sql.*;

public class UserManager {

    private Connection dbConnection;

    public OperationResult initDBConnection() {
        OperationResult funcResult = OperationResult.getSuccess(null);
        String dbURL = "jdbc:sqlite:users.db";
        dbConnection = null;
        try {
            Class.forName("org.sqlite.JDBC");
            dbConnection = DriverManager.getConnection(dbURL);
            PreparedStatement createUsersTableStmt = dbConnection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS \"users\" (\n" +
                            "\t\"login\"\tTEXT NOT NULL UNIQUE,\n" +
                            "\t\"password_hash\"\tTEXT NOT NULL,\n" +
                            "\tCONSTRAINT \"pk_users_login\" PRIMARY KEY(\"login\")\n" +
                            ")");
            createUsersTableStmt.execute();
        } catch (ClassNotFoundException | SQLException e) {
            funcResult = OperationResult.getExceptionResult(e);
        }
        return funcResult;
    }

    public OperationResult closeDBConnection() {
        OperationResult funcResult = OperationResult.getSuccess(null);
        if (dbConnection != null) {
            try {
                if (!dbConnection.isClosed()) dbConnection.close();
            } catch (SQLException e) {
                funcResult = OperationResult.getExceptionResult(e);
            }
        }
        return funcResult;
    }

    public OperationResult<Boolean> addUser(String login, String password) {
        OperationResult<Boolean> funcResult = null;
        if (dbConnection == null) funcResult = OperationResult.getFailure("Database is not connected");
        else {
            String passwordHash = "";
            OperationResult<String> hashingResult = PasswordHasher.createHash(password);
            if (hashingResult.isSuccess()) {
                passwordHash = hashingResult.getProduct();
                try {
                    PreparedStatement updateUsersStmt = dbConnection.prepareStatement(
                            "INSERT INTO users (login, password_hash)\n" +
                                    "VALUES\n" +
                                    "   (?, ?)");
                    updateUsersStmt.setString(1, login);
                    updateUsersStmt.setString(2, passwordHash);
                    updateUsersStmt.executeUpdate();
                    funcResult = OperationResult.getSuccess(true);
                } catch (SQLException e) {
                    funcResult = OperationResult.getExceptionResult(e);
                }
            } else funcResult = OperationResult.getExceptionResult(hashingResult.getException());
        }
        return funcResult;
    }

    public OperationResult<Boolean> userExists(String login) {
        OperationResult<Boolean> funcResult = null;
        if (dbConnection == null) funcResult = OperationResult.getFailure("Database is not connected");
        else {
            try {
                PreparedStatement updateUsersStmt = dbConnection.prepareStatement(
                        "SELECT T.login\n" +
                                "FROM users AS T\n" +
                                "WHERE T.login = (?)\n" +
                                "LIMIT 1");
                updateUsersStmt.setString(1, login);
                ResultSet resultSet = updateUsersStmt.executeQuery();
                funcResult = OperationResult.getSuccess(resultSet.next());
            } catch (SQLException e) {
                funcResult = OperationResult.getExceptionResult(e);
            }
        }
        return funcResult;
    }

    public OperationResult<Boolean> isPasswordCorrect(String login, String password) {
        OperationResult<Boolean> funcResult = null;
        if (dbConnection == null) funcResult = OperationResult.getFailure("Database is not connected");
        else {
            try {
                PreparedStatement checkUserPasswordStmt = dbConnection.prepareStatement(
                        "SELECT T.password_hash AS password_hash\n" +
                                "FROM users AS T\n" +
                                "WHERE T.login = (?)\n" +
                                "LIMIT 1");
                checkUserPasswordStmt.setString(1, login);
                ResultSet resultSet = checkUserPasswordStmt.executeQuery();
                if (resultSet.next()) {
                    String passwordHash = resultSet.getString("password_hash");
                    OperationResult<Boolean> operationResult = PasswordHasher.isValidPassword(password, passwordHash);
                    if (!operationResult.isSuccess()) funcResult = operationResult;
                    else funcResult = OperationResult.getSuccess(operationResult.getProduct());
                } else funcResult = OperationResult.getSuccess(false);
            } catch (SQLException e) {
                funcResult = OperationResult.getExceptionResult(e);
            }
        }
        return funcResult;
    }

}
