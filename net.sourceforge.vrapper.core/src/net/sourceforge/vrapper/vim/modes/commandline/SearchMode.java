package net.sourceforge.vrapper.vim.modes.commandline;

import java.util.LinkedList;

import net.sourceforge.vrapper.keymap.KeyStroke;
import net.sourceforge.vrapper.platform.Configuration.Option;
import net.sourceforge.vrapper.platform.CursorService;
import net.sourceforge.vrapper.platform.SearchAndReplaceService;
import net.sourceforge.vrapper.platform.VrapperPlatformException;
import net.sourceforge.vrapper.utils.Position;
import net.sourceforge.vrapper.utils.Search;
import net.sourceforge.vrapper.utils.SearchOffset;
import net.sourceforge.vrapper.utils.SearchResult;
import net.sourceforge.vrapper.utils.VimUtils;
import net.sourceforge.vrapper.vim.ConfigurationListener;
import net.sourceforge.vrapper.vim.EditorAdaptor;
import net.sourceforge.vrapper.vim.Options;
import net.sourceforge.vrapper.vim.commands.Command;
import net.sourceforge.vrapper.vim.commands.CommandExecutionException;
import net.sourceforge.vrapper.vim.commands.MotionCommand;
import net.sourceforge.vrapper.vim.commands.Selection;
import net.sourceforge.vrapper.vim.commands.motions.StickyColumnPolicy;
import net.sourceforge.vrapper.vim.modes.ExecuteCommandHint;
import net.sourceforge.vrapper.vim.modes.ModeSwitchHint;

public class SearchMode extends AbstractCommandLineMode {

    protected class SearchConfigurationListener implements
            ConfigurationListener {
        
        private EditorAdaptor vim;

        public SearchConfigurationListener(EditorAdaptor vim) {
            this.vim = vim;
        }

        public <T> void optionChanged(Option<T> option, T oldValue, T newValue) {
            if (option.equals(Options.SMART_CASE)
                    || option.equals(Options.IGNORE_CASE)
                    || option.equals(Options.SEARCH_HIGHLIGHT)
                    || option.equals(Options.SEARCH_HL_SCOPE)
                    || option.equals(Options.SEARCH_REGEX)) {
                Search lastSearch = vim.getRegisterManager().getSearch();
                // Update the search settings for the last/active search.
                if (lastSearch != null) {
                    lastSearch = SearchCommandParser.createSearch(vim, lastSearch.getKeyword(),
                            lastSearch.isBackward(), lastSearch.getSearchOffset());
                    vim.getRegisterManager().setSearch(lastSearch);
                    
                    if (Options.SEARCH_HIGHLIGHT.equals(option) && Boolean.FALSE.equals(newValue)) {
                        try {
                            HighlightSearch.CLEAR_HIGHLIGHT.evaluate(vim, new LinkedList<String>());
                        } catch (CommandExecutionException e) {
                            vim.getUserInterfaceService().setErrorMessage(e.getMessage());
                        }

                    // Update highlights when enabled, as search might now match more things
                    } else if (vim.getConfiguration().get(Options.SEARCH_HIGHLIGHT)) {
                        try {
                            HighlightSearch.HIGHLIGHT.evaluate(vim, new LinkedList<String>());
                        } catch (CommandExecutionException e) {
                            vim.getUserInterfaceService().setErrorMessage(e.getMessage());
                        }
                    }
                }
            }
        }
    }

    public static final String NAME = "search mode";
    public static final String DISPLAY_NAME = "SEARCH";

    private Boolean forward;
    private Position startPos;
    private int originalTopLine;
    private Command command;
    private SearchCommandParser searchParser;

    public SearchMode(EditorAdaptor editorAdaptor) {
        super(editorAdaptor);
        editorAdaptor.getConfiguration().addListener(new SearchConfigurationListener(editorAdaptor));
    }

    /**
     * @param args {@link Direction} of the search
     */
    @Override
    public void enterMode(ModeSwitchHint... args) throws CommandExecutionException {
        forward = null;
        command = null;
        for (ModeSwitchHint hint : args) {
            if (hint instanceof Direction) {
                forward = hint.equals(Direction.FORWARD);
            } else if (hint instanceof ExecuteCommandHint.OnLeave) {
                command = ((ExecuteCommandHint.OnLeave) hint).getCommand();
            }
        }
        if (forward == null || command == null) {
            throw new CommandExecutionException("Wrong number of hints passed to search mode!");
        }
        startPos = editorAdaptor.getCursorService().getPosition();
        originalTopLine = editorAdaptor.getViewportService().getViewPortInformation().getTopLine();
        searchParser = new SearchCommandParser(editorAdaptor, command);
        super.enterMode(args);
    }

    @Override
    protected AbstractCommandParser createParser() {
        return searchParser;
    }

    @Override
    public boolean handleKey(KeyStroke stroke) {
        boolean incsearch = editorAdaptor.getConfiguration().get(Options.INCREMENTAL_SEARCH);
        if (incsearch &&
                (stroke.equals(AbstractCommandParser.KEY_RETURN) ||
                    stroke.equals(AbstractCommandParser.KEY_ESCAPE))) {
                resetIncSearch();
        }
        super.handleKey(stroke);
        if (incsearch && isEnabled) {
            // isEnabled == false indicates that super method ran a search and went to normal mode.
            doIncSearch();
        }
        return true;
    }

    private void resetIncSearch() {
        editorAdaptor.getSearchAndReplaceService().removeIncSearchHighlighting();
        editorAdaptor.getCursorService().setPosition(startPos, StickyColumnPolicy.NEVER);
        editorAdaptor.getViewportService().setTopLine(originalTopLine);
    }

    private void doIncSearch() {
        String keyword = searchParser.getKeyWord();
        Search s = SearchCommandParser.createSearch(editorAdaptor, keyword, !forward, SearchOffset.NONE);
        CursorService cursorService = editorAdaptor.getCursorService();
        int fixedPos = startPos.getModelOffset() + (forward ? 1 : -1);
        Position startSearchPos = cursorService.newPositionForModelOffset(fixedPos, startPos, true);
        SearchResult res;
        try {
            res = VimUtils.wrapAroundSearch(editorAdaptor, s, startSearchPos);
        } catch (VrapperPlatformException e) {
            // This might happen if the user is modifying a regex, making it invalid. Bail out.
            resetIncSearch();
            return;
        }
        boolean fromVisual = parser.isFromVisual();
        if (res.isFound()) {
            MotionCommand.gotoAndChangeViewPort(editorAdaptor, res.getStart(), StickyColumnPolicy.NEVER);

            if (fromVisual) {
                Selection lastSel = editorAdaptor.getLastActiveSelection();
                Selection updated = lastSel.reset(editorAdaptor, lastSel.getFrom(), res.getStart());
                editorAdaptor.setSelection(updated);
            } else {
                SearchAndReplaceService sars = editorAdaptor.getSearchAndReplaceService();
                sars.incSearchhighlight(res.getStart(), res.getModelLength());
            }
        } else {
            resetIncSearch();
            if (fromVisual) {
                editorAdaptor.setSelection(editorAdaptor.getLastActiveSelection());
            }
        }
    }

    @Override
    public String resolveKeyMap(KeyStroke stroke) {
        return COMMANDLINE_KEYMAP_NAME;
    }

    @Override
    protected String getPrompt() {
        return forward ? "/" : "?";
    }

    public String getName() {
        return NAME;
    }

    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    public enum Direction implements ModeSwitchHint {
        FORWARD, BACKWARD;
    }


}
