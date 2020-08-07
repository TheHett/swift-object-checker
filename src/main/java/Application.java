import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.javaswift.joss.model.Container;


import java.io.*;
import java.nio.charset.StandardCharsets;

public class Application {

    private final static Logger logger = Logger.getLogger(Application.class);

    public static void main(String[] args) throws IOException {
        final var cmd = setupOptions(args);
        if (cmd == null) {
            return;
        }

        final var swift = new SwiftService();
        final var checker = new ObjectCheckerService();
        checker.setConcurrency(Integer.parseInt(cmd.getOptionValue("c")));

        final var account = swift.authenticate(
                cmd.getOptionValue("u"),
                cmd.getOptionValue("p"),
                cmd.getOptionValue("a")
        );
        final var totalReport = new Report();

        final var logFile = new File(cmd.getOptionValue("l"));
        if (logFile.exists()) {
            logger.error("Log " + logFile.getAbsolutePath() + " file already exists");
            return;
        }

        try (var writer = new PrintWriter(logFile, StandardCharsets.UTF_8)) {
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
        logger.info("The log was saved to " + logFile.getAbsolutePath());
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
                .longOpt("logName")
                .desc("Output file log name")
                .hasArg()
                .required()
                .build()
        )
        ;
        final var parser = new DefaultParser();
        final CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException exp) {
            logger.error("Parsing failed.  Reason: " + exp.getMessage());
            return null;
        }
        return cmd;
    }

}
