package src.PreProcessing;

import java.util.ArrayList;
import java.util.Collections;

import src.Taxon.RealTaxon;
import src.Tree.Branch;

public class Component{

    public static class InternalNodeWithIndex{
        public InternalNode internalNode;
        public int index;


        public InternalNodeWithIndex(InternalNode a, int b){
            this.internalNode = a;
            this.index = b;
        }
    }


    public ArrayList<Component> parents;
    public ArrayList<Component> children;

    public ArrayList<InternalNodeWithIndex> partOfInternalNodes;
    
    public boolean isLeaf;
    public String label;

    public ArrayList<RealTaxon> realTaxaInComponent;

    // public Data data;
    public ArrayList<Data> dataList;

    // public boolean gainPartition;
    // public boolean onlyGainPartition;
    public double[] gainsForSubTreeSat, gainsForSubTreeVio;

    public int nodeCount;

    // public PartitionNode(ArrayList<PartitionNode> parents, ArrayList<PartitionNode> children, boolean isLeaf, Data data){
    //     this.parents = parents;
    //     this.children = children;
    //     this.isLeaf = isLeaf;
    //     this.data = data;
        
    // }

    public void initializeDataListForEachInternalNode(
        int dummyTaxonCount
    ){
        this.dataList = new ArrayList<>();
        for(int i = 0; i < this.partOfInternalNodes.size(); ++i){
            this.dataList.add(new Data());
        }
        for(Data d: this.dataList){
            d.branch = new Branch(dummyTaxonCount);
        }
    }

    public Component(boolean isLeaf){
        this.parents = new ArrayList<Component>();
        this.children = new ArrayList<Component>();
        this.isLeaf = isLeaf;
        this.partOfInternalNodes = new ArrayList<>();
        this.dataList = new ArrayList<>();
        // this.gainPartition = false;
        // this.onlyGainPartition = false;
        this.nodeCount = 0;
        this.realTaxaInComponent = null;
        
    }
    
    public void addChild(Component child){
        this.children.add(child);
    }

    public void addParent(Component parent){
        this.parents.add(parent);
    }

    public int addInternalNode(InternalNode p, int index){
        this.partOfInternalNodes.add(new InternalNodeWithIndex(p, index));
        return this.partOfInternalNodes.size() - 1;
    }


    @Override
    public String toString(){
        ArrayList<String> members = this.members();
        Collections.sort(members);

        StringBuilder sb = new StringBuilder();
        for(String s: members){
            sb.append(s);
            sb.append(",");
        }
        return sb.toString();

    }

    public ArrayList<String> members(){
        ArrayList<String> members = new ArrayList<>();
        if(this.isLeaf){
            members.add(this.label);
        }
        else{
            for(Component child: this.children){
                members.addAll(child.members());
            }
        }
        return members;
    }

    public void setRealTaxaInComponent(ArrayList<RealTaxon> realTaxa){
        this.realTaxaInComponent = realTaxa;
    }
}