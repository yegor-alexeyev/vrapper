package net.sourceforge.vrapper.eclipse.ui;

import net.sourceforge.vrapper.utils.CaretType;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Caret;

public class CaretUtils {

    public static Caret createCaret(CaretType caretType, StyledText styledText) {
    	
    	String currentCharacterString = styledText.getTextRange( styledText.getCaretOffset(), 1);
        int width;
        GC gc = new GC(styledText);

    	if (!currentCharacterString.isEmpty()) {
	    	char currentCharacter = currentCharacterString.charAt(0);
	    	System.out.println(currentCharacter);
	        width = gc.getCharWidth(currentCharacter);
    	} else {
	        width = gc.getFontMetrics().getAverageCharWidth();    		
    	}
    	System.out.println(width);

    	final int height = gc.getFontMetrics().getHeight();
        gc.dispose();

        EvilCaret caret = new EvilCaret(styledText, SWT.NULL, height);

        switch (caretType) {
        case VERTICAL_BAR:
            caret.setSize(2, height);
            break;
        case RECTANGULAR:
            caret.setSize(width, height);
            break;
        case LEFT_SHIFTED_RECTANGULAR:
            caret.setSize(width, height);
            caret.setShiftLeft(true);
            break;
        case HALF_RECT:
            caret.setSize(width, height / 2);
            break;
        case UNDERLINE:
            caret.setSize(width, 3);
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
        public void setLocation(int x, int y) {

        	StyledText styledText = (StyledText)getParent();
        	String currentCharacterString = styledText.getTextRange( styledText.getCaretOffset(), 1);
            int width;
            GC gc = new GC(styledText);

        	if (!currentCharacterString.isEmpty()) {
    	    	char currentCharacter = currentCharacterString.charAt(0);
    	    	System.out.println(currentCharacter);
    	        width = gc.getAdvanceWidth(currentCharacter);
        	} else {
    	        width = gc.getFontMetrics().getAverageCharWidth();    		
        	}
        	System.out.println("in setLocation: " + width);

        	
        	setSize(width, getSize().y);
        	
        	
        	if (shiftLeft) {
                x -= getSize().x;
            }
            // Caret is placed top-left above a character but underline and half-block need to be
            // at the bottom. Fix this by offsetting with textHeight and correcting by size.
            super.setLocation(x, y + textHeight - getSize().y);
        }

        private void setShiftLeft(boolean shift) {
            shiftLeft = shift;
        }
    }
}
