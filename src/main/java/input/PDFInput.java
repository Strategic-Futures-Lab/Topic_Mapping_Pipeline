package input;

import IO.Console;
import IO.Timer;
import data.Pair;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import pipeline.config.ModuleConfig;
import pipeline.config.modules.InputConfigPDF;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Module generating a corpus from a PDF file or directory of PDF files
 *
 * @author T. Methven, P. Le Bras, A. Gharavi
 * @version 3
 */
public class PDFInput extends FileInput {

    // module parameters
    private final InputConfigPDF config;

    // Flag for processing PDFs in parallel (may affect order of documents)
    private final static boolean RUN_IN_PARALLEL = true;

    private PDFInput(InputConfigPDF c){
        config = c;
        config.logConfig();
        corpus.name = config.corpusName;
    }

    /**
     * Main module method - processes parameters, reads PDF file/folder and writes JSON corpus
     * @param moduleParameters module parameters
     * @throws IOException If the file(s) type is unexpected
     */
    public static void run(ModuleConfig moduleParameters) throws IOException {
        String MODULE_NAME = moduleParameters.moduleName+" ("+moduleParameters.moduleType+")";
        Console.moduleStart(MODULE_NAME);
        Timer.start(MODULE_NAME);
        PDFInput instance = new PDFInput((InputConfigPDF) moduleParameters);
        instance.extension = ".pdf";
        try {
            instance.findFiles(instance.config.sourceFile);
            instance.loadFiles(instance::loadPDF, RUN_IN_PARALLEL);
            instance.writeCorpus(instance.config.outputFile);
        } catch (Exception e) {
            Console.moduleFail(MODULE_NAME);
            throw e;
        }
        Console.moduleComplete(MODULE_NAME);
        Timer.stop(MODULE_NAME);
    }

    // process one .pdf file
    private void loadPDF(Pair<File, String> pdf){
        File file = pdf.getLeft();
        String directory = pdf.getRight();
        String rootName = file.getName().substring(0, file.getName().lastIndexOf('.'));
        try {
            // load file
            PDDocument doc = PDDocument.load(file);
            PDFTextStripper s = new PDFTextStripper();
            HashMap<String, String> docFields = new HashMap<>();
            int subDocCount = 0;
            if(config.splitPages<1){
                // read PDF in single block
                String text = cleanText(s.getText(doc));
                docFields.put("text", text);
                docFields.put("folder", directory);
                docFields.put("file", rootName);
                addDocument(docFields);
            } else {
                // read PDF by block of pages
                for(int pageStart = 1; pageStart<doc.getNumberOfPages(); pageStart+=config.splitPages){
                    s.setStartPage(pageStart);
                    int pageEnd = Math.min(pageStart+config.splitPages-1, doc.getNumberOfPages());
                    s.setEndPage(pageEnd);
                    String text = cleanText(s.getText(doc));
                    docFields.put("text", text);
                    docFields.put("folder", directory);
                    docFields.put("file", rootName);
                    docFields.put("subDoc", Integer.toString(subDocCount));
                    docFields.put("pages", pageStart+"-"+pageEnd);
                    addDocument(docFields);
                    subDocCount++;
                }

            }
            Console.log(file.getName()+" read successfully"+(subDocCount>0?" ("+subDocCount+" sub-documents)":""), 1);
        } catch (Exception e) {
            Console.error("Could not read "+file.getName()+" - skipping", 1);
        }
    }

    // fixes typical issues with reading pdf text (new lines, split words)
    private String cleanText(String in){
        return in.replaceAll("-\n","").replaceAll("\n", " ");
    }
}
