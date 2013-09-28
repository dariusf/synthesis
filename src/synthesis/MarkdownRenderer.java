


// this class is out of date. refer to the one in test which is being developed
// then i can just plug it in here

package synthesis;
//
//import java.util.logging.*;
//import java.util.regex.*;
//import javax.swing.*;
//import javax.swing.text.*;
//
class MarkdownRenderer {    
//    private static Style PLAIN = null;
//    private static Style BOLD = null;
//    private static Style ITALIC = null;
//    private static Style UNDERLINE = null;
//    private static Style HEADER = null;
//    
//    private DefaultStyledDocument Document = null;
//
//    public MarkdownRenderer(JTextPane editor) {
//        Document = (DefaultStyledDocument) editor.getDocument();
//
//        PLAIN = Document.addStyle("plain", null);
//        StyleConstants.setFontSize(PLAIN, 12);
//        
//        BOLD = Document.addStyle("bold", null);
//        StyleConstants.setBold(BOLD, true);
//
//        ITALIC = Document.addStyle("italic", null);
//        StyleConstants.setItalic(ITALIC, true);
//
//        UNDERLINE = Document.addStyle("underline", null);
//        StyleConstants.setUnderline(UNDERLINE, true);
//
//        HEADER = Document.addStyle("header", null);
//        StyleConstants.setFontSize(HEADER, 24);
//    }
//    
//    void render() {
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
////        m = Pattern.compile("(.+)\\n=+\\n").matcher(temp);
////        while (m.find()) {
////            Document.setCharacterAttributes(m.start(), m.group().length(), HEADER, false);
////        }
////        Matcher n = Pattern.compile("(\\#{1,6})[\\s]*(.+?)[\\s]*\\#*\\n").matcher(temp);
////        while (n.find()) {
////            Document.setCharacterAttributes(n.start(), n.group().length(), HEADER, false);
////        }
//    }
}