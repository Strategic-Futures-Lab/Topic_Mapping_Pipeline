
const fs = require('fs');
const Create_hierarchy_data = require('./create_hierarchy_data');
const createHierarchyData = new Create_hierarchy_data();
const bb = require('./libs/bubbletreemap.js');

const D3Node = require('d3-node');
const d3 = require('./libs/d3.js');
const options = {d3Module: d3};
const d3n = new D3Node(options);

class Main{
	
    makeBubbleData(hexmapDataUrl, distributionDataUrl) {
       
		//Reading HexmapData and Distribution JSON files from URL
		let hexmapRawData = fs.readFileSync(hexmapDataUrl);
		let hexmapData = JSON.parse(hexmapRawData);
		
		let distribRawData = fs.readFileSync(distributionDataUrl);
		let distributionData = JSON.parse(distribRawData);

        console.log('****** Finished Reading Files **');
	
		// Create BubbleMapData
		let bubbleMapData = this.getBubbleMapData(hexmapData, distributionData);
		
		//Save to JSON file
        const path = "./data/SE-V2Feb20/bubbleData/bubblesData.json";
		this.storeData(bubbleMapData[0], bubbleMapData[1], path );		
    }


    //This function is used only for ScottishEnterprise project dempo. 60 to 1000 topics clustering
    // it stores all the sun_bubbleMaps in one JSON file 
    makeBubbleData_hierarchical(path_to_dir, output_file_name, num_sup_topics) {

        var jArray = [];
        
        for (var j = 0; j < 60; j++) {
            let subHexDataUrl = path_to_dir + "/SE_V2_sub_map_" + j + ".json";
            let subDistribDataUrl = path_to_dir + "/SE_V2_sub_distrib_" + j +".json";

            //Reading HexmapData and Distribution JSON files from URL
            let hexmapRawData = fs.readFileSync(subHexDataUrl);
            let hexmapData = JSON.parse(hexmapRawData);

            let distribRawData = fs.readFileSync(subDistribDataUrl);
            let distributionData = JSON.parse(distribRawData);

            console.log('****** Finished Reading Files **');

            // Create BubbleMapData
            let subBubbleMapData = this.getBubbleMapData(hexmapData, distributionData);

            let bData = subBubbleMapData[0];
            let cData = subBubbleMapData[1];

            

            try {
                var tempTopics = [];
                for (var i = 0; i < bData.length; i++) {

                   // console.log(bData[i].data.topicData);

                    tempTopics.push(
                        {
                            "topicId": bData[i].data.topicData.conceptId,
                            "subTopicId": bData[i].data.topicData.subTopicId,
                            "clusterId": bData[i].data.topicData.clusterId,
                            "size": bData[i].data.size,
                            "labels": bData[i].data.topicData.labels,
                            "hexMap": bData[i].data.topicData.hexCoordinates,
                            "bubbleMap": {
                                "r": bData[i].r,
                                "cx": bData[i].x,
                                "cy": bData[i].y,
                            }
                        })
                }

                console.log(tempTopics)

                var tempObj = {
                    "topics": tempTopics,
                    "bubbleMapBorder": cData
                }

            } catch (err) {
                console.error(err)
            }

            jArray.push({
                "sup_topicId": j,
                "sub_bubbleData": tempObj
            })

        }


      //  console.log(jArray[0]);

        fs.writeFileSync(output_file_name, JSON.stringify(jArray));

        }



	storeData(bData, cData, path){
        try {
            //console.log(bData[0]);
            var tempTopics = [];
            for (var i = 0; i < bData.length; i++) {
                tempTopics.push(
                {
                    "topicId": bData[i].data.topicData.conceptId,
                    "clusterId": bData[i].data.topicData.clusterId,
                    "size": bData[i].data.size,
                    "labels": bData[i].data.topicData.labels,
                    "hexMap": bData[i].data.topicData.hexCoordinates,
                    "bubbleMap": {
                        "r": bData[i].r,
                        "cx": bData[i].x,
                        "cy": bData[i].y,
                    }
                })
            }

            var tempObj = {
                "topics": tempTopics,
                "bubbleMapBorder" : cData
            }
            
            fs.writeFileSync(path, JSON.stringify(tempObj) );
		
	  } catch (err) {
		console.error(err)
	  }
	}

    getBubbleMapData(hexmapData, distributionData) {
		console.log('getBubbleMapData() started' );
        let hData = createHierarchyData.createHierarchyData(hexmapData, distributionData);
        let sizeScale = d3.scaleLinear()
            .domain([1, d3.max(createHierarchyData.getSizes(hData))])
            .range([5, 40]);
        let root = d3.hierarchy(hData)
            .sum(function (d) {
                return sizeScale(d.size);
            })
            .sort(function (a, b) { return b.value - a.value; });
			
        let bubbletreemap = bb.bubbletreemap()
            .padding(1)
            .curvature(8)
            .hierarchyRoot(root)
            .width(1000)
            .height(1000);
			
        let hierarchyData = bubbletreemap
            .doLayout()
            .hierarchyRoot();
			
        let bubblesData = hierarchyData.descendants().filter(function (candidate) {
            return !candidate.children;
        });
        let contourData = bubbletreemap.getContour().filter(arc => { return arc.strokeWidth > 0; })

         return [bubblesData , contourData];		 		
    }
}

module.exports  = Main;

