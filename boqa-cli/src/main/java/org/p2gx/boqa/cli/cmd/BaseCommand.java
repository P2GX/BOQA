package org.p2gx.boqa.cli.cmd;

import picocli.CommandLine;

public class BaseCommand {

    @CommandLine.Option(
            names = {"-x", "--out-prefix"},
            description = "Common prefix for all generated files, which can also contain the path."
    )
    protected String outPrefix = "JABOQA";

}