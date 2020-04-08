
const hexmapDataUrl = './data/ScottishAllSince2014_60T_Map.json',
    distributionDataUrl = './data/ScottishAllSince2014_60T_Distrib.json',
   

const path_to_subModels_dir = "./data/SE-V2Feb20/SE_UpdatedSubModels",
    sub_bubbleMaps_data = "./data/SE-V2Feb20/bubbleData/subBubbleMapsData.json",
    num_sup_topics = 60;
	
const Main = require('./main');
const main = new Main();

//main.makeBubbleData(hexmapDataUrl, distributionDataUrl);

main.makeBubbleData_hierarchical(path_to_subModels_dir, sub_bubbleMaps_data, num_sup_topics);