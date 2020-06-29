const fs = require('fs');
const d3 = require('../libs/d3.min.js');
//const planck = require('../libs/planck.min.js');
const bubbletreemap = require('../libs/bubbletreemap.js');
const hierarchyData = require('../hierarchy/hierarchyData.js');

(function Main(){

    function LOG(msg, depth){
        let tab = "  ".repeat(depth);
        console.log(tab+" - "+msg)
    }

    let topicFile, mapFile, sizeId, sizeScale, isMain;
    let topicsData;

    function processArgs(){
        LOG("processing arguments", 1)
        let args = process.argv.slice(2);
        topicFile = args[0];
        mapFile = args[1];
        isMain = (args[2] === "true")
        sizeId = args[3] || "";
        sizeScale = JSON.parse(args[4]) || [5,40];
        LOG("mapping topics from "+topicFile+" to "+mapFile, 2);
    }

    function readTopics(){
        LOG("reading topics", 1);
        topicsData = JSON.parse(fs.readFileSync(topicFile));
    }

    function buildMap(topics, depth=2){
        if(topics.topics.length == 0){
            LOG("Empty topic group, returning empty map", depth)
            return {
               "topics": [],
               "bubbleMapBorder": []
           }
        }
        LOG("generating hierarchy", depth);
        let hData = hierarchyData.make(topics, sizeId);
        LOG("generating bubble map", depth);
        console.log(sizeScale)
        let bubbleSizeScale = d3.scaleLinear()
            .domain([1, d3.max(hierarchyData.getSizes(hData))])
            .range(sizeScale);
        let root = d3.hierarchy(hData)
            .sum(d => bubbleSizeScale(d.size))
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
        LOG("building map data", depth);
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
            LOG("saving map data", 2);
            fs.writeFileSync(mapFile, JSON.stringify(data));
        } catch(err){
            console.error(err);
        }
    }

    function buildMainMap(){
        LOG("building main map", 1);
        let mapData = buildMap(topicsData);
        saveMap(mapData);
    }

    function buildSubMap(){
        LOG("building sub maps", 1);
        let mapsArray = [];
        for(let group of topicsData.subTopicGroups){
            LOG("building sub map "+group.mainTopicId, 2)
            let mapData = buildMap(group, 3);
            mapsArray.push({
                "subMap": mapData,
                "mainTopicId": group.mainTopicId
            })
        }
        saveMap(mapsArray);
    }

    function start(){
        LOG("Node JS - starting", 0);
        processArgs();
        readTopics();
        if(isMain){
            buildMainMap();
        } else {
            buildSubMap();
        }
        LOG("Node JS - finished", 0);
    }

    start();
})()