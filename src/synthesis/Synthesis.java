package synthesis;
//
//import java.awt.BorderLayout;
//import java.awt.EventQueue;
//
//import javax.swing.JFrame;
//import javax.swing.JPanel;
//import javax.swing.border.EmptyBorder;

//public class Synthesis extends JFrame {
//
//	private JPanel contentPane;
//
//	/**
//	 * Launch the application.
//	 */
//	public static void main(String[] args) {
//		EventQueue.invokeLater(new Runnable() {
//			public void run() {
//				try {
//					Synthesis frame = new Synthesis();
//					frame.setVisible(true);
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		});
//	}

	/**
	 * Create the frame.
	 */
//	public Synthesis() {
//      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		setBounds(100, 100, 450, 300);
//		contentPane = new JPanel();
//		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
//		contentPane.setLayout(new BorderLayout(0, 0));
//		setContentPane(contentPane);
//	}

import com.cedarsoftware.util.io.JsonReader;
import com.cedarsoftware.util.io.JsonWriter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.*;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.filechooser.*;
import javax.swing.text.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.undo.UndoManager;
import org.tautua.markdownpapers.Markdown;
import org.tautua.markdownpapers.parser.*;

@SuppressWarnings("serial")
public class Synthesis extends javax.swing.JFrame {

	public static Font HIGHLIGHTED_WORDS_DIALOG_FONT = new Font("Consolas", Font.PLAIN, 12);
	public static Font SYNTHESIS_FONT = new Font("Calibri (Body)", Font.PLAIN, 14);

	public static String HIGHLIGHT_PRELUDE_STRING = "// Words (actually, regexes) to be coloured go in here.\n// Put each #colour on its own line, followed by the words to be highlighted with that colour, each also on its own line.\n// Aliases for all web colours are available.\n// <- line comment\n\n";
	
	public static boolean DEBUG_MODE = true;
	
    static Synthesis That;
    JFileChooser FC = null;
    JFileChooser ExportFC = null;
    DefaultMutableTreeNode Root = null;
//    MarkdownRenderer MarkdownR = null;
    TextColourizer Colours = null;
    ColourInformation ColInfo = null;
    String ColInfoString = "";
    boolean renderColours = true;
    Timer T;
    boolean update = true;
    String fileName = "";
    
    UndoManager undoManager = new UndoManager();
    boolean rememberUndoStates = true;
    
    public Synthesis() {
        initComponents();
        FC = new JFileChooser();
        FC.removeChoosableFileFilter(FC.getAcceptAllFileFilter());
        FC.addChoosableFileFilter(new FileNameExtensionFilter("Synthesis files (*.syn)", "syn"));
        FC.addChoosableFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));
        FC.addChoosableFileFilter(FC.getAcceptAllFileFilter());

        ExportFC = new JFileChooser();
        ExportFC.removeChoosableFileFilter(ExportFC.getAcceptAllFileFilter());
        ExportFC.addChoosableFileFilter(new FileNameExtensionFilter("HTML files (*.html)", "html"));
        ExportFC.addChoosableFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));
    }            
    
    private void createChildren(DefaultMutableTreeNode parent, SynthesisNode node) {
        for (SynthesisNode child : node.children) {
            DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(child.data);
            parent.add(newNode);
            createChildren(newNode, child);
        }
    }
    
    private void growTreeFromSynthesisNode(SynthesisNode s) {
        try {
            Root = new DefaultMutableTreeNode(s.data);
            SynthesisTreeModel model = (SynthesisTreeModel) Tree.getModel();
            model.setRoot(Root);
            createChildren(Root, s);
            Tree.expandPath(new TreePath(Root.getPath()));
        } catch (NullPointerException e) {
            JOptionPane.showMessageDialog(this, "Invalid or corrupt Synthesis file! Please open another.", "Invalid or corrupt Synthesis file", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void formWindowOpened(java.awt.event.WindowEvent evt) {                                  
        // Tree initialization
        Tree.setModel(new SynthesisTreeModel((DefaultMutableTreeNode) Tree.getModel().getRoot()));
        Tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        Tree.setShowsRootHandles(true);
        Tree.setEditable(true);
        Tree.setInvokesStopCellEditing(true);
        Tree.setDragEnabled(true);
        Tree.setDropMode(DropMode.ON_OR_INSERT);

        // Disable tree CCP
        ActionMap actionMap = Tree.getActionMap();
        actionMap.remove("cut");
        actionMap.getParent().remove("cut");
        actionMap.remove("copy");
        actionMap.getParent().remove("copy");
        actionMap.remove("paste");
        actionMap.getParent().remove("paste");
        
        // Tree events
        Tree.setTransferHandler(new TreeTransferHandler());
        Tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) Tree.getLastSelectedPathComponent();
                if (node != null) {
                    SynthesisData obj = (SynthesisData) node.getUserObject();
                    Textarea.setText(obj.text.trim());
                    undoManager.discardAllEdits();
                    updateUndoState();
                    updateRedoState();
                    javax.swing.SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() {
                            ((JScrollPane) Textarea.getParent().getParent()).getVerticalScrollBar().setValue(0);
                            Textarea.setCaretPosition(0);
                        }
                    });
                }
            }
        });

        // Node rename event
        ((SynthesisTreeModel) Tree.getModel()).addTreeModelListener(new TreeModelListener() {
            @Override
            public void treeNodesChanged(TreeModelEvent e) {
//                DefaultMutableTreeNode node = (DefaultMutableTreeNode) (e.getTreePath().getLastPathComponent());

                /*
                 * If the event lists children, then the changed node is the child of the node we have already
                 * gotten.  Otherwise, the changed node and the specified node are the same.
                 */
//                try {
//                    int index = e.getChildIndices()[0];
//                    node = (DefaultMutableTreeNode) (node.getChildAt(index));
//                } catch (NullPointerException exc) {}

//                System.out.println("The user has finished editing the node.");
                Textarea.requestFocusInWindow();
            }
            @Override
            public void treeNodesInserted(TreeModelEvent e) {
            }
            @Override
            public void treeNodesRemoved(TreeModelEvent e) {
            }
            @Override
            public void treeStructureChanged(TreeModelEvent e) {
            }
        });
        
        // Textarea initialization
//        Textarea.setDragEnabled(true);
        Textarea.setEditorKit(new MyStyledEditorKit());
        Textarea.setDocument(new NoTabDocument());
        Textarea.setMargin(new Insets(5,5,5,5));
        Textarea.setFont(SYNTHESIS_FONT);
//        MarkdownR = new MarkdownRenderer(Textarea);
        Colours = new TextColourizer(Textarea);
        ColInfo = new ColourInformation("");
        
        // Textarea events
        Textarea.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                // No longer fires because of intercepted tabs in NoTabDocument
            }
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_TAB){
                    Tree.requestFocusInWindow();
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
            }
        });
        Textarea.getDocument().addUndoableEditListener(new UndoableEditListener() {
                @Override public void undoableEditHappened(UndoableEditEvent e) {
                    if (rememberUndoStates) {
                        undoManager.addEdit(e.getEdit());
                        updateUndoState();
                        updateRedoState();
                    }
                }
        });
        Textarea.getDocument().addDocumentListener(new DocumentListener() {
            public void saveText() {
                ((SynthesisData) ((DefaultMutableTreeNode) Tree.getLastSelectedPathComponent()).getUserObject()).text = Textarea.getText();
                unsavedChanges = true;
                update = true;
                // need new thread if i want to render the markdown here
                
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                	@Override
                	public void run() {
                		//MarkdownR.render();
            		}
                });
            }
            @Override
            public void insertUpdate(DocumentEvent e) {
                saveText();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                saveText();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                // fires on attribute change
            }
        });

        Timer T = new Timer(500, new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (update) {
                    update = false;

                    updateWordCount();
                    rememberUndoStates = false;
//                    MarkdownR.render();
                    if (renderColours) {
                        Colours.render();
                    } else {
                    	Colours.normalize();
                    }
                    rememberUndoStates = true;
                }
            }
        });
        T.setInitialDelay(0);
        T.start();
        
        initialize();
    }                                 

    /**
     * Reads the contents of the file at the specified path, then grows the tree
     * from the resulting object.
     * @param path
     */
    private void read(String path) {
        SynthesisNode s;        
        try {
            FileReader f = new FileReader(path);
            BufferedReader b = new BufferedReader(f);
            String json = "";
            while (true) {
                String line = b.readLine();
                if (line == null) {
                    break;
                } else {
                    json += line;
                }
            }
            b.close();
            f.close();
            s = (SynthesisNode) JsonReader.toJava(json);
//            FileInputStream fileIn = new FileInputStream(path);
//            ObjectInputStream in = new ObjectInputStream(fileIn);
//            s = (SynthesisNode) in.readObject();
//            in.close();
//            fileIn.close();
            
        } catch(IOException e) {
            e.printStackTrace();
            return;
        }// catch(ClassNotFoundException e) {
//            System.out.println("SynthesisNode not found");
//            e.printStackTrace();
//            return;
//        }

        // update file name
        String[] temp = path.split("\\\\");
        fileName = temp[temp.length-1];

        growTreeFromSynthesisNode(s);
        if (s.highlightedWords != null) {
        	// for legacy synthesis files with no support for highlighting
            ColInfoString = s.highlightedWords.trim();
        } else {
            ColInfoString = "";
        }
        ColInfo = new ColourInformation(ColInfoString);
        selectNode(Root);
    }

    /**
     * Writes the contents of the tree to a file in path
     * @param path
     */
//    http://www.tutorialspoint.com/java/java_serialization.htm
    private void write(String path) {
        SynthesisNode s = new SynthesisNode(Root);
        s.highlightedWords = ColInfoString;
        try {
            String json = JsonWriter.objectToJson(s);
            String extension = "." + ((FileNameExtensionFilter) FC.getFileFilter()).getExtensions()[0];
            if(!path.toLowerCase().endsWith(extension)) {
                path += extension;
            }
            FileWriter f = new FileWriter(path);
            f.write(json);
            f.close();
//            FileOutputStream fileOut = new FileOutputStream(path);
//            ObjectOutputStream out = new ObjectOutputStream(fileOut);
//            out.writeObject((SynthesisData) Root.getUserObject());
//            out.close();
//            fileOut.close();
            
            // update filename
            
            String[] temp = path.split("\\\\");
            fileName = temp[temp.length-1];
        } catch(IOException i) {
            i.printStackTrace();
        }
    }
    
    public String getRoot() {
        URL u = getClass().getProtectionDomain().getCodeSource().getLocation();
        File f;
        try {
            f = new File(u.toURI());
            return f.getParent();
        } catch (URISyntaxException ex) {
            Logger.getLogger(Synthesis.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }
    
    /**
     * Shows the open file dialog, reads the file and returns true if one was
     * selected, returns false otherwise.

     * @return
     */
    private boolean open() {
        FC.setCurrentDirectory(new File(getRoot()));
        FC.setSelectedFile(new File(""));
        int returnVal = FC.showOpenDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = FC.getSelectedFile();
            if (file.exists()) {
                currentSavedFile = file;
                read(currentSavedFile.getAbsolutePath());
                savedOnceAlready = true;
                return true;
            } else {
                JOptionPane.showMessageDialog(Synthesis.this, "Invaild file specified. Check the file path and try again.", "Invalid file specified!", JOptionPane.ERROR_MESSAGE);
                return open();
            }
        } else {
            return false;
        }
    }

    /**
     * Shows the save file dialog, writes the file and returns true if a file
     * was selected, returns false otherwise.
     * @return
     */
    private boolean save() {
        FC.setCurrentDirectory(new File(getRoot()));
        FC.setSelectedFile(new File(""));
        int returnVal = FC.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            currentSavedFile = FC.getSelectedFile();
            write(currentSavedFile.getPath());
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Shows the confirmation dialog and returns the result
     * @return
     */
    private static int confirm() {
        return JOptionPane.showConfirmDialog((Component) null, "This document has unsaved changes! Do you want to keep them?", "Unsaved changes!", JOptionPane.YES_NO_CANCEL_OPTION);
    }

    File currentSavedFile = null;
    boolean savedOnceAlready = false;
    boolean unsavedChanges = false;
    int newNodeSuffix = 0;
    
    /**
     * grows the tree from a new object and resets the state of the program
     */
    private void initialize() {
        currentSavedFile = null;
        savedOnceAlready = false;
        unsavedChanges = false;
        newNodeSuffix = 0;
    
        growTreeFromSynthesisNode(new SynthesisNode());
        Tree.setSelectionPath(new TreePath(Root.getPath()));
    }
    
    private void OpenActionPerformed(java.awt.event.ActionEvent evt) {                                     
        if (unsavedChanges) {
            int result = confirm();
            
            if (result == JOptionPane.YES_OPTION) {
                if (save()) {
                    open();
                }
            } else if (result == JOptionPane.NO_OPTION) {
                open();
            }
        } else {
            open();
        }
    }                                    
    
    private void SaveActionPerformed(java.awt.event.ActionEvent evt) {                                     
        if (!savedOnceAlready) {
            if (save()) {
                savedOnceAlready = true;
                unsavedChanges = false;
                updateWordCount();
            }
        } else {
            write(currentSavedFile.getPath());
            unsavedChanges = false;
            updateWordCount();
        }
    }                                    

    private void SaveAsActionPerformed(java.awt.event.ActionEvent evt) {                                       
        save();
        savedOnceAlready = true;
        unsavedChanges = false;
        updateWordCount();
    }                                      
        
    private void ExitActionPerformed(java.awt.event.ActionEvent evt) {                                       
    	formWindowClosing(null);
	}                                      

    private void NewActionPerformed(java.awt.event.ActionEvent evt) {                                    
        if (unsavedChanges) {
            int result = confirm();
            if (result == JOptionPane.YES_OPTION) {
                if (save()) {
                    initialize();
                }
            } else if (result == JOptionPane.NO_OPTION) {
                initialize();
            }
        } else {
            initialize();
        }
    }                                   
    
//<editor-fold defaultstate="collapsed" desc="editSelectedNode()">
    private void editSelectedNode() {
        Tree.startEditingAtPath(Tree.getSelectionPath());
    }
//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="selectNode()">
    void selectNode(DefaultMutableTreeNode n) {
       TreePath path = new TreePath(n.getPath());
       Tree.expandPath(path); 
       Tree.setSelectionPath(path);
    }
//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="getSelectedNode() - returns the node or the root if nothing is selected">
    private DefaultMutableTreeNode getSelectedNode() {
        DefaultMutableTreeNode parentNode;
        TreePath parentPath = Tree.getSelectionPath();
        if (parentPath == null) { //There is no selection. Default to the root node.
            parentNode = (DefaultMutableTreeNode) Tree.getModel().getRoot();
        } else {
            parentNode = (DefaultMutableTreeNode) (parentPath.getLastPathComponent());
        }
        return parentNode;
    }
//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="getParentOfSelectedNode() - returns the node or the root if nothing is selected">
    private DefaultMutableTreeNode getParentOfSelectedNode() {
        DefaultMutableTreeNode parentNode;
        TreePath parentPath = Tree.getSelectionPath().getParentPath();
        if (parentPath == null) { //There is no selection. Default to the root node.
            parentNode = (DefaultMutableTreeNode) Tree.getModel().getRoot();
        } else {
            parentNode = (DefaultMutableTreeNode) (parentPath.getLastPathComponent());
        }
        return parentNode;
    }
//</editor-fold>
//<editor-fold defaultstate="collapsed" desc="deleteNode()">
    private void deleteNode(DefaultMutableTreeNode n) {
        ((DefaultTreeModel) Tree.getModel()).removeNodeFromParent(n);
    }
//</editor-fold>

    // http://stackoverflow.com/questions/14184995/swing-triggering-tree-cell-edit-event
    private void RenameNodeActionPerformed(java.awt.event.ActionEvent evt) {                                           
        editSelectedNode();
        unsavedChanges = true;
    }                                          

    
    private void DeleteNodeActionPerformed(java.awt.event.ActionEvent evt) {                                           
        DefaultMutableTreeNode node = getSelectedNode();
        if (node == Root) {
            return;
        }

        DefaultMutableTreeNode parent = getParentOfSelectedNode();

        int result;
        if (node.getChildCount() > 0) {
            result = JOptionPane.showConfirmDialog((Component) null, "This node has children! Are you sure you wish to delete it?", "Node has children!", JOptionPane.YES_NO_OPTION);
        } else {
            result = JOptionPane.showConfirmDialog((Component) null, "Are you sure you wish to delete this node?", "Node deletion confirmation", JOptionPane.YES_NO_OPTION);
        }
        
        if (result == JOptionPane.YES_OPTION) {
            if (parent.getChildCount() <= 1) {
                selectNode(parent);
            } else {
                if (node.getNextSibling() == null) {
                    selectNode(node.getPreviousSibling());
                } else {
                    selectNode(node.getNextSibling());
                }
            }
            deleteNode(node);
            updateWordCount();
            unsavedChanges = true;
        }
    }                                          
    
    private DefaultMutableTreeNode newChild(DefaultMutableTreeNode parentNode) {
        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(new SynthesisData("New Node " + ++newNodeSuffix, "", 0));
        ((SynthesisTreeModel) Tree.getModel()).insertNodeInto(childNode, parentNode, parentNode.getChildCount());
        Tree.scrollPathToVisible(new TreePath(childNode.getPath()));
        return childNode;
    }
    
    private void NewChildNodeActionPerformed(java.awt.event.ActionEvent evt) {                                             
        DefaultMutableTreeNode child = newChild(getSelectedNode());
        selectNode(child);
        editSelectedNode();
        unsavedChanges = true;
    }                                            

    private void NewSiblingNodeActionPerformed(java.awt.event.ActionEvent evt) {                                               
        DefaultMutableTreeNode sibling = newChild(getParentOfSelectedNode());
        selectNode(sibling);
        editSelectedNode();
        unsavedChanges = true;
    }                                              

    private void ExpandAllActionPerformed(java.awt.event.ActionEvent evt) {                                          
        for (int row = 0; row < Tree.getRowCount(); row++) {
            Tree.expandRow(row);
        }
    }                                         
    
    private void collapseAll(DefaultMutableTreeNode node) {
        if (node.getChildCount() > 0) {
            for (int i=0; i<node.getChildCount(); i++) {
                collapseAll((DefaultMutableTreeNode) node.getChildAt(i));
            }
            Tree.collapsePath(new TreePath(node.getPath()));
        }
    }
    
    private void CollapseAllActionPerformed(java.awt.event.ActionEvent evt) {                                            
        collapseAll(Root);
    }                                           

    private void OverviewActionPerformed(java.awt.event.ActionEvent evt) {
        JTextArea textArea = new JTextArea();
        textArea.setText("Synthesis is a lightweight, minimalistic text editor that expresses the idea of the hierarchy. It was made for the organization and planning of novels but can repurposed for anything else which may benefit from a hierarchical outlook.\n\nAll available operations are accessible from the various menus. To get started, select a node in the tree on the left, and you will be able to edit its associated text in the text field to the right. Any such changes will be kept in memory, so you can navigate freely between nodes as you edit your document. Don't forget to save your work after - the contents and structure of the tree will be maintained in a single file.\n\nKeyboard navigation is fully supported. All menu operations have hotkeys, and you can use Shift-Tab or Tab to switch focus between the tree and the text field. All navigation keys (arrows, Home, End, Pg Up, Pg Down) will work in both components. Drag-and-drop is also supported within the tree itself.\n\nSynthesis was built in Java and so is perfectly cross-platform. It saves in the highly-portable and extensively-supported JSON format (www.json.org). It is open-source and free: everything is released under the Creative Commons and source code is freely available.\n\nIf you have any questions or concerns, you can find my email address in Credits. Have fun using this program!");
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setMargin(new Insets(5,5,5,5));
        textArea.setEditable(false);
        final JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {scrollPane.getVerticalScrollBar().setValue(0);}
        });
        JOptionPane.showMessageDialog(this, scrollPane, "Synthesis 2 - an overview", JOptionPane.INFORMATION_MESSAGE);
    }

    private void CreditsActionPerformed(java.awt.event.ActionEvent evt) {                                        
        JTextArea textArea = new JTextArea();
        textArea.setText("Synthesis 2.0.0\n\nMade in Eclipse Juna with Swing\n\nThanks to:\n- Mark James for all icons (http://www.famfamfam.com/lab/icons/silk/)\n- John DeRegnaucourt for json-io (http://code.google.com/p/json-io/)\n-John Gruber for Markdown Papers (markdown.tautua.org)\n\nCreated by Darius Foo, (C) 2012. Feel free to email me at dead_or_alivex@hotmail.com if you have bug reports or concerns.\n\nThis work is licensed under a Creative Commons Attribution-NonCommercial-NoDerivs 3.0 Unported License.\nhttp://creativecommons.org/licenses/by-nc-nd/3.0/");
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setMargin(new Insets(5,5,5,5));
        textArea.setEditable(false);
        final JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {scrollPane.getVerticalScrollBar().setValue(0);}
        });
        JOptionPane.showMessageDialog(this, scrollPane, "Credits", JOptionPane.INFORMATION_MESSAGE);
    }                                       

    private void formWindowClosing(java.awt.event.WindowEvent evt) {                                   
        if (unsavedChanges) {
            int result = confirm();
            
            if (result == JOptionPane.YES_OPTION) {
                if (save()) {
                    System.exit(0);
                }
            } else if (result == JOptionPane.NO_OPTION) {
                System.exit(0);
            }
        } else {
            System.exit(0);
        }
    }
    
    @SuppressWarnings("unused")
	private void alert(Object o) {
        JOptionPane.showMessageDialog(Synthesis.this, o);
    }
    
    private void OutputActionPerformed(java.awt.event.ActionEvent evt) {
    	renderColours = !renderColours;
    	alert("Colour rendering turned " + (renderColours ? "on" : "off"));
    	update = true;
//    	if (ColInfo.toString().equals("")) {
//    		alert("empty");
//    	} else {
//        	alert(ColInfo.toString());
//    	}
//        FileWriter f;
//        try {
//            f = new FileWriter("debug.txt");
//            say("Debug - internal structure:\r\n");
//            output(Root, 0);
//            f.write(a);
//            a = "";
//            f.close();
//            ProcessBuilder pb = new ProcessBuilder("Notepad.exe", "debug.txt");
//            pb.start();
//        } catch (IOException ex) {
//            Logger.getLogger(Synthesis.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }                                      

    protected void updateUndoState() {
        Undo.setEnabled(undoManager.canUndo());
    }

    protected void updateRedoState() {
        Redo.setEnabled(undoManager.canRedo());
    }
    
    private void UndoActionPerformed(java.awt.event.ActionEvent evt) {                                     
        if (undoManager.canUndo()) {
            undoManager.undo();
        }
        updateUndoState();
        updateRedoState();
    }                                    

    private void RedoActionPerformed(java.awt.event.ActionEvent evt) {                                     
        if (undoManager.canRedo()) {
            undoManager.redo();
        }
        updateRedoState();
        updateUndoState();
    }                                    

    private void BoldActionPerformed(java.awt.event.ActionEvent evt) {
    	Textarea.requestFocusInWindow();
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
        	@Override
        	public void run() {
                if (Textarea.getSelectedText() == null) {
                    try {
                        Textarea.getDocument().insertString(Textarea.getCaretPosition(), "****", null);
                        Textarea.setCaretPosition(Textarea.getCaretPosition()-2);
                    } catch (BadLocationException ex) {
                        Logger.getLogger(Synthesis.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    String selection = Textarea.getSelectedText();
                    int length = selection.length();
                    if (selection.startsWith("**") && selection.endsWith("**")) {
                        Textarea.replaceSelection(selection.substring(2, length-2));
                    } else if (selection.startsWith("*") && selection.endsWith("*")) {
                        Textarea.replaceSelection("*" + selection + "*");
                    } else {
                        Textarea.replaceSelection("**" + selection + "**");
                    }
                }
    		}
        });
    }                                    

    private void ItalicizeActionPerformed(java.awt.event.ActionEvent evt) {
    	Textarea.requestFocusInWindow();
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
        	@Override
        	public void run() {
        		if (Textarea.getSelectedText() == null) {
                    try {
                        Textarea.getDocument().insertString(Textarea.getCaretPosition(), "**", null);
                        Textarea.setCaretPosition(Textarea.getCaretPosition()-1);
                    } catch (BadLocationException ex) {
                        Logger.getLogger(Synthesis.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    String selection = Textarea.getSelectedText();
                    int length = selection.length();
                    if (selection.startsWith("*") && selection.endsWith("*")) {
                        Textarea.replaceSelection(selection.substring(1, length-1));
                    } else {
                        Textarea.replaceSelection("*" + selection + "*");
                    }
                }
    		}
        });
    }

    private void UnderlineActionPerformed(java.awt.event.ActionEvent evt) {
    	Textarea.requestFocusInWindow();
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
        	@Override
        	public void run() {
                if (Textarea.getSelectedText() == null) {
                    try {
                        Textarea.getDocument().insertString(Textarea.getCaretPosition(), "__", null);
                        Textarea.setCaretPosition(Textarea.getCaretPosition()-1);
                    } catch (BadLocationException ex) {
                        Logger.getLogger(Synthesis.class.getName()).log(Level.SEVERE, null, ex);
                    }
                } else {
                    String selection = Textarea.getSelectedText();
                    int length = selection.length();
                    if (selection.startsWith("_") && selection.endsWith("_")) {
                        Textarea.replaceSelection(selection.substring(1, length-1));
                    } else {
                        Textarea.replaceSelection("_" + selection + "_");
                    }
                }
    		}
        });
    }
       
    private void HighlightedWordsActionPerformed(java.awt.event.ActionEvent evt) {                                          
        JTextArea textArea = new JTextArea();
        
        if (ColInfoString.equals("")) {
            textArea.setText(HIGHLIGHT_PRELUDE_STRING + ColInfoString);
        } else {
        	textArea.setText(ColInfoString);
        }
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setMargin(new Insets(5,5,5,5));
    	textArea.setFont(HIGHLIGHTED_WORDS_DIALOG_FONT);
        final JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(400, 200));
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
            	scrollPane.getVerticalScrollBar().setValue(0);
        	}
        });
        JOptionPane.showMessageDialog(this, scrollPane, "Highlighted Words", JOptionPane.INFORMATION_MESSAGE);
        
        // dialogs are blocking, so the input to the textarea can just be collected here
        ColInfoString = textArea.getText();
        ColInfo = new ColourInformation(ColInfoString);
        update = true;
        
        // consider making it a textpane so i can color its text?
    }
    
    private void saveToMarkdown(FileWriter f, DefaultMutableTreeNode node, int level) throws IOException {
        SynthesisData nodeData = (SynthesisData) node.getUserObject();
        String hashes="";
        for (int i=0; i < level && i < 6; i++) {
            hashes += "#";
        }
        if (level != 0) {
            hashes += " ";
        }

        String text = nodeData.text.replaceAll("\n", "\r\n").replaceAll("\\&", "&amp;").replaceAll("\\<", "&lt;").replaceAll("\\>", "&gt;").trim();
        f.write(hashes + node.toString() + "\r\n\r\n" + text + "\r\n\r\n");
        if (node.getChildCount() > 0) {
            for (int i=0; i<node.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                saveToMarkdown(f, child, level+1);
            }
        }
        if (level == 2) {
            f.write("* * *\r\n\r\n");
        }
    }
    
    private void saveToText(FileWriter f, DefaultMutableTreeNode node, int level, int count, String current) throws IOException {
        SynthesisData nodeData = (SynthesisData) node.getUserObject();
        if (level != 0) {
            if (current.equals("")) {
                current = count + "";
            } else {
                current += "." + count;
            }
        }
        f.write(current + (current.equals("") ? "" : ". ") + node.toString() + "\r\n\r\n" + nodeData.text.replaceAll("\n", "\r\n").trim() + "\r\n\r\n");
        if (node.getChildCount() > 0) {
            for (int i=0; i<node.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                saveToText(f, child, level+1, i+1, current);
            }
        }
    }
    
    private void ExportActionPerformed(java.awt.event.ActionEvent evt) {                                       
        ExportFC.setSelectedFile(new File(""));
        File current;
        int returnVal = ExportFC.showSaveDialog(this);
        FileWriter f;
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            current = ExportFC.getSelectedFile();
            String ext = ((FileNameExtensionFilter) ExportFC.getFileFilter()).getExtensions()[0];
            String path = current.getPath();
            if(!path.toLowerCase().endsWith("." + ext)) {
                path += "." + ext;
            }
            try {
                if (ext.equals("html")) {
                    String mdpath = path.substring(0, path.length()-5) + ".markdown";
                    f = new FileWriter(mdpath);
                    saveToMarkdown(f, Root, 1); //change
                    f.close();
                    
                    FileReader in = new FileReader(mdpath);
                    FileWriter out = new FileWriter(path);                    
                    Markdown md = new Markdown();
                    
                    try {
                        md.transform(in, out);
                    } catch (ParseException ex) {
                        Logger.getLogger(Synthesis.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    in.close();
                    out.close();
                    
                } else if (ext.equals("txt")) {
                    f = new FileWriter(path);
                    saveToText(f, Root, 0, 0+1, "");
                    f.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(Synthesis.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }                                      

//    private void headerInfo() {
//        JTextArea textArea = new JTextArea();
//        textArea.setText("Header syntax in Synthesis in exactly the same as that of Markdown.\n\nThis is a first-level header.\n=========================\n\nIt's equivalent to <h1> in HTML - if you're unfamiliar with HTML, think of it simply as a header of maximum font size.\n\nHeaders of smaller font sizes would naturally follow:\n\nThis is a second-level header.\n--------------------------------------------\n\nOr, in HTML, equivalent to the <h2> tag.\n\n### Headers <h3> through ###\n#### to <h6> can be #\n##### represented in any##\n###### of these ways ###########\n\nBasically, only the placement of the initial #s matter; the rest is just to aesthetically pad your document.");
//        textArea.setLineWrap(true);  
//        textArea.setWrapStyleWord(true);
//        textArea.setMargin(new Insets(5,5,5,5));
//        textArea.setEditable(false);
//        final JScrollPane scrollPane = new JScrollPane(textArea);
//        scrollPane.setPreferredSize(new Dimension(400, 200));
//        javax.swing.SwingUtilities.invokeLater(new Runnable() {
//            @Override public void run() {scrollPane.getVerticalScrollBar().setValue(0);}
//        });
//        JOptionPane.showMessageDialog(this, scrollPane, "Headers in Synthesis", JOptionPane.INFORMATION_MESSAGE);
//    }

    String a = "";
    private void say(String stuff) {
        a += stuff + "\r\n";
    }
    
    private void output(DefaultMutableTreeNode node, int level) {
        SynthesisData nodeData = (SynthesisData) node.getUserObject();
        String spaces="";
        for (int i=0; i<level; i++) {
            spaces+="    ";
        }
        say(spaces + "Node: " + node.toString() + " (" + (nodeData.text.length() <= 10 ? (nodeData.text + ")") : (nodeData.text.substring(0,9)) + "...)"));
        if (node.getChildCount() > 0) {
            for (int i=0; i<node.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                say(spaces + "- " + child.toString());
                output(child, level+1);
            }
        } else {
            say(spaces + "- No children");
        }
    }
    
    /**
     * word count
     * @param s the string to count words in
     * @return
     */
    private static int countWords(String s){
        int counter = 0;

        boolean word = false;
        int endOfLine = s.length() - 1;

        for (int i = 0; i < s.length(); i++) {
            // if the char is letter, word = true.
            if (Character.isLetter(s.charAt(i)) == true && i != endOfLine) {
                word = true;
                // if char isnt letter and there have been letters before (word
                // == true), counter goes up.
            } else if (Character.isLetter(s.charAt(i)) == false && word == true) {
                counter++;
                word = false;
                // last word of String, if it doesnt end with nonLetter it
                // wouldnt count without this.
            } else if (Character.isLetter(s.charAt(i)) && i == endOfLine) {
                counter++;
            }
        }
        return counter;
    }
    void updateWordCount() {
        this.setTitle("Synthesis 2" + (fileName.equals("") ? "" : " - " + fileName) + (unsavedChanges ? "*" : "") + " - (" + countWords(Textarea.getText()) + "/" + deepCount(Root) + " words)");
    }
    private int deepCount(DefaultMutableTreeNode node) {
        SynthesisData nodeData = (SynthesisData) node.getUserObject();
        int count = countWords(nodeData.text);
        if (node.getChildCount() > 0) {
            for (int i=0; i<node.getChildCount(); i++) {
                count += deepCount((DefaultMutableTreeNode) node.getChildAt(i));
            }
        }
        return count;
    }
    
    public static void main(String args[]) {
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Synthesis.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                Synthesis synthesis = new Synthesis();
                synthesis.setVisible(true);
                synthesis.setLocationRelativeTo(null);
                
                // icon
//                java.net.URL url = ClassLoader.getSystemResource("synthesis/images/application_side_tree.png");
//                synthesis.setIconImage(Toolkit.getDefaultToolkit().createImage(url));
                String[] images = new String[] {
                	"synthesis/images/16new.png",
                	"synthesis/images/32new.png",
                	"synthesis/images/48new.png",
                	"synthesis/images/64new.png",
                	"synthesis/images/256new.png"
                };
                ArrayList<Image> icons = new ArrayList<>();
                for (int i=0; i<images.length; i++) {
                    icons.add(Toolkit.getDefaultToolkit().createImage(ClassLoader.getSystemResource(images[i])));
                }
                synthesis.setIconImages(icons);
                
                Synthesis.That = synthesis;
            }
        });
    }
    
    /**
     * GUI stuff
     */

    private javax.swing.JMenuItem Bold;
    private javax.swing.JMenuItem CollapseAll;
    private javax.swing.JMenuItem Credits;
    private javax.swing.JMenu Debug;
    private javax.swing.JMenuItem DeleteNode;
    private javax.swing.JMenu Edit;
    private javax.swing.JMenuItem ExpandAll;
    private javax.swing.JMenuItem Export;
    private javax.swing.JMenu File;
    private javax.swing.JMenu Help;
    private javax.swing.JMenuItem Italicize;
    private javax.swing.JMenuBar Menu;
    private javax.swing.JMenuItem New;
    private javax.swing.JMenuItem NewChildNode;
    private javax.swing.JMenuItem NewSiblingNode;
    private javax.swing.JMenu Node;
    private javax.swing.JMenuItem Open;
    private javax.swing.JMenuItem Output;
    private javax.swing.JMenuItem Overview;
    private javax.swing.JMenuItem Redo;
    private javax.swing.JMenuItem RenameNode;
    private javax.swing.JMenuItem Save;
    private javax.swing.JMenuItem SaveAs;
    private javax.swing.JMenuItem Exit;
    private javax.swing.JTextPane Textarea;
    private javax.swing.JTree Tree;
    private javax.swing.JMenuItem Underline;
    private javax.swing.JMenuItem Undo;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JMenuItem HighlightedWords;
    
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        Tree = new javax.swing.JTree();
        Tree.setModel(new DefaultTreeModel(
        	new DefaultMutableTreeNode("Untitled") {{}}
        ));
        jScrollPane3 = new javax.swing.JScrollPane();
        Textarea = new javax.swing.JTextPane();
        Menu = new javax.swing.JMenuBar();
        File = new javax.swing.JMenu();
        Exit = new javax.swing.JMenuItem();
        New = new javax.swing.JMenuItem();
        Open = new javax.swing.JMenuItem();
        Save = new javax.swing.JMenuItem();
        SaveAs = new javax.swing.JMenuItem();
        Export = new javax.swing.JMenuItem();
        Edit = new javax.swing.JMenu();
        Undo = new javax.swing.JMenuItem();
        Redo = new javax.swing.JMenuItem();
        Bold = new javax.swing.JMenuItem();
        Italicize = new javax.swing.JMenuItem();
        Underline = new javax.swing.JMenuItem();
        HighlightedWords = new javax.swing.JMenuItem();
        Node = new javax.swing.JMenu();
        NewChildNode = new javax.swing.JMenuItem();
        NewSiblingNode = new javax.swing.JMenuItem();
        RenameNode = new javax.swing.JMenuItem();
        DeleteNode = new javax.swing.JMenuItem();
        ExpandAll = new javax.swing.JMenuItem();
        CollapseAll = new javax.swing.JMenuItem();
        Help = new javax.swing.JMenu();
        Overview = new javax.swing.JMenuItem();
        Credits = new javax.swing.JMenuItem();
        Debug = new javax.swing.JMenu();
        Output = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Synthesis 2");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        jSplitPane1.setResizeWeight(0.26);

        jScrollPane1.setViewportView(Tree);

        jSplitPane1.setLeftComponent(jScrollPane1);

        jScrollPane3.setViewportView(Textarea);

        jSplitPane1.setRightComponent(jScrollPane3);

        File.setMnemonic('F');
        File.setText("File");

        New.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        New.setIcon(new javax.swing.ImageIcon(getClass().getResource("/synthesis/images/book_open.png")));
        New.setMnemonic('N');
        New.setText("New");
        New.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NewActionPerformed(evt);
            }
        });
        File.add(New);

        Open.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        Open.setIcon(new javax.swing.ImageIcon(getClass().getResource("/synthesis/images/folder.png")));
        Open.setMnemonic('O');
        Open.setText("Open");
        Open.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OpenActionPerformed(evt);
            }
        });
        File.add(Open);
        File.addSeparator();

        Save.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        Save.setIcon(new javax.swing.ImageIcon(getClass().getResource("/synthesis/images/disk.png")));
        Save.setMnemonic('S');
        Save.setText("Save");
        Save.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SaveActionPerformed(evt);
            }
        });
        File.add(Save);

        SaveAs.setIcon(new javax.swing.ImageIcon(getClass().getResource("/synthesis/images/disk_multiple.png")));
        SaveAs.setMnemonic('A');
        SaveAs.setText("Save As...");
        SaveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SaveAsActionPerformed(evt);
            }
        });
        File.add(SaveAs);

        Export.setIcon(new javax.swing.ImageIcon(getClass().getResource("/synthesis/images/book_next.png")));
        Export.setMnemonic('E');
        Export.setText("Export...");
        Export.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExportActionPerformed(evt);
            }
        });
        File.add(Export);
        
        File.addSeparator();

        Exit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.CTRL_MASK));
        Exit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/synthesis/images/door_out.png")));
        Exit.setMnemonic('X');
        Exit.setText("Exit");
        Exit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExitActionPerformed(evt);
            }
        });
        File.add(Exit);

        Menu.add(File);

        Edit.setMnemonic('E');
        Edit.setText("Edit");

        Undo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
        Undo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/synthesis/images/arrow_undo.png")));
        Undo.setMnemonic('U');
        Undo.setText("Undo");
        Undo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UndoActionPerformed(evt);
            }
        });
        Edit.add(Undo);

        Redo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_MASK));
        Redo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/synthesis/images/arrow_redo.png")));
        Redo.setMnemonic('R');
        Redo.setText("Redo");
        Redo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RedoActionPerformed(evt);
            }
        });
        Edit.add(Redo);
        Edit.addSeparator();

        Bold.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, java.awt.event.InputEvent.CTRL_MASK));
        Bold.setIcon(new javax.swing.ImageIcon(getClass().getResource("/synthesis/images/text_bold.png")));
        Bold.setMnemonic('B');
        Bold.setText("Bold | <strong>");
        Bold.setToolTipText("");
        Bold.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BoldActionPerformed(evt);
            }
        });
        Edit.add(Bold);

        Italicize.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_MASK));
        Italicize.setIcon(new javax.swing.ImageIcon(getClass().getResource("/synthesis/images/text_italic.png")));
        Italicize.setMnemonic('I');
        Italicize.setText("Italicize | <em>");
        Italicize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ItalicizeActionPerformed(evt);
            }
        });
        Edit.add(Italicize);

        Underline.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_U, java.awt.event.InputEvent.CTRL_MASK));
        Underline.setIcon(new javax.swing.ImageIcon(getClass().getResource("/synthesis/images/text_underline.png")));
        Underline.setMnemonic('U');
        Underline.setText("Underline | non-standard");
        Underline.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                UnderlineActionPerformed(evt);
            }
        });
        Edit.add(Underline);

        Edit.addSeparator();
        
        HighlightedWords.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK));
        HighlightedWords.setIcon(new javax.swing.ImageIcon(getClass().getResource("/synthesis/images/page_edit.png")));
        HighlightedWords.setMnemonic('H');
        HighlightedWords.setText("Highlighted Words...");
        HighlightedWords.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
            	HighlightedWordsActionPerformed(evt);
            }
        });
        Edit.add(HighlightedWords);
        
        Menu.add(Edit);

        Node.setMnemonic('N');
        Node.setText("Node");

        NewChildNode.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        NewChildNode.setIcon(new javax.swing.ImageIcon(getClass().getResource("/synthesis/images/folder_user.png")));
        NewChildNode.setMnemonic('N');
        NewChildNode.setText("New Child Node");
        NewChildNode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NewChildNodeActionPerformed(evt);
            }
        });
        Node.add(NewChildNode);

        NewSiblingNode.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_M, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        NewSiblingNode.setIcon(new javax.swing.ImageIcon(getClass().getResource("/synthesis/images/group.png")));
        NewSiblingNode.setMnemonic('S');
        NewSiblingNode.setText("New Sibling Node");
        NewSiblingNode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                NewSiblingNodeActionPerformed(evt);
            }
        });
        Node.add(NewSiblingNode);

        RenameNode.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0));
        RenameNode.setIcon(new javax.swing.ImageIcon(getClass().getResource("/synthesis/images/user_edit.png")));
        RenameNode.setMnemonic('R');
        RenameNode.setText("Rename Node");
        RenameNode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                RenameNodeActionPerformed(evt);
            }
        });
        Node.add(RenameNode);

        DeleteNode.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0));
        DeleteNode.setIcon(new javax.swing.ImageIcon(getClass().getResource("/synthesis/images/user_delete.png")));
        DeleteNode.setMnemonic('D');
        DeleteNode.setText("Delete Node");
        DeleteNode.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DeleteNodeActionPerformed(evt);
            }
        });
        Node.add(DeleteNode);
        Node.addSeparator();

        ExpandAll.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        ExpandAll.setIcon(new javax.swing.ImageIcon(getClass().getResource("/synthesis/images/shape_ungroup_rotated.png")));
        ExpandAll.setMnemonic('E');
        ExpandAll.setText("Expand All");
        ExpandAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ExpandAllActionPerformed(evt);
            }
        });
        Node.add(ExpandAll);

        CollapseAll.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        CollapseAll.setIcon(new javax.swing.ImageIcon(getClass().getResource("/synthesis/images/shape_ungroup.png")));
        CollapseAll.setMnemonic('C');
        CollapseAll.setText("Collapse All");
        CollapseAll.setBorderPainted(true);
        CollapseAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CollapseAllActionPerformed(evt);
            }
        });
        Node.add(CollapseAll);

        Menu.add(Node);

        Help.setMnemonic('H');
        Help.setText("Help");

        Overview.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        Overview.setIcon(new javax.swing.ImageIcon(getClass().getResource("/synthesis/images/information.png")));
        Overview.setMnemonic('O');
        Overview.setText("Overview");
        Overview.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OverviewActionPerformed(evt);
            }
        });
        Help.add(Overview);

        Credits.setIcon(new javax.swing.ImageIcon(getClass().getResource("/synthesis/images/page_white_medal.png")));
        Credits.setMnemonic('C');
        Credits.setText("Credits");
        Credits.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CreditsActionPerformed(evt);
            }
        });
        Help.add(Credits);

        Menu.add(Help);

        Debug.setMnemonic('D');
        Debug.setText("Debug");

        Output.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_MASK));
        Output.setIcon(new javax.swing.ImageIcon(getClass().getResource("/synthesis/images/page_white_wrench.png")));
        Output.setMnemonic('O');
        Output.setText("Output");
        Output.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OutputActionPerformed(evt);
            }
        });
        Debug.add(Output);

        if (DEBUG_MODE) {
            Menu.add(Debug);
        }

        setJMenuBar(Menu);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 682, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 466, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }
}
