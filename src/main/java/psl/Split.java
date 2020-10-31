package psl;

import org.apache.pdfbox.pdmodel.PDDocument;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "split", mixinStandardHelpOptions = true)
public class Split implements Callable<Integer> {
    @CommandLine.Parameters(index = "0", description = "input file")
    private File file;

    @Override
    public Integer call() throws Exception {
        if (!file.exists()) {
            System.err.printf("No such file: %s\n", file.getAbsolutePath());
            return 2;
        }
        PDDocument.load(file);
        return 0;
    }
}
