package synthesis;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

// http://stackoverflow.com/questions/5685068/how-to-rename-a-node-in-jtree
// http://stackoverflow.com/questions/11554583/jtree-node-rename-preserve-user-object?rq=1

@SuppressWarnings("serial")
class SynthesisTreeModel extends DefaultTreeModel {

    public SynthesisTreeModel(DefaultMutableTreeNode node) {
        super(node);
    }
    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        SynthesisData nodeData = (SynthesisData) ((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
        nodeData.title = newValue.toString();
        super.valueForPathChanged(path, nodeData);
    }
}