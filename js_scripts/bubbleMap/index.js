const fs = require('fs');
const d3 = require('../libs/d3.min.js');
//const planck = require('../libs/planck.min.js');
const bubbletreemap = require('../libs/bubbletreemap.js');
const hierarchyData = require('../hierarchy/hierarchyData.js');

(function Main(){

    let topicFile, mapFile, sizeId, isMain;
    let topicsData;

    function processArgs(){
        let args = process.argv.slice(2);
        topicFile = args[0];
        mapFile = args[1];
        isMain = (args[2] === "true")
        sizeId = args[3] || "";
    }

    function readTopics(){
        console.log("reading topics");
        topicsData = JSON.parse(fs.readFileSync(topicFile));
    }

    function buildMap(topics){
        console.log("generating hierarchy");
        let hData = hierarchyData.make(topics, sizeId);
        console.log("generating bubble map");
        let sizeScale = d3.scaleLinear()
            .domain([1, d3.max(hierarchyData.getSizes(hData))])
            .range([5, 40]);
        let root = d3.hierarchy(hData)
            .sum(d => sizeScale(d.size))
            .sort((a, b) => {return b.value - a.value});

        let bubbleTreeMap = bubbletreemap()
            .padding(1)
            .curvature(8)
            .hierarchyRoot(root)
            .width(1000)
            .height(1000);

        let bubbleMapData = bubbleTreeMap
            .doLayout()
            .hierarchyRoot();

        let bubblesData = bubbleMapData.descendants().filter(c=>{
            return !c.children;
        });
        let bordersData = bubbleTreeMap.getContour().filter(a =>{
            return a.strokeWidth > 0;
        });
        console.log("building map data");
        let tmpTopics = bubblesData.map(d=>{
            return {
                "topicId": d.data.topicData.topicId,
                "clusterId": d.data.topicData.clusterId,
                "size": d.data.size,
                "labels": d.data.topicData.topWords,
                "bubbleMap": {
                    "r": d.r,
                    "cx": d.x,
                    "cy": d.y
                }
            }
        });

        let mapData = {
            "topics": tmpTopics,
            "bubbleMapBorder": bordersData
        }

        return mapData;
    }

    function saveMap(data){
        try{
            console.log("saving map");
            fs.writeFileSync(mapFile, JSON.stringify(data));
        } catch(err){
            console.error(err);
        }
    }

    function buildMainMap(){
        console.log("Building main map");
        let mapData = buildMap(topicsData);
        saveMap(mapData);
    }

    function buildSubMap(){
        console.log("Building sub maps");
        let mapsArray = [];
        for(let group of topicsData.subTopicGroups){
            let mapData = buildMap(group);
            mapsArray.push({
                "subMap": mapData,
                "mainTopicId": group.mainTopicId
            })
        }
        saveMap(mapsArray);
    }

    function start(){
        console.log("Node JS - starting");
        processArgs();
        console.log("mapping topics from "+topicFile+" to "+mapFile);
        readTopics();
        if(isMain){
            buildMainMap();
        } else {
            buildSubMap();
        }
        console.log("Node JS - finished!");
    }

    start();
})()