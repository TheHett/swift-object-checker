import org.apache.commons.cli.*;
import org.javaswift.joss.exception.CommandException;
import org.javaswift.joss.exception.NotFoundException;
import org.javaswift.joss.model.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.*;
import java.nio.charset.StandardCharsets;

public class Application {

    private final static Logger logger = LoggerFactory.getLogger(Application.class);

    public static final String MODE_COLLECT = "collect";
    public static final String MODE_DELETE = "delete";

    public static void main(String[] args) throws IOException, InterruptedException {

        final CommandLine commandLine;
        try {
            commandLine = CommandLineBuilder.build(args);
        } catch (ParseException e) {
            logger.error("Error parsing command arguments: {}", e.getMessage());
            return;
        }

        final var log = new File(commandLine.getOptionValue("log"));
        final var swift = new SwiftService();
        final var concurrency = Integer.parseInt(commandLine.getOptionValue("concurrency", "10"));
        final var replaceLog = commandLine.hasOption("replaceLog");
        final var account = swift.authenticate(
                commandLine.getOptionValue("user"),
                commandLine.getOptionValue("password"),
                commandLine.getOptionValue("authUrl")
        );

        final var mode = commandLine.getOptionValue("mode");

        if (mode.equals(Application.MODE_COLLECT)) {
            collect(account, log, replaceLog, concurrency);
        } else if (mode.equals(Application.MODE_DELETE)) {
            delete(account, log);
        } else {
            logger.error("Unknown mode (should be collect or delete)");
        }
    }

    private static void collect(Account account, File log, boolean replaceLog, int concurrency)
            throws IOException, InterruptedException {

        if (log.exists() && !replaceLog) {
            logger.error("Log {} file already exists", log.getAbsolutePath());
            return;
        }

        final var checker = new ObjectCheckerService(concurrency);
        final var totalReport = checker.checkAllContainers(account);
        checker.shutdown();


        try (var writer = new PrintWriter(log, StandardCharsets.UTF_8)) {
            for (var objectPath : totalReport.getNotFoundObjects()) {
                writer.println(objectPath);
            }
        }

        logger.info("Ended work: {}", totalReport.asString());
        logger.info("The log was saved to {}", log.getAbsolutePath());
    }

    private static void delete(Account account, File log) throws IOException {
        try (var reader = new BufferedReader(new FileReader(log))) {
            String path;
            while ((path = reader.readLine()) != null) {
                if (path.isEmpty()) {
                    continue;
                }
                logger.info("Deleting object {}", path);
                var slashPos = path.indexOf("/");
                if (slashPos == -1) {
                    logger.warn("Invalid object path {}", path);
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
                } else {
                    logger.warn("The object {} is exists, the deletion skipped", path);
                }
            }
        }
    }
}
