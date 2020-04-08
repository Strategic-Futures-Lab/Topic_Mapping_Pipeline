// JavaScript source code
const fs = require('fs');

var parse = require('csv-parse');

const path_to_subModels_dir = "./data/SE-V2Feb20/SE-V2Feb20-SubData";
const path_to_Updated_subModels_dir = "./data/SE-V2Feb20/SE_UpdatedSubModels/";

var csvData = [];
fs.createReadStream('./data/SE-V2Feb20/Input/SE_V2_Feb20assignment.csv')
    .pipe(parse({ delimiter: ',' }))
    .on('data', function (csvrow) {
          csvData.push(csvrow);
    })
    .on('end', function () {

        for (var i = 0; i < 60; i++) {
            let subTopicIds = [];

            subTopicIds =  csvData[i];

            console.log(subTopicIds);

            let subTopicModelDataUrl = path_to_subModels_dir + "/SE_V2_sub_map_" + i + ".json";
            let topicModelRawData = fs.readFileSync(subTopicModelDataUrl);
            let jsonFile = JSON.parse(topicModelRawData);




            let conceptsData = [];

            for (var j = 0; j < jsonFile.conceptsData.length; j++) {
                let t = jsonFile.conceptsData[j];
                conceptsData.push({

                    "conceptId": t.conceptId,
                    "clusterId": t.clusterId,
                    "hexCoordinates": t.hexCoordinates,
                    "labels": t.labels,
                    "subTopicId": parseInt(subTopicIds[t.conceptId])
                })
            }

            let updatedJSON = {
                "linkageTable": jsonFile.linkageTable,
                "conceptsData": conceptsData
            }

            //save json
            fs.writeFileSync(path_to_Updated_subModels_dir + "SE_V2_sub_map_" + i + ".json", JSON.stringify(updatedJSON));
        }
        
    });

//console.log(csvData[0])



   














