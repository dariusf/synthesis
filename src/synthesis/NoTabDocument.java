package synthesis;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

@SuppressWarnings("serial")
class NoTabDocument extends DefaultStyledDocument {
	@Override
    public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
            str = str.replaceAll("\t", "");
            super.insertString(offs, str, a);
    }
}