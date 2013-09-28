package synthesis;

import java.util.ArrayList;
import javax.swing.tree.DefaultMutableTreeNode;

class SynthesisNode {//implements java.io.Serializable {
    SynthesisData data;
    ArrayList<SynthesisNode> children;
    String highlightedWords = null;

    public SynthesisNode(DefaultMutableTreeNode node) {
        data = (SynthesisData) node.getUserObject();
        children = new ArrayList<>();
        if (node.getChildCount() > 0) {
            for (int i=0; i<node.getChildCount(); i++) {
                SynthesisNode newone = new SynthesisNode((DefaultMutableTreeNode) node.getChildAt(i));
                children.add(newone);
            }
        }
    }
    public SynthesisNode() {
        data = new SynthesisData("Untitled", "", 0);
        children = new ArrayList<>();
    }    
}
