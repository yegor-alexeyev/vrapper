package net.sourceforge.vrapper.eclipse.ui;

import net.sourceforge.vrapper.utils.CaretType;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.internal.DPIUtil;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Caret;

public class CaretUtils {

	@SuppressWarnings("restriction")
	public static Point getCaretDimensions(StyledText styledText) {
        int width;
        GC gc = new GC(styledText);
        if (styledText.getCaretOffset() + 1 <= styledText.getCharCount()) {
	    	String currentCharacterString = styledText.getTextRange( styledText.getCaretOffset(), 1);
	
	    	if (currentCharacterString.isEmpty()) {
		        width = gc.getFontMetrics().getAverageCharWidth()*3/4;    		
	    	} else {
		    	char currentCharacter = currentCharacterString.charAt(0);		    	
		        width = currentCharacter == ' ' ? gc.getFontMetrics().getAverageCharWidth()*3/4 : DPIUtil.autoScaleDown(gc.getCharWidth(currentCharacter));
	    	}
    	} else {
    		width = gc.getFontMetrics().getAverageCharWidth()*3/4;
    	}
    	final int height = gc.getFontMetrics().getHeight();
        gc.dispose();
        return new Point(width, height);
	}
	
    public static Caret createCaret(CaretType caretType, StyledText styledText) {

    	Point caretDimensions = CaretUtils.getCaretDimensions(styledText);


        EvilCaret caret = new EvilCaret(styledText, SWT.NULL, caretDimensions.y);

        switch (caretType) {
        case VERTICAL_BAR:
            caret.setSize(2, caretDimensions.y);
            break;
        case RECTANGULAR:
            caret.setSize(caretDimensions);
            break;
        case LEFT_SHIFTED_RECTANGULAR:
            caret.setSize(caretDimensions);
            caret.setShiftLeft(true);
            break;
        case HALF_RECT:
            caret.setSize(caretDimensions.x, caretDimensions.y / 2);
            break;
        case UNDERLINE:
            caret.setSize(caretDimensions.x, 3);
            break;
        }

        return caret;
    }

    // XXX: this is EXTREMALLY evil :->
    private static final class EvilCaret extends Caret {
        private final int textHeight;
        private boolean shiftLeft;

        private EvilCaret(Canvas parent, int style, int textHeight) {
            super(parent, style);
            this.textHeight = textHeight;
        }

        @Override
        protected void checkSubclass() {}

        @Override
        public void setLocation(Point location) {

        	StyledText styledText = (StyledText)getParent();

        	Point caretDimensions = CaretUtils.getCaretDimensions(styledText);

        	
        	setSize(caretDimensions);
//        	setSize(caretDimensions.x, getSize().y);
        	
        	
            // Caret is placed top-left above a character but underline and half-block need to be
            // at the bottom. Fix this by offsetting with textHeight and correcting by size.
            super.setLocation(shiftLeft ? location.x - getSize().x : location.x, location.y + textHeight - getSize().y);
        }

        private void setShiftLeft(boolean shift) {
            shiftLeft = shift;
        }
    }
}
