package synthesis;

//import java.awt.Shape;
import java.lang.reflect.Field;

//import javax.swing.event.DocumentEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.Element;
import javax.swing.text.GlyphView;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.ParagraphView;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
//
//// From here
//// http://stackoverflow.com/questions/14293879/problems-rendering-markdown-formatting-in-jtextpane
//// Fixes a bug with text pane wrapping where it messed up when dealing with formatted text
//
//@SuppressWarnings("serial")
//class MyStyledEditorKit extends StyledEditorKit {
//	private MyFactory factory;
//
//    @Override
//    public ViewFactory getViewFactory() {
//        if (factory == null) {
//            factory = new MyFactory();
//        }
//        return factory;
//    }
//}
//
//class MyFactory implements ViewFactory {
//    @Override
//    public View create(Element elem) {
//        String kind = elem.getName();
//        if (kind != null) {
//            if (kind.equals(AbstractDocument.ContentElementName)) {
//                return new MyLabelView(elem);
//            } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
//                return new MyParagraphView(elem);
//            } else if (kind.equals(AbstractDocument.SectionElementName)) {
//                return new BoxView(elem, View.Y_AXIS);
//            } else if (kind.equals(StyleConstants.ComponentElementName)) {
//                return new ComponentView(elem);
//            } else if (kind.equals(StyleConstants.IconElementName)) {
//                return new IconView(elem);
//            }
//        }
//        // default to text display
//        return new LabelView(elem);
//    }
//}
//
//class MyParagraphView extends ParagraphView {
//
//    public MyParagraphView(Element elem) {
//        super(elem);
//    }
//    @Override
//    public void removeUpdate(DocumentEvent e, Shape a, ViewFactory f) {
//        super.removeUpdate(e, a, f);
//        resetBreakSpots();
//    }
//    @Override
//    public void insertUpdate(DocumentEvent e, Shape a, ViewFactory f) {
//        super.insertUpdate(e, a, f);
//        resetBreakSpots();
//    }
//
//    private void resetBreakSpots() {
//        for (int i=0; i<layoutPool.getViewCount(); i++) {
//            View v=layoutPool.getView(i);
//            if (v instanceof MyLabelView) {
//                ((MyLabelView)v).resetBreakSpots();
//            }
//        }
//    }
//}
//
//class MyLabelView extends LabelView {
//
//    boolean isResetBreakSpots=false;
//
//    public MyLabelView(Element elem) {
//        super(elem);
//    }
//    @Override
//    public View breakView(int axis, int p0, float pos, float len) {
//        if (axis == View.X_AXIS) {
//            resetBreakSpots();
//        }
//        return super.breakView(axis, p0, pos, len);
//    }
//
//    public void resetBreakSpots() {
//        isResetBreakSpots=true;
//        removeUpdate(null, null, null);
//        isResetBreakSpots=false;
//   }
//
//    @Override
//    public void removeUpdate(DocumentEvent e, Shape a, ViewFactory f) {
//        super.removeUpdate(e, a, f);
//    }
//
//    @Override
//    public void preferenceChanged(View child, boolean width, boolean height) {
//        if (!isResetBreakSpots) {
//            super.preferenceChanged(child, width, height);
//        }
//    }
//}

class MyStyledEditorKit extends StyledEditorKit {
    private MyFactory factory;

    public ViewFactory getViewFactory() {
        if (factory == null) {
            factory = new MyFactory();
        }
        return factory;
    }
}

class MyFactory implements ViewFactory {
    public View create(Element elem) {
        String kind = elem.getName();
        if (kind != null) {
            if (kind.equals(AbstractDocument.ContentElementName)) {
                return new MyLabelView(elem);
            } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                return new ParagraphView(elem);
            } else if (kind.equals(AbstractDocument.SectionElementName)) {
                return new BoxView(elem, View.Y_AXIS);
            } else if (kind.equals(StyleConstants.ComponentElementName)) {
                return new ComponentView(elem);
            } else if (kind.equals(StyleConstants.IconElementName)) {
                return new IconView(elem);
            }
        }

        // default to text display
        return new LabelView(elem);
    }
}

class MyLabelView extends LabelView {
    public MyLabelView(Element elem) {
        super(elem);
    }
    public View breakView(int axis, int p0, float pos, float len) {
        if (axis == View.X_AXIS) {
            resetBreakSpots();
        }
        return super.breakView(axis, p0, pos, len);
    }

    private void resetBreakSpots() {
        try {
            // HACK the breakSpots private fields
            Field f=GlyphView.class.getDeclaredField("breakSpots");
            f.setAccessible(true);
            f.set(this, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}