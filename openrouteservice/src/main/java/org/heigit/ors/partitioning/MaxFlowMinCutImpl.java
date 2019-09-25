package org.heigit.ors.partitioning;

import com.graphhopper.storage.Graph;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.heigit.ors.partitioning.FastIsochroneParameters.*;


public class MaxFlowMinCutImpl extends MaxFlowMinCut {

    private Graph _graph;
    private EdgeExplorer _edgeExpl;
    private EdgeIterator _edgeIter;

    private Map<String, Integer> flowEdgeMap;

    private int maxEdgeId = -1;


    MaxFlowMinCutImpl(GraphHopperStorage ghStorage) {
        this._graph = ghStorage.getBaseGraph();
        this._edgeExpl = _graph.createEdgeExplorer();

        initStatics();
    }

    public void run(){
        //Need entries for all edges + one dummy edge for all nodes
        PartitioningData.createEdgeDataStructures(_graph.getAllEdges().getMaxId() + _graph.getNodes());
        PartitioningData.createNodeDataStructures(_graph.getNodes());
        buildStaticNetwork();
        pairEdges();
        freeMemory();
    }


    public void initStatics() {
        this.nodes = _graph.getNodes();
        this.flowEdgeMap = new HashMap<>();
        _dummyNodeId = _dummyEdgeId = -2;
    }

    public void buildStaticNetwork() {
        Set<Integer> targSet = new HashSet<>();

        for (int nodeId = 0; nodeId < nodes; nodeId++)
            addDummyEdgePair(nodeId);

        for (int baseId = 0; baseId < nodes; baseId++) {
            targSet.clear();
            _edgeIter = _edgeExpl.setBaseNode(baseId);
            while (_edgeIter.next()) {
                int targId = _edgeIter.getAdjNode();
                //>> eliminate Loops and MultiEdges
                if ((baseId != targId) && (!targSet.contains(targId))) {
                    targSet.add(targId);

                    this.flowEdgeMap.put(targId + "," + baseId, _edgeIter.getEdge());
                    addEdge(_edgeIter.getEdge(), INFL__GRAPH_EDGE_CAPACITY);
                }
            }
        }
    }

    private int addEdge(int edgeId, int capacity) {
        PartitioningData.flowEdgeDataMap.put(edgeId,
                new FlowEdgeData(0, capacity, -1, false));
        if(maxEdgeId < edgeId)
            maxEdgeId = edgeId;
        return edgeId;
    }


    private void pairEdges() {
        for (Map.Entry<String, Integer> entry : flowEdgeMap.entrySet()) {
            FlowEdgeData edgeData = PartitioningData.flowEdgeDataMap.get(entry.getValue());
            if (edgeData.inverse == -1) {
//                String[] ids = entry.getKey().split(",");
//                int baseId = Integer.parseInt(ids[0]);
//                int targId = Integer.parseInt(ids[1]);

//                int invEdge = flowEdgeMap.getOrDefault(targId + "," + baseId, -1);
//                if (invEdge == -1) {
//                    maxEdgeId++;
//                    invEdge = addEdge(getDummyEdgeId(), 0);
//                }
                int invEdgeId = getDummyEdgeId();
                edgeData.inverse = invEdgeId;
                FlowEdgeData invEdgeData = new FlowEdgeData(edgeData.flow, edgeData.capacity, entry.getValue(), false);

                PartitioningData.flowEdgeDataMap.put(entry.getValue(), edgeData);
                PartitioningData.flowEdgeDataMap.put(invEdgeId, invEdgeData);
            }
        }
    }

    public void addDummyEdgePair(int node) {
        FlowEdge forwEdge = new FlowEdge(getDummyEdgeId(), node, -1);
        FlowEdge backEdge = new FlowEdge(getDummyEdgeId(), -1, node);
        forwEdge.inverse = backEdge;
        backEdge.inverse = forwEdge;
        PartitioningData.dummyEdges.put(node, forwEdge);

        FlowEdgeData flowEdgeData = new FlowEdgeData(0, 0, backEdge.id, false);
        PartitioningData.flowEdgeDataMap.put(forwEdge.id, flowEdgeData);

        FlowEdgeData invFlowEdgeData = new FlowEdgeData(0, 0, forwEdge.id, false);
        PartitioningData.flowEdgeDataMap.put(backEdge.id, invFlowEdgeData);
    }


    public void freeMemory() {
        this.flowEdgeMap = null;
    }

}