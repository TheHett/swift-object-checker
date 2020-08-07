import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.javaswift.joss.model.Container;


import java.io.*;
import java.nio.charset.StandardCharsets;

public class Application {

    private final static Logger logger = Logger.getLogger(Application.class);

    public static void main(String[] args) throws IOException {
        final var commandLine = setupOptions(args);
        if (commandLine == null) {
            return;
        }

        final var swift = new SwiftService();
        final var checker = new ObjectCheckerService();
        checker.setConcurrency(Integer.parseInt(commandLine.getOptionValue("concurrency")));

        final var account = swift.authenticate(
                commandLine.getOptionValue("user"),
                commandLine.getOptionValue("password"),
                commandLine.getOptionValue("authUrl")
        );
        final var totalReport = new Report();

        final var log = new File(commandLine.getOptionValue("log"));
        if (log.exists()) {
            logger.error("Log " + log.getAbsolutePath() + " file already exists");
            return;
        }

        try (var writer = new PrintWriter(log, StandardCharsets.UTF_8)) {
            account.list().forEach((final Container container) -> {
                try {
                    final var report = checker.check(container, (object) -> {
                        synchronized (Application.class) {
                            writer.println(container.getName() + "/" + object.getName());
                        }
                    });
                    totalReport.appendFrom(report);
                } catch (InterruptedException e) {
                    logger.error("Error check container: " + e.getMessage(), e);
                }
            });

        }

        checker.shutdown();
        logger.info("Ended work: " + totalReport.asString());
        logger.info("The log was saved to " + log.getAbsolutePath());
    }

    static private CommandLine setupOptions(String[] args) {
        Options options = new Options();
        options.addOption(Option
                .builder("u")
                .longOpt("user")
                .desc("Swift user")
                .hasArg()
                .required()
                .build()
        );
        options.addOption(Option
                .builder("p")
                .longOpt("password")
                .desc("Swift password")
                .hasArg()
                .required()
                .build()
        );
        options.addOption(Option
                .builder("a")
                .longOpt("authUrl")
                .desc("Swift auth url")
                .hasArg()
                .required()
                .build()
        );
        options.addOption(Option
                .builder("c")
                .longOpt("concurrency")
                .desc("Number of parallel thread")
                .hasArg()
                .required()
                .build()
        );
        options.addOption(Option
                .builder("l")
                .longOpt("log")
                .desc("Output file log name")
                .hasArg()
                .required()
                .build()
        )
        ;
        final var parser = new DefaultParser();
        final CommandLine commandLine;
        try {
            commandLine = parser.parse(options, args);
        } catch (ParseException exp) {
            logger.error("Parsing failed.  Reason: " + exp.getMessage());
            return null;
        }
        return commandLine;
    }

}
