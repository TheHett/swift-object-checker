import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.client.factory.AuthenticationMethod;
import org.javaswift.joss.client.impl.ClientImpl;
import org.javaswift.joss.model.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwiftService {

    private final static Logger logger = LoggerFactory.getLogger(SwiftService.class);

    public Account authenticate(String username, String password, String authUrl) {
        logger.debug("Authenticate {} as {}", authUrl, username);
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
