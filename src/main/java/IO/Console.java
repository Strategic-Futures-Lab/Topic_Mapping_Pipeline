package IO;

/*
    ANSI COMMAND: \033[<codes>m
    ANSI CODES (sep with ;):
        - regular text: 0
        - bold text: 1
        - italic text: 3
        - underlined text: 4
        - strikethrough text: 9
        - set text color: 3x
        - set text color bright: 9x
        - set back color: 4x
        - set back color bright: 10x
    ANSI COLORS:
        - Black: 0
        - Red: 1
        - Green: 2
        - Yellow: 3
        - Blue: 4
        - Purple: 5
        - Cyan: 6
        - White: 7
    RESET COMMAND: \033[0m
*/

public class Console {

    private static final int SIZE = 80;

    // common ANSI commands
    private static final String ANSI_RESET = "\033[0m";
    private static final String ANSI_RED_EMPHASIS = "\033[1;4;31m";
    private static final String ANSI_RED = "\033[31m";
    private static final String ANSI_RED_BG = "\033[1;41;30m";
    private static final String ANSI_BRIGHT_RED = "\033[91m";
    private static final String ANSI_GREEN_EMPHASIS = "\033[1;4;32m";
    private static final String ANSI_GREEN = "\033[32m";
    private static final String ANSI_GREEN_BG = "\033[1;42;30m";
    private static final String ANSI_BRIGHT_GREEN = "\033[92m";
    private static final String ANSI_YELLOW_EMPHASIS = "\033[1;4;33m";
    private static final String ANSI_YELLOW = "\033[33m";
    private static final String ANSI_BRIGHT_YELLOW = "\033[93m";
    private static final String ANSI_BLUE_EMPHASIS = "\033[1;4;34m";
    private static final String ANSI_BLUE = "\033[34m";
    private static final String ANSI_BLUE_BG = "\033[1;44;30m";
    private static final String ANSI_PURPLE = "\033[35m";

    // common methods
    private static String tabs(int depth){ return "  ".repeat(depth); }
    private static String spaces(int depth){ return " ".repeat(depth); }
    private static void print(String str){
        System.out.print("\n"+str);
    }
    private static String center(String str, int size){
        int pad = size - str.length();
        int left = pad / 2;
        int right = (pad%2==0) ? pad/2 : pad/2+1;
        return spaces(left)+str+spaces(right);
    }
    private static String center(String str){ return center(str, SIZE); }
    private static String left(String str, int size){
        int pad = size - str.length();
        return str+spaces(pad);
    }
    private static String left(String str){ return left(str, SIZE); }
    private static String right(String str, int size){
        int pad = size - str.length();
        return spaces(pad)+str;
    }
    private static String right(String str){ return right(str, SIZE); }
    private static String redEmphasis(String str){ return ANSI_RED_EMPHASIS+str+ANSI_RESET; }
    private static String red(String str){ return ANSI_RED+str+ANSI_RESET; }
    private static String redBack(String str){ return ANSI_RED_BG+str+ANSI_RESET; }
    private static String brightRed(String str){ return ANSI_BRIGHT_RED+str+ANSI_RESET; }
    private static String greenEmphasis(String str){ return ANSI_GREEN_EMPHASIS+str+ANSI_RESET; }
    private static String green(String str){ return ANSI_GREEN+str+ANSI_RESET; }
    private static String greenBack(String str){ return ANSI_GREEN_BG+str+ANSI_RESET; }
    private static String brightGreen(String str){ return ANSI_BRIGHT_GREEN+str+ANSI_RESET; }
    private static String yellowEmphasis(String str){ return ANSI_YELLOW_EMPHASIS+str+ANSI_RESET; }
    private static String yellow(String str){ return ANSI_YELLOW+str+ANSI_RESET; }
    private static String brightYellow(String str){ return ANSI_BRIGHT_YELLOW+str+ANSI_RESET; }
    private static String blueEmphasis(String str){ return ANSI_BLUE_EMPHASIS+str+ANSI_RESET; }
    private static String blue(String str){ return ANSI_BLUE+str+ANSI_RESET; }
    private static String blueBack(String str){ return ANSI_BLUE_BG+str+ANSI_RESET; }
    private static String purple(String str){ return ANSI_PURPLE+str+ANSI_RESET; }

    /** Arrow character */
    public static final String ARROW = "➡";
    /** Tick mark character */
    public static final String TICK = "✔";
    /** Cross mark character */
    public static final String CROSS = "❌";
    /** Warning trinagle character */
    public static final String WARNING = "⚠";

    /**
     * Prints a top level error, typically halting execution
     * @param msg Error message
     */
    public static void error(String msg){
        error(msg, 0);
    }

    /**
     * Prints an error, at a given depth
     * @param msg Error message
     * @param depth Indentation level
     */
    public static void error(String msg, int depth){
        print(tabs(depth)+redEmphasis("Error:")+" "+red(msg));
    }

    /**
     * Prints a top level warning
     * @param msg Warning message
     */
    public static void warning(String msg){
        warning(msg, 0);
    }

    /**
     * Prints a warning, at a given depth
     * @param msg Warning message
     * @param depth Indentation level
     */
    public static void warning(String msg, int depth){
        print(tabs(depth)+yellowEmphasis("Warning:")+" "+yellow(msg));
    }

    /**
     * Prints a top level success
     * @param msg Success message
     */
    public static void success(String msg){
        success(msg, 0);
    }

    /**
     * Prints a success, at a given depth
     * @param msg Success message
     * @param depth Indentation level
     */
    public static void success(String msg, int depth){
        print(tabs(depth)+greenEmphasis("Success:")+" "+green(msg));
    }

    /**
     * Prints a top level info
     * @param msg Info message
     */
    public static void info(String msg){
        info(msg, 0);
    }

    /**
     * Prints an info, at a given depth
     * @param msg Info message
     * @param depth Indentation level
     */
    public static void info(String msg, int depth){
        print(tabs(depth)+blueEmphasis("Info:")+" "+blue(msg));
    }

    /**
     * Prints a regular log at top level
     * @param msg Log message
     */
    public static void log(String msg){
        log(msg, 0);
    }

    /**
     * Prints a regular log at a given depth
     * @param msg Log message
     * @param depth Indentation level
     */
    public static void log(String msg, int depth){
        print(tabs(depth)+msg);
    }

    /**
     * Prints a bulleted log at top level
     * @param msg Log message
     */
    public static void step(String msg){
        log("- "+msg, 0);
    }

    /**
     * Prints a bulleted log at a given depth
     * @param msg Log message
     * @param depth Indentation level
     */
    public static void step(String msg, int depth){
        log("- "+msg, depth);
    }

    /**
     * Prints a note at top level
     * @param msg Note message
     */
    public static void note(String msg){
        note(msg, 0);
    }

    /**
     * Prints a note at a given depth
     * @param msg Note message
     * @param depth Indentation level
     */
    public static void note(String msg, int depth){
        print(tabs(depth)+ARROW+" "+msg);
    }

    /**
     * Prints a bright green tick mark on the current line
     */
    public static void tick(){
        System.out.print(tabs(1)+brightGreen(TICK));
    }

    /**
     * Prints a bright red cross mark on the current line
     */
    public static void cross(){
        System.out.print(tabs(1)+brightRed(CROSS));
    }

    /**
     * Prints a bright yellow warning triangle on the current line
     */
    public static void warn(){
        System.out.print(tabs(1)+brightYellow(WARNING));
    }

    /**
     * Prints a blue banner to mark the start of a module
     * @param module Module starting
     */
    public static void moduleStart(String module){
        print(blueBack(center("** Starting "+module+" **"))+"\n");
    }

    /**
     * Prints a green banner to mark the completion of a module
     * @param module Module completed
     */
    public static void moduleComplete(String module){
        print(greenBack(center("** "+module+" complete! **"))+"\n\n");
    }

    /**
     * Prints a red banner to mark the failure of a module
     * @param module Module failed
     */
    public static void moduleFail(String module){
        print(redBack(center("** "+module+" failed! **"))+"\n\n");
    }

    /**
     * Prints a blue banner to mark the start of a submodule
     * @param module Submodule starting
     */
    public static void submoduleStart(String module){
        print(blue(">>> ")+blueEmphasis("Starting "+module));
    }

    /**
     * Prints a green banner to mark the completion of a submodule
     * @param module Submodule completed
     */
    public static void submoduleComplete(String module){
        print(green(">>> ")+greenEmphasis(module+" complete!")+"\n");
    }

    /**
     * Prints a red banner to mark the failure of a submodule
     * @param module Submodule failed
     */
    public static void submoduleFail(String module){
        print(red(">>> ")+redEmphasis(module+" failed!")+"\n");
    }

}
