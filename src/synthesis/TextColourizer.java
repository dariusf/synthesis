
package synthesis;

import java.awt.Color;
import java.util.logging.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.text.*;

class TextColourizer {    
    private static Style PLAIN = null;
//    private static Style BOLD = null;
//    private static Style ITALIC = null;
//    private static Style UNDERLINE = null;
//    private static Style HEADER = null;
    
    private DefaultStyledDocument Document = null;
    private JTextPane Editor = null;

//    private String[] words = new String[] {
//    	"[V|v]aruys",
//    	"[A|a]ndra(dormi)?",
//    	"Source"
//    };
    public TextColourizer(JTextPane editor) {
    	Editor = editor;
        Document = (DefaultStyledDocument) editor.getDocument();

        PLAIN = Document.addStyle("plain", null);
        StyleConstants.setFontSize(PLAIN, Synthesis.SYNTHESIS_FONT.getSize());
    }
    
    void normalize() {
    	Document.setCharacterAttributes(0, Document.getLength(), PLAIN, true);
    }
    
    void render() {
//        DefaultStyledDocument current = Document;
//        int pos = Editor.getCaretPosition();
//        Editor.setDocument(new DefaultStyledDocument());

    	String s = "";
    	try {
    		s = Document.getText(0, Document.getLength());
    	} catch (BadLocationException ex) {
    		Logger.getLogger(MarkdownRenderer.class.getName()).log(Level.SEVERE, null, ex);
    	}
    	normalize();

    	for (int i=0; i<Synthesis.That.ColInfo.colours.size(); i++) {
	    	MutableAttributeSet attributes = new SimpleAttributeSet();  
	    	StyleConstants.setForeground(attributes, Synthesis.That.ColInfo.colours.get(i)); // new Color(0,255,0) Color.blue

//	    	String temp = s.replaceAll("\\*\\*([^\\n*]+)\\*\\*", "|`$1`|");
	    	for (int j=0; j<Synthesis.That.ColInfo.keywords.get(i).size(); j++) {
	        	Matcher m = Pattern.compile(Synthesis.That.ColInfo.keywords.get(i).get(j)).matcher(s);
	        	while (m.find()) {
	            	Document.setCharacterAttributes(m.start(), m.group().length(), attributes, false);
	        	}
	    	}
		}

//		Editor.setDocument(current);
//		Editor.setCaretPosition(pos);


//        
//        String s = "";
//        try {
//            s = Document.getText(0, Document.getLength());
//        } catch (BadLocationException ex) {
//            Logger.getLogger(MarkdownRenderer.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        Document.setCharacterAttributes(0, Document.getLength(), PLAIN, true);
//
//        String temp = s.replaceAll("\\*\\*([^\\n*]+)\\*\\*", "|`$1`|"); // can also use lazy quantifier: (.+?)
//        
//        Matcher m = Pattern.compile("\\|`.+?`\\|").matcher(temp);
//        while (m.find()) {
//            Document.setCharacterAttributes(m.start(), m.group().length(), BOLD, false);
//        }
//        m = Pattern.compile("\\*([^\\n*]+)\\*").matcher(temp);
//        while (m.find()) {
//            Document.setCharacterAttributes(m.start(), m.group().length(), ITALIC, false);
//        }
//        m = Pattern.compile("_+([^\\n*]+)_+").matcher(temp);
//        while (m.find()) {
//            Document.setCharacterAttributes(m.start(), m.group().length(), UNDERLINE, false);
//        }
        
        
//        m = Pattern.compile("(.+)\\n=+\\n").matcher(temp);
//        while (m.find()) {
//            Document.setCharacterAttributes(m.start(), m.group().length(), HEADER, false);
//        }
//        Matcher n = Pattern.compile("(\\#{1,6})[\\s]*(.+?)[\\s]*\\#*\\n").matcher(temp);
//        while (n.find()) {
//            Document.setCharacterAttributes(n.start(), n.group().length(), HEADER, false);
//        }
    }
}