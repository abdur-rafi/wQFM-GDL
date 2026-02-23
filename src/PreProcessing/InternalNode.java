package src.PreProcessing;

import java.util.ArrayList;
import java.util.Collections;

import src.ScoreCalculator.NumSatSQ;
import src.Tree.Branch;

public class InternalNode {

    public Component[] childs;
    public Component parent;

    public int[] netTranser;

    public int count;
    public NumSatSQ scoreCalculator;

    public boolean[] realTaxaPresent;

    public int[] childrenIndicesInComponent;
    public int parentIndexInComponent;



    public InternalNode(Component[] childs, Component parent){
        this.childs = childs;
        this.parent = parent;

        this.count = 1;
        this.scoreCalculator = null;

        this.realTaxaPresent = null;

        this.childrenIndicesInComponent = new int[childs.length];

        if(childs.length > 2){
            System.out.println("polytomy");
        }
        for(int i = 0; i < childs.length; ++i){
            this.childrenIndicesInComponent[i] = childs[i].addInternalNode(this, i);
        }

        this.parentIndexInComponent = parent.addInternalNode(this, 2);

        // this.netTranser = new int[childCompsCommon.length];

    }

    public Branch[] getBranchesOfChilds(){
        Branch[] branches = new Branch[this.childs.length];
        for(int i = 0; i < this.childs.length; ++i){
            branches[i] = this.childs[i].dataList.get(this.childrenIndicesInComponent[i]).branch;
        }
        return branches;
    }

    public Branch getBranchOfParent(){
        return this.parent.dataList.get(this.parentIndexInComponent).branch;
    }

    public void increaseCount(){
        this.count++;
    }

    public void setRealTaxaPresent(boolean[] realTaxaInChilds){
        this.realTaxaPresent = realTaxaInChilds;
    }
    
    
    public void batchTransfer(){
        // for(int i = 0; i < this.partitionNodes.length; ++i){
        //     if(this.netTranser[i] != 0){
        //         this.scoreCalculator.batchTransferRealTaxon(i, this.netTranser[i]);
        //     }
        //     netTranser[i] = 0;
        // }
        
    }


    @Override
    public String toString(){
        return convertToString(childs, parent);
    }


    public void cumulateTransfer(int index, int currPartition){
        // negative if transfering from 1 to 0
        // positive if transfering from 0 to 1
        netTranser[index] += (currPartition == 0 ? 1 : -1);

    }

    public static String convertToString(Component[] childs, Component parent){
        ArrayList<String> componentStrings = new ArrayList<>();
        for(Component p : childs){
            componentStrings.add(p.toString());
        }
        componentStrings.add(parent.toString());
        Collections.sort(componentStrings);
        return String.join("|", componentStrings);
    }
}
