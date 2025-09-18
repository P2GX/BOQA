package com.github.p2gx.boqa.cli;

import com.github.p2gx.boqa.cli.cmd.BoqaBenchmarkCommand;
import com.github.p2gx.boqa.cli.cmd.BlendedBenchmarkCommand;
import com.github.p2gx.boqa.cli.cmd.DownloadCommand;
import picocli.CommandLine;
import java.util.concurrent.Callable;
import static picocli.CommandLine.Help.Ansi.Style.*;

@CommandLine.Command(name = "boqa",
        header = "Bayesian Ontology Query Analysis (BOQA)\n",
        mixinStandardHelpOptions = true,
        usageHelpWidth = Main.WIDTH,
        version = Main.VERSION,
        footer = Main.FOOTER)
public class Main implements Callable<Integer> {

    public static final String VERSION = "v0.1.0-SNAPSHOT";
    public static final int WIDTH = 120;
    public static final String FOOTER = "The BOQA algorithm matches phenotypic features observed in patients " +
            "with annotated disease-phenotype associations.";

    private static final CommandLine.Help.ColorScheme COLOR_SCHEME = new CommandLine.Help.ColorScheme.Builder()
            .commands(bold, fg_blue, underline)
            .options(fg_yellow)
            .parameters(fg_yellow)
            .optionParams(italic)
            .build();

    public static void main(String[] args) {
        if (args.length == 0) {
            // if the user doesn't pass any command or option, add -h to show help
            args = new String[]{"-h"};
        }

        CommandLine cline = new CommandLine(new Main())
                .setColorScheme(COLOR_SCHEME)
                .addSubcommand("download", new DownloadCommand())
                .addSubcommand("plain", new BoqaBenchmarkCommand())
                .addSubcommand("blended", new BlendedBenchmarkCommand());
        cline.setToggleBooleanFlags(false);
        System.exit(cline.execute(args));
    }

    @Override
    public Integer call() throws Exception {
        // work done in subcommands
        return 0;
    }
}