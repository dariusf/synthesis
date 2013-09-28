package synthesis;

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

// http://stackoverflow.com/questions/4588109/drag-and-drop-nodes-in-jtree

@SuppressWarnings("serial")
class TreeTransferHandler extends TransferHandler {
    DataFlavor nodesFlavor;
    DataFlavor[] flavors = new DataFlavor[1];
    DefaultMutableTreeNode[] nodesToRemove;
    
    public TreeTransferHandler() {
        try {
             String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=\"" + javax.swing.tree.DefaultMutableTreeNode[].class.getName() + "\"";
             nodesFlavor = new DataFlavor(mimeType);
             flavors[0] = nodesFlavor;
         } catch (ClassNotFoundException e) {
             //System.out.println("ClassNotFound: " + e.getMessage());
        }
    }
    
    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        if(!support.isDrop() || !support.isDataFlavorSupported(nodesFlavor)) {
            return false;
        }
        support.setShowDropLocation(true);
        
        // Do not allow self-drop
        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        TreePath dropPath = dl.getPath();
        JTree tree = (JTree) support.getComponent();
        int dropRow = tree.getRowForPath(dropPath);
        int selectedRow = tree.getSelectionRows()[0];
        
        if(selectedRow == dropRow) {
            return false;
        }
        // Do not allow MOVE-action drops if a non-leaf node is  
        // selected unless all of its children are also selected.
        
        // EDIT: this is now allowed, and all children will be implicitly selected
        
//        int action = support.getDropAction();
//        if(action == MOVE) {
//            return haveCompleteNode(tree);
//        }
        
        // Do not allow a non-leaf node to be copied to a level  
        // which is less than its source level.
        
        // EDIT: modified. now only prevents drop into its descendents
        
        TreePath dest = dropPath;
        DefaultMutableTreeNode target = (DefaultMutableTreeNode) dest.getLastPathComponent();
        TreePath path = tree.getPathForRow(selectedRow);
        DefaultMutableTreeNode firstNode = (DefaultMutableTreeNode)path.getLastPathComponent();
        if(firstNode.getChildCount() > 0 && firstNode.isNodeDescendant(target)) {
            return false;
        }
        return true;
    }
//    private boolean haveCompleteNode(JTree tree) {
//        int[] selRows = tree.getSelectionRows();
//        TreePath path = tree.getPathForRow(selRows[0]);
//        DefaultMutableTreeNode first =  
//            (DefaultMutableTreeNode)path.getLastPathComponent();
//        int childCount = first.getChildCount();
//        // first has children and no children are selected.  
//        if(childCount > 0 && selRows.length == 1) {
//            return false;
//        }
//        // first may have children.  
//        for(int i = 1; i < selRows.length; i++) {
//            path = tree.getPathForRow(selRows[i]);
//            DefaultMutableTreeNode next =  
//                (DefaultMutableTreeNode)path.getLastPathComponent();
//            if(first.isNodeChild(next)) {
//                // Found a child of first.  
//                if(childCount > selRows.length-1) {
//                    // Not all children of first are selected.  
//                    return false;
//                }
//            }
//        }
//        return true;
//    }
   
    @Override
    protected Transferable createTransferable(JComponent c) {
        JTree tree = (JTree) c;
        TreePath path = tree.getSelectionPath();
        
        if (path == null) return null;

        // Disallow drag on root
        if (path.getLastPathComponent() == tree.getModel().getRoot()) {
            //System.out.println("ROOT cannot be dragged");
            return null;
        }
       
        // Make up a node array of copies for transfer and  
        // another for/of the nodes that will be removed in  
        // exportDone after a successful drop.
        ArrayList<DefaultMutableTreeNode> copies = new ArrayList<>();
        ArrayList<DefaultMutableTreeNode> toRemove = new ArrayList<>();
        
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
        DefaultMutableTreeNode copy = deep_copy(node);
        copies.add(copy);
        toRemove.add(node);
//            for(int i = 1; i < paths.length; i++) {
//                DefaultMutableTreeNode next = (DefaultMutableTreeNode)paths[i].getLastPathComponent();
//                // Do not allow higher level nodes to be added to list.  
//                if(next.getLevel() < node.getLevel()) {
//                    break;
//                } else if(next.getLevel() > node.getLevel()) {// child node  
//                    //copy.add(copy(next));
//                    // node already contains child  
//                } else {                                      // sibling  
//                    copies.add(deep_copy(next));
//                    toRemove.add(next);
//                }
//            }
        
        DefaultMutableTreeNode[] nodes = copies.toArray(new DefaultMutableTreeNode[copies.size()]);
        nodesToRemove = toRemove.toArray(new DefaultMutableTreeNode[toRemove.size()]);
        return new NodesTransferable(nodes);
    }
   
    // Defensive copy used in createTransferable.
    private DefaultMutableTreeNode deep_copy(TreeNode node) {
        //return new DefaultMutableTreeNode(node);
        return (DefaultMutableTreeNode) JsonReader.toJava(JsonWriter.toJson(node));
    }
   
    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        if(action == MOVE) {
            JTree tree = (JTree)source;
            SynthesisTreeModel model = (SynthesisTreeModel) tree.getModel();
            // Remove nodes saved in nodesToRemove in createTransferable.
            for(int i = 0; i < nodesToRemove.length; i++) {
                model.removeNodeFromParent(nodesToRemove[i]);
            }
            Synthesis.That.updateWordCount();
        }
    }
   
    @Override
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }
   
    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
        if(!canImport(support)) {
            return false;
        }
        // Extract transfer data.  
        DefaultMutableTreeNode[] nodes = null;
        try {
            Transferable t = support.getTransferable();
            nodes = (DefaultMutableTreeNode[]) t.getTransferData(nodesFlavor);
        } catch(UnsupportedFlavorException ufe) {
            //System.out.println("UnsupportedFlavor: " + ufe.getMessage());
        } catch(java.io.IOException ioe) {
            //System.out.println("I/O error: " + ioe.getMessage());
        }
        
        // Get drop location info
        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        int childIndex = dl.getChildIndex();
        TreePath dropPath = dl.getPath();
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dropPath.getLastPathComponent();
        JTree tree = (JTree) support.getComponent();
        SynthesisTreeModel model = (SynthesisTreeModel) tree.getModel();

        // Configure for drop mode
        int index = childIndex;  // DropMode.INSERT  
        if(childIndex == -1) {   // DropMode.ON
            index = parent.getChildCount();
        }
        
        // Add data to model
//        for(int i = 0; i < nodes.length; i++) {
//            model.insertNodeInto(nodes[i], parent, index++);
//        }
        model.insertNodeInto(nodes[0], parent, index);        
        Synthesis.That.selectNode(nodes[0]);
        return true;
    }
   
    @Override
    public String toString() {
        return getClass().getName();
    }
   
    public class NodesTransferable implements Transferable {
        DefaultMutableTreeNode[] nodes;
   
        public NodesTransferable(DefaultMutableTreeNode[] nodes) {
            this.nodes = nodes;
         }
   
        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if(!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return nodes;
        }
   
        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }
   
        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return nodesFlavor.equals(flavor);
        }
    }
}