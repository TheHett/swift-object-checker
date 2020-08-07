import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.javaswift.joss.model.Container;


import java.io.*;
import java.nio.charset.StandardCharsets;

public class Application {

    private final static Logger logger = Logger.getLogger(Application.class);

    public static void main(String[] args) throws IOException {

        final CommandLine commandLine;
        try {
            commandLine = setupOptions(args);
        } catch (ParseException e) {
            logger.error("Parsing failed.  Reason: " + e.getMessage());
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
        if (log.exists() && !commandLine.hasOption("replaceLog")) {
            logger.error("Log " + log.getAbsolutePath() + " file already exists");
            return;
        }

        try (var writer = new PrintWriter(log, StandardCharsets.UTF_8)) {
            for (Container container : account.list()) {
                final var report = checker.check(container, (object) -> {
                    synchronized (Application.class) {
                        writer.println(container.getName() + "/" + object.getName());
                    }
                });
                totalReport.appendFrom(report);
            }
        }

        checker.shutdown();
        logger.info("Ended work: " + totalReport.asString());
        logger.info("The log was saved to " + log.getAbsolutePath());
    }

    static private CommandLine setupOptions(String[] args) throws ParseException {
        Options options = new Options()
                .addOption(Option
                        .builder("u")
                        .longOpt("user")
                        .desc("Swift user")
                        .hasArg()
                        .required()
                        .build()
                )
                .addOption(Option
                        .builder("p")
                        .longOpt("password")
                        .desc("Swift password")
                        .hasArg()
                        .required()
                        .build()
                )
                .addOption(Option
                        .builder("a")
                        .longOpt("authUrl")
                        .desc("Swift auth url")
                        .hasArg()
                        .required()
                        .build()
                )
                .addOption(Option
                        .builder("c")
                        .longOpt("concurrency")
                        .desc("Number of parallel threads")
                        .hasArg()
                        .required()
                        .build()
                )
                .addOption(Option
                        .builder("l")
                        .longOpt("log")
                        .desc("Output file log name")
                        .hasArg()
                        .required()
                        .build()
                ).addOption(Option
                        .builder("r")
                        .longOpt("replaceLog")
                        .desc("Replace log file")
                        .required()
                        .build()
                );
        return new DefaultParser().parse(options, args);
    }

}
