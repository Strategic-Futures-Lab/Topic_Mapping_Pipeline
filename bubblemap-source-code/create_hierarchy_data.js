
class Create_hierarchy_data {
	createHierarchyData(mapData, distributionData){
		// make the hierarchy
		let hierarchy = this.makeHierarchy(mapData);
		// add the cluster and uncertainty information
		this.getClusters(hierarchy);
		// add the size information to the topics entries
		this.addSizes(hierarchy, distributionData);
		// return the hierarchy object
		return hierarchy
	}
	
	makeHierarchy(data){
		// we get the full topics list, sort it and remove generic topics (cluster ID negative)
		let topicsFull = data.conceptsData;
		topicsFull.sort((a,b)=>{return a.conceptId - b.conceptId})
		let topics = topicsFull.filter(e=>{return e.clusterId >= 0})
		let nTopics = topics.length;
	 
		// we get the linkage table and top node index
		let table = data.linkageTable;
		let topNodeIdx = table.length-1;
	 
		// call the recursive function explore node to create the hierarchy and return
		return this.exploreNode(topNodeIdx+nTopics, nTopics, topics, table);
	}
	
	exploreNode(node, nTopics, topics, table){
		if(node >= nTopics){
			// if current explored node points to linkageTable
			// return a new hierarchy node, and recursively explore children
			return {
				uncertainty: 0,
				leaf: false,
				children: [this.exploreNode(table[node-nTopics].node1, nTopics, topics, table),
						   this.exploreNode(table[node-nTopics].node2, nTopics, topics, table)]
			}
		} else {
			// else return a leaf node with topic data
			return {
				uncertainty: 0,
				leaf:true,
				topicData: topics[node]
			};
		}
	}
	
	getClusters(node){
		if(node.leaf){
			// if node is a leaf simply return the cluster id
			return node.topicData.clusterId;
		} else {
			// otherwise get the cluster of both children nodes
			let c1 = this.getClusters(node.children[0]),
				c2 = this.getClusters(node.children[1]);
			if(c1 === c2){
				// if the children clusters have the same value
				if(c1 !== null){
					// and if that value is non-null (i.e. all children underneath belong in the same cluster)
					// set the current nodes' uncertainty to 1 (to draw path around it later)
					node.uncertainty = 1;
					// the children uncertainty would have been set to 1, so reset ot 0
					node.children[0].uncertainty = 0;
					node.children[1].uncertainty = 0;
					// attach the cluster value at the current node's level
					node.cluster = c1;
				}
				// in any case return the children cluster value as the current node cluster value
				return c1;
			} else {
				// if the children node are from different clusters, return null
				return null;
			}
		}
	}
	
	addSizes(node, distribution){
		if(node.leaf){
			// if the current node is a leaf node
			// then gets it's distribution entry and sum the values in it
			// and set the node's size value to this sum
			node.size = distribution.filter(e=>{
				return e.conceptId === node.topicData.conceptId;
			})[0].topicData.reduce((v,e)=>{
				return v+e.numberOfDocuments;
			}, 0)
		} else {
			// otherwise explore the children nodes
			this.addSizes(node.children[0], distribution);
			this.addSizes(node.children[1], distribution);
		}
	}
	// Accssors functions
	getSizes(hierarchy){
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
	
}


module.exports = Create_hierarchy_data;
