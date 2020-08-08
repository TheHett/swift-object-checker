import org.apache.log4j.Logger;
import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.client.factory.AuthenticationMethod;
import org.javaswift.joss.client.impl.ClientImpl;
import org.javaswift.joss.exception.CommandException;
import org.javaswift.joss.exception.NotFoundException;
import org.javaswift.joss.model.Account;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class SwiftService {

    private final static Logger logger = Logger.getLogger(SwiftService.class);

    public Account authenticate(String username, String password, String authUrl) {
        AccountConfig config = new AccountConfig();
        config.setAuthenticationMethod(AuthenticationMethod.TEMPAUTH);
        config.setUsername(username);
        config.setPassword(password);
        config.setAuthUrl(authUrl);
        config.setAllowCaching(false);
        config.setAllowContainerCaching(false);
        config.setAllowReauthenticate(true);

        var client = new ClientImpl(config);
        return client.authenticate();
    }
}
