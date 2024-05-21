package input;

import IO.Console;
import IO.Timer;
import config.Project;
import config.modules.CorpusPDF;
import data.Pair;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

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
    private int splitPages;

    // Flag for processing PDFs in parallel (may affect order of documents)
    private final static boolean RUN_IN_PARALLEL = true;

    /**
     * Main module method - processes parameters, reads PDF file/folder and writes JSON corpus
     * @param moduleParameters module parameters
     * @param projectParameters project meta parameters
     * @throws IOException If the file(s) type is unexpected
     */
    public static void run(CorpusPDF moduleParameters, Project projectParameters) throws IOException {
        String MODULE_NAME = moduleParameters.moduleName+" ("+moduleParameters.moduleType+")";
        Console.moduleStart(MODULE_NAME);
        Timer.start(MODULE_NAME);
        PDFInput instance = new PDFInput();
        instance.processParameters(moduleParameters, projectParameters);
        instance.extension = ".pdf";
        try {
            instance.findFiles();
            instance.loadFiles(instance::loadPDF, RUN_IN_PARALLEL);
            instance.writeJSON();
        } catch (Exception e) {
            Console.moduleFail(MODULE_NAME);
            throw e;
        }
        Console.moduleComplete(MODULE_NAME);
        Timer.stop(MODULE_NAME);
    }

    // processes project and module parameters
    private void processParameters(CorpusPDF moduleParameters, Project projectParameters){
        Console.log("Processing parameters");
        source = projectParameters.sourceDirectory+moduleParameters.source;
        outputFile = projectParameters.dataDirectory+moduleParameters.output;
        splitPages = moduleParameters.splitPages;
        Console.tick();
        Console.info("Reading PDF input from "+source+" and saving to "+outputFile, 1);
    }

    // process one .pdf file
    private void loadPDF(Pair<File, String> pdf){
        File file = pdf.getLeft();
        String directory = pdf.getRight();
        String rootName = file.getName();
        Console.log("Processing: "+rootName, 1);
        rootName = rootName.substring(0, rootName.lastIndexOf('.'));
        try {
            // load file
            PDDocument doc = PDDocument.load(file);
            PDFTextStripper s = new PDFTextStripper();
            HashMap<String, String> docFields = new HashMap<>();
            int subDocCount = 0;
            if(splitPages<1){
                // read PDF in single block
                String text = s.getText(doc);
                docFields.put("text", text);
                docFields.put("folder", directory);
                docFields.put("file", rootName);
                addDocument(docFields);
            } else {
                // read PDF by block of pages
                for(int pageStart = 1; pageStart<doc.getNumberOfPages(); pageStart+=splitPages){
                    s.setStartPage(pageStart);
                    int pageEnd = Math.min(pageStart+splitPages+1, doc.getNumberOfPages());
                    s.setEndPage(pageEnd);
                    String text = s.getText(doc);
                    docFields.put("text", text);
                    docFields.put("folder", directory);
                    docFields.put("file", rootName);
                    docFields.put("subDoc", Integer.toString(subDocCount));
                    docFields.put("pages", pageStart+"-"+pageEnd);
                    addDocument(docFields);
                    subDocCount++;
                }

            }
            Console.tick();
            if(subDocCount>0){
                Console.note("Number of sub-documents extracted: " + subDocCount, 2);
            }
        } catch (Exception e) {
            Console.error("Could not read pdf file - skipping");
        }
    }
}
