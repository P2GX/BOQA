package org.p2gx.boqa.cli;

import org.p2gx.boqa.cli.cmd.BoqaBenchmarkCommand;
import org.p2gx.boqa.cli.cmd.BlendedBenchmarkCommand;
import org.p2gx.boqa.cli.cmd.DownloadCommand;
import picocli.CommandLine;
import java.util.concurrent.Callable;
import static picocli.CommandLine.Help.Ansi.Style.*;

/**
 * Main entry point for the BOQA (Bayesian Ontology Query Analysis) command-line application.
 * <p>
 * This class configures and executes the CLI using PicoCLI framework. It sets up the main command
 * with three subcommands:
 * <ul>
 *   <li>{@code download} - Downloads disease and phenotype data</li>
 *   <li>{@code plain} - Runs BOQA benchmark analysis using plain scoring</li>
 *   <li>{@code blended} - Runs BOQA benchmark analysis using blended scoring (work in progress)</li>
 * </ul>
 * The application matches phenotypic features observed in patients with annotated 
 * disease-phenotype associations.
 * </p>
 *
 * @version v0.1.0
 */
@CommandLine.Command(name = "boqa",
        header = "Bayesian Ontology Query Analysis (BOQA)\n",
        mixinStandardHelpOptions = true,
        usageHelpWidth = Main.WIDTH,
        version = Main.VERSION,
        footer = Main.FOOTER)
public class Main implements Callable<Integer> {

    public static final String VERSION = "v0.1.0";
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