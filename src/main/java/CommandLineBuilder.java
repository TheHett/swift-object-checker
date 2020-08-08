import org.apache.commons.cli.*;

public class CommandLineBuilder {

    public static CommandLine build(String[] args) throws ParseException {
        Options options = new Options()
                .addOption(Option
                        .builder("m")
                        .longOpt("mode")
                        .required()
                        .desc("Mode (collect or delete)")
                        .hasArg()
                        .build()
                )
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
                        .required(false)
                        .build()
                )
                .addOption(Option
                        .builder("l")
                        .longOpt("log")
                        .desc("Output file log name")
                        .hasArg()
                        .required()
                        .build()
                )
                .addOption(Option
                        .builder("r")
                        .longOpt("replaceLog")
                        .desc("Replace log file")
                        .required(false)
                        .build()
                );
        return new DefaultParser().parse(options, args);
    }
}
