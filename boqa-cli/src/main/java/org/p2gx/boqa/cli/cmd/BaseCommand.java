package org.p2gx.boqa.cli.cmd;

import picocli.CommandLine;

/**
 * Base class for CLI commands, providing shared options used by subcommands.
 * <p>
 * Currently exposes a common output prefix option for generated files.
 * </p>
 */
public class BaseCommand {

    @CommandLine.Option(
            names = {"-x", "--out-prefix"},
            description = "Common prefix for all generated files, which can also contain the path."
    )
    protected String outPrefix = "BOQA";

}