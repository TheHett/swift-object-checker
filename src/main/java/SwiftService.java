import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.client.factory.AuthenticationMethod;
import org.javaswift.joss.client.impl.ClientImpl;
import org.javaswift.joss.model.Account;

public class SwiftService {

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
