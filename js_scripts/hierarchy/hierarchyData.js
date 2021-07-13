module.exports = function(){

    function exploreNode(node, nTopics, topics, table){
        if(node >= nTopics){ // if not leaf
            return { // return object with recursive exploration
                uncertainty: 0,
                leaf: false,
                children:[
                    exploreNode(table[node-nTopics].node1, nTopics, topics, table),
                    exploreNode(table[node-nTopics].node2, nTopics, topics, table)
                ]
            }
        } else { // if leaf node
            return { // return leaf object
                uncertainty: 0,
                leaf: true,
                topicData: topics[node]
            }
        }
    }

    function makeHierarchy(topicData){
        // sort topics and remove generic ones (clusterId < 0)
        let topicsFull = topicData.topics;
        topicsFull.sort((a,b)=>{return a.topicId - b.topicId});
        let topics = topicsFull.filter(e=>{return parseInt(e.clusterId) >= 0});
        let nTopics = topics.length;
        // get linkage table and top node index
        let table = topicData.linkageTable;
        let topNodeIdx = table.length-1;
        // return hierarchy from exploring top node
        return exploreNode(topNodeIdx+nTopics, nTopics, topics, table);
    }

    function getClusters(node){
        if(node.leaf){ // if leaf
            return node.topicData.clusterId; // return clusterId from leaf
        } else { // else
            // get cluster ids from children nodes
            let c1 = getClusters(node.children[0]),
                c2 = getClusters(node.children[1]);
            if(c1 == c2){ // if same cluster ids
                if(c1 !== null){ // if cluster id not-null
                    node.uncertainty = 1; // set uncertainty to 1 (border)
                    node.children[0].uncertainty = 0; // reset uncertainty of children (no-norder
                    node.children[1].uncertainty = 0;
                    node.cluster = c1; // get cluster id from child
                }
                return c1; // return child id in any case
            } else { // if children cluster ids are different
                return null; // return null
            }
        }
    }

    function addSizes(node, sizeId){
        if(node.leaf){
            let s = node.topicData.totals.filter(t=>t.id == sizeId)
            if(s.length > 0){
                node.size = s[0].weight;
            } else {
                node.size = 1;
            }
        } else {
            addSizes(node.children[0], sizeId);
            addSizes(node.children[1], sizeId);
        }
    }

    let public = {};

    public.make = function(topicData, sizeId){
        let hierarchy = makeHierarchy(topicData);
        getClusters(hierarchy);
        addSizes(hierarchy, sizeId);
        return hierarchy;
    }

    public.getSizes = function(hierarchy){
        function getSizeNode(node, sizeArray){
            if(node.leaf){
                sizeArray.push(node.size);
            } else {
                getSizeNode(node.children[0], sizeArray);
                getSizeNode(node.children[1], sizeArray);
            }
        }
        let sizes = [];
        getSizeNode(hierarchy, sizes);
        return sizes;
    }

    return public;
}();