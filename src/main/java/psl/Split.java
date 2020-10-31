package psl;

import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdfwriter.COSWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import picocli.CommandLine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

@CommandLine.Command(name = "split", mixinStandardHelpOptions = true,
        description = "Split PDF documents according to specifications", version = "0.1.0")
public class Split implements Callable<Integer> {
    @CommandLine.Parameters(index = "0", description = "input file")
    private File file;

    @CommandLine.Option(names = {"-s", "--start-page"}, description = "start page")
    private Integer startPage;

    @CommandLine.Option(names = {"-e", "--end-page"}, description = "end page")
    private Integer endPage;

    @CommandLine.Option(names = {"-p", "--split"}, description = "split at page")
    private Integer split;

    private Splitter splitter;

    private void setSplittingOptions() {
        splitter = new Splitter();
        if (split != null) {
            splitter.setSplitAtPage(split);
        }
        if (startPage != null) {
            splitter.setStartPage(startPage);
        }
        if (endPage != null) {
            splitter.setEndPage(endPage);
        }
    }

    private String getBasename(String filePath) {
       String[] separators = filePath.split(File.pathSeparator);
       return separators[separators.length - 1];
    }

    private String getExtension(String basename) {
        if (!basename.contains(".")) {
            return null;
        }
        String[] components = basename.split("\\.");
        return components[components.length - 1];
    }

    private String getFileNamePrefix(String filename) {
        String[] components = filename.split("\\.");
        return String.join(".", Arrays.copyOfRange(components, 0, components.length - 1));
    }

    private class SubDocument {
        private final String basename;
        private final String extension;
        private final String prefix;

        public SubDocument(String filePath) {
            this.basename = getBasename(filePath);
            this.extension = getExtension(basename);
            this.prefix = getFileNamePrefix(basename);
        }

        public String getSuffixedName(int suffixIndex) {
            if (prefix == null) {
                return String.format("%s-%d", basename, suffixIndex);
            }
            return String.format("%s-%d.%s", basename, suffixIndex, extension);
        }
    }

    private void writeDocument(PDDocument document, String filename) {
        try (FileOutputStream output = new FileOutputStream(filename);
             COSWriter cosWriter = new COSWriter(output)) {
            cosWriter.write(document);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error writing document %s", filename), e);
        }
    }

    @Override
    public Integer call() throws Exception {
        String filePath = file.getAbsolutePath();
        if (!file.exists()) {
            System.err.printf("No such file: %s\n", filePath);
            return 2;
        }

        PDDocument document = PDDocument.load(file);

        setSplittingOptions();

        List<PDDocument> documents = splitter.split(document);
        IntStream.range(0, documents.size()).forEach(index -> {
            var subDocument = documents.get(index);
            var outputName = new SubDocument(filePath).getSuffixedName(index + 1);
            writeDocument(subDocument, outputName);
            System.out.printf("Wrote output %s\n", outputName);
        });

        return 0;
    }
}
