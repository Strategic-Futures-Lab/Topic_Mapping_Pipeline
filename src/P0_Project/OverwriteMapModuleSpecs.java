package P0_Project;

import org.json.simple.JSONObject;

public class OverwriteMapModuleSpecs {

    /** Filename for JSON Distribution file, from which to overwrite main map */
    public String mainDistribFile;
    /** Filename for JSON Distribution file, from which to overwrite sub maps, optional, defaults to "" */
    public String subDistribFile = "";
    /** Flag for overwriting the sub maps, defaults to false if subDistribFile = "" */
    public boolean overwriteSubMaps = false;
    /** Filename for JSON main map file to overwrite */
    public String mainMapFile;
    /** Filename for JSON sub maps file to overwrite, only required if subDistribFile != "" */
    public String subMapsFile = "";
    /** Name of distribution to use if overwriting the size value of topics, optional, defaults to "" */
    public String sizeName = "";
    /** Flag for overwriting the size value of topics, defaults to false if sizeName = "" */
    public boolean overwriteSize = false;
    /** Flag for overwriting the labels of topics, optional, defaults to false */
    public boolean overwriteLabels = false;
    /** Filename for JSON main map file to save after edit */
    public String mainMapOutput;
    /** Filename for JSON sub maps file to save after edit, only required if subDistribFile != "" */
    public String subMapsOutput = "";

    public OverwriteMapModuleSpecs(JSONObject specs, MetaSpecs metaSpecs){
        mainDistribFile = metaSpecs.getDataDir() + (String) specs.get("mainDistribution");
        mainMapFile = metaSpecs.getSourceDir() + (String) specs.get("mainMap");
        mainMapOutput = metaSpecs.getOutputDir() + (String) specs.get("mainMapOutput");
        subDistribFile = (String) specs.getOrDefault("subDistribution", "");
        if(subDistribFile.length() > 0){
            subDistribFile = metaSpecs.getDataDir() + subDistribFile;
            subMapsFile = metaSpecs.getSourceDir() + (String) specs.get("subMaps");
            subMapsOutput = metaSpecs.getOutputDir() + (String) specs.get("subMapsOutput");
            overwriteSubMaps = true;
        }
        sizeName = (String) specs.getOrDefault("sizeName", "");
        overwriteSize = sizeName.length() > 0;
        overwriteLabels = (boolean) specs.getOrDefault("overwriteLabels", false);
    }
}
