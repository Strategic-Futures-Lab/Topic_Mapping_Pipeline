package pipeline;

import IO.Console;
import config.ModuleConfig;
import config.ProjectConfig;
import config.ProjectConfigParser;
import config.modules.*;

import java.lang.reflect.InvocationTargetException;

public enum ModuleType {
    CSVInput (input.CSVInput.class, InputConfigCSV.class),
    TXTInput (input.TXTInput.class, InputConfigTXT.class),
    PDFInput (input.PDFInput.class, InputConfigPDF.class),
    HTMLInput (input.HTMLInput.class, InputConfigHTML.class),
    GTRInput (input.GTRInput.class, InputConfigGTR.class);

    public final Class module;
    public final Class config;

    ModuleType(Class mod, Class conf){
        module = mod;
        config = conf;
    }

    public void runModule(ModuleConfig moduleParams, ProjectConfig projectParams) throws RuntimeException {
        try {
            module.getMethod("run", ModuleConfig.class, ProjectConfig.class ).invoke(null, moduleParams, projectParams );
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            Console.error("Error while trying to execute the module " + this);
            throw new RuntimeException(e);
        }
    }

    public static ModuleType getType(String typeName) throws ProjectConfigParser.ParseException {
        switch (typeName){
            case "inputCSV": return CSVInput;
            case "inputTXT": return TXTInput;
            case "inputPDF": return PDFInput;
            case "inputHTML": return HTMLInput;
            case "inputGTR": return GTRInput;
            default:
                throw new ProjectConfigParser.ParseException("Module type \""+typeName+"\" is not recognised");
        }
    }
}
