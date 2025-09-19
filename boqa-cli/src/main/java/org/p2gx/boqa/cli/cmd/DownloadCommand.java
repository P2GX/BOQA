package org.p2gx.boqa.cli.cmd;

import org.monarchinitiative.biodownload.BioDownloader;
import org.monarchinitiative.biodownload.BioDownloaderBuilder;
import org.monarchinitiative.biodownload.FileDownloadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Callable;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.ZipFile;

/**
 * Download a number of files needed for the analysis. We download by default to a subdirectory called
 * {@code data}, which is created if necessary. We download the files {@code hp.json}, {@code phenotype.hpoa},
 * and {@code mim2gene_medgen}.
 * @author <a href="mailto:peter.hansen@bih-charite.de">Peter Hansen</a>
 */

@CommandLine.Command(name = "download",
        mixinStandardHelpOptions = true,
        description = "Downloads required data.",
        sortOptions = false, usageHelpWidth = 200)
public class DownloadCommand implements Callable<Integer>{
    private static final Logger logger = LoggerFactory.getLogger(DownloadCommand.class);

    @CommandLine.Option(
            names={"-h","--hpo-release-tag"},
            description = "A valid release tag from:\nhttps://github.com/obophenotype/human-phenotype-ontology/releases.\n" +
                    "Examples: v2025-05-06, v2025-03-03, or latest (default: ${DEFAULT-VALUE})",
            defaultValue = "latest")
    public String hpoReleaseTag;

    @CommandLine.Option(
            names={"-p","--phenopacket-store-release-tag"},
            description = "A valid release tag from:\nhttps://github.com/monarch-initiative/phenopacket-store/releases.\n" +
                    "Examples: 0.1.24, 0.1.23, or latest (default: ${DEFAULT-VALUE})",
            defaultValue = "latest")
    public String phenopacketStoreReleaseTag;

    @CommandLine.Option(
            names={"-d","--data-dir"},
            description = "Destination directory for downloaded files (default: ${DEFAULT-VALUE}). " +
                    "The files are downloaded to a subdirectory named after the release tag.",
            defaultValue = "./data")
    public String datadir;

    @CommandLine.Option(
            names={"-w","--overwrite"},
            description = "Overwrite previously downloaded files (default: ${DEFAULT-VALUE})")
    public boolean overwrite = true;

    @Override
    public Integer call() throws FileDownloadException, MalformedURLException {
        logger.info(String.format("Download analysis to %s", datadir));

        // Download from human-phenotype-ontology
        Path destination;
        String base_url;
        if (hpoReleaseTag.equals("latest")){
            String timeStamp = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime());
            destination = Paths.get(datadir + "/human-phenotype-ontology/latest_" + timeStamp);
            base_url = "https://github.com/obophenotype/human-phenotype-ontology/releases/latest/download/";
        } else {
            destination = Paths.get(datadir + "/human-phenotype-ontology/" + hpoReleaseTag);
            base_url = "https://github.com/obophenotype/human-phenotype-ontology/releases/download/" + hpoReleaseTag + "/";
        }

        System.out.println("Destination directory: " + destination);

        List<String> file_names = List.of("phenotype.hpoa", "hp.json", "genes_to_disease.txt");
        BioDownloaderBuilder builder;
        for (String file_name : file_names) {
            System.out.println("FILE: " + file_name);
            URL url = new URL(base_url + file_name);
            System.out.println("URL: " + url);
            builder = BioDownloader.builder(destination);
            builder.overwrite(overwrite);
            builder.custom(url);
            BioDownloader downloader = builder.build();
            downloader.download();
        }

        // Download from phenopacket-store
        if (phenopacketStoreReleaseTag.equals("latest")){
            String timeStamp = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime());
            destination = Paths.get(datadir + "/phenopacket-store/latest_" + timeStamp);
            base_url = "https://github.com/monarch-initiative/phenopacket-store/releases/latest/download/";
        } else {
            destination = Paths.get(datadir + "/phenopacket-store/" + phenopacketStoreReleaseTag);
            base_url = "https://github.com/monarch-initiative/phenopacket-store/releases/download/" + phenopacketStoreReleaseTag + "/";
        }

        System.out.println("FILE: " + "all_phenopackets.zip");
        URL url = new URL(base_url + "all_phenopackets.zip");
        System.out.println("URL: " + url);
        builder = BioDownloader.builder(destination);
        builder.overwrite(overwrite);
        builder.custom(url);
        BioDownloader downloader = builder.build();
        downloader.download();
        try {
            ZipFile zipFile = new ZipFile(destination + "/all_phenopackets.zip");
            zipFile.extractAll(destination.toString());
        } catch (ZipException e) {
            e.printStackTrace();
        }
        return 0;
    }
}