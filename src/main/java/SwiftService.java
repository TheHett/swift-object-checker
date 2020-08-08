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

    public void deleteObjects(Account account, File log) throws IOException {
        try (var reader = new BufferedReader(new FileReader(log))) {
            String path;
            while ((path = reader.readLine()) != null) {
                if (path.isEmpty()) {
                    continue;
                }
                logger.info("Delete object " + path);
                var slashPos = path.indexOf("/");
                if (slashPos == -1) {
                    logger.warn("Invalid object path " + path);
                    continue;
                }
                var container = account.getContainer(path.substring(0, slashPos));
                if (!container.exists()) {
                    logger.warn(String.format("Container %s isn't exists", container.getName()));
                    continue;
                }
                var object = container.getObject(path.substring(slashPos + 1));
                if (!object.exists()) {
                    try {
                        object.delete();
                    } catch (NotFoundException ignoring) {
                        // the swift will throws this exception when deleting problem objects
                    } catch (CommandException e) {
                        logger.warn("Error delete object", e);
                    }
                }
            }
        }
    }
}
