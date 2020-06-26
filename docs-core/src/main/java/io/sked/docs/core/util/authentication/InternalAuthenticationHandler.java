package io.sked.docs.core.util.authentication;

import io.sked.docs.core.dao.UserDao;
import io.sked.docs.core.model.jpa.User;
import io.sked.util.ClasspathScanner;

/**
 * Authenticate using the internal database.
 *
 * @author bgamard
 */
@ClasspathScanner.Priority(100) // We can add handlers before this one
public class InternalAuthenticationHandler implements AuthenticationHandler {
    @Override
    public User authenticate(String username, String password) {
        UserDao userDao = new UserDao();
        return userDao.authenticate(username, password);
    }
}
