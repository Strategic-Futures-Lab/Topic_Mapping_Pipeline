package PY_Helper;

/**
 * Class facilitating progress logs.
 *
 * @author P. Le Bras
 * @version 1
 */
public class LogPrint {

    /**
     * Static class listing ANSI escape codes to control the logs' text format.
     */
    public static class ConsoleColors {
        /** Reset value: regular light grey text. */
        public static final String RESET = "\033[0m";  // Text Reset

        // Regular Colors
        /** Regular black text. */
        public static final String BLACK = "\033[0;30m";   // BLACK
        /** Regular red text. */
        public static final String RED = "\033[0;31m";     // RED
        /** Regular green text. */
        public static final String GREEN = "\033[0;32m";   // GREEN
        /** Regular yellow text. */
        public static final String YELLOW = "\033[0;33m";  // YELLOW
        /** Regular blue text. */
        public static final String BLUE = "\033[0;34m";    // BLUE
        /** Regular purple text. */
        public static final String PURPLE = "\033[0;35m";  // PURPLE
        /** Regular cyan text. */
        public static final String CYAN = "\033[0;36m";    // CYAN
        /** Regular white text. */
        public static final String WHITE = "\033[0;37m";   // WHITE

        // Bold
        /** Bold black text. */
        public static final String BLACK_BOLD = "\033[1;30m";  // BLACK
        /** Bold red text. */
        public static final String RED_BOLD = "\033[1;31m";    // RED
        /** Bold green text. */
        public static final String GREEN_BOLD = "\033[1;32m";  // GREEN
        /** Bold yellow text. */
        public static final String YELLOW_BOLD = "\033[1;33m"; // YELLOW
        /** Bold blue text. */
        public static final String BLUE_BOLD = "\033[1;34m";   // BLUE
        /** Bold purple text. */
        public static final String PURPLE_BOLD = "\033[1;35m"; // PURPLE
        /** Bold cyan text. */
        public static final String CYAN_BOLD = "\033[1;36m";   // CYAN
        /** Bold white text. */
        public static final String WHITE_BOLD = "\033[1;37m";  // WHITE

        // Underline
        /** Underlined black text. */
        public static final String BLACK_UNDERLINED = "\033[4;30m";  // BLACK
        /** Underlined red text. */
        public static final String RED_UNDERLINED = "\033[4;31m";    // RED
        /** Underlined green text. */
        public static final String GREEN_UNDERLINED = "\033[4;32m";  // GREEN
        /** Underlined yellow text. */
        public static final String YELLOW_UNDERLINED = "\033[4;33m"; // YELLOW
        /** Underlined blue text. */
        public static final String BLUE_UNDERLINED = "\033[4;34m";   // BLUE
        /** Underlined purple text. */
        public static final String PURPLE_UNDERLINED = "\033[4;35m"; // PURPLE
        /** Underlined cyan text. */
        public static final String CYAN_UNDERLINED = "\033[4;36m";   // CYAN
        /** Underlined white text. */
        public static final String WHITE_UNDERLINED = "\033[4;37m";  // WHITE

        // Background
        /** Black background. */
        public static final String BLACK_BACKGROUND = "\033[40m";  // BLACK
        /** Red background. */
        public static final String RED_BACKGROUND = "\033[41m";    // RED
        /** Green background. */
        public static final String GREEN_BACKGROUND = "\033[42m";  // GREEN
        /** Yellow background. */
        public static final String YELLOW_BACKGROUND = "\033[43m"; // YELLOW
        /** Blue background. */
        public static final String BLUE_BACKGROUND = "\033[44m";   // BLUE
        /** Purple background. */
        public static final String PURPLE_BACKGROUND = "\033[45m"; // PURPLE
        /** Cyan background. */
        public static final String CYAN_BACKGROUND = "\033[46m";   // CYAN
        /** White background. */
        public static final String WHITE_BACKGROUND = "\033[47m";  // WHITE

        // High Intensity
        /** High intensity black text. */
        public static final String BLACK_BRIGHT = "\033[0;90m";  // BLACK
        /** High intensity red text. */
        public static final String RED_BRIGHT = "\033[0;91m";    // RED
        /** High intensity green text. */
        public static final String GREEN_BRIGHT = "\033[0;92m";  // GREEN
        /** High intensity yellow text. */
        public static final String YELLOW_BRIGHT = "\033[0;93m"; // YELLOW
        /** High intensity blue text. */
        public static final String BLUE_BRIGHT = "\033[0;94m";   // BLUE
        /** High intensity purple text. */
        public static final String PURPLE_BRIGHT = "\033[0;95m"; // PURPLE
        /** High intensity cyan text. */
        public static final String CYAN_BRIGHT = "\033[0;96m";   // CYAN
        /** High intensity white text. */
        public static final String WHITE_BRIGHT = "\033[0;97m";  // WHITE

        // Bold High Intensity
        /** Bold high intensity black text. */
        public static final String BLACK_BOLD_BRIGHT = "\033[1;90m"; // BLACK
        /** Bold high intensity red text. */
        public static final String RED_BOLD_BRIGHT = "\033[1;91m";   // RED
        /** Bold high intensity green text. */
        public static final String GREEN_BOLD_BRIGHT = "\033[1;92m"; // GREEN
        /** Bold high intensity yellow text. */
        public static final String YELLOW_BOLD_BRIGHT = "\033[1;93m";// YELLOW
        /** Bold high intensity blue text. */
        public static final String BLUE_BOLD_BRIGHT = "\033[1;94m";  // BLUE
        /** Bold high intensity purple text. */
        public static final String PURPLE_BOLD_BRIGHT = "\033[1;95m";// PURPLE
        /** Bold high intensity cyan text. */
        public static final String CYAN_BOLD_BRIGHT = "\033[1;96m";  // CYAN
        /** Bold high intensity white text. */
        public static final String WHITE_BOLD_BRIGHT = "\033[1;97m"; // WHITE

        // High Intensity backgrounds
        /** High intensity black background. */
        public static final String BLACK_BACKGROUND_BRIGHT = "\033[0;100m";// BLACK
        /** High intensity red background. */
        public static final String RED_BACKGROUND_BRIGHT = "\033[0;101m";// RED
        /** High intensity green background. */
        public static final String GREEN_BACKGROUND_BRIGHT = "\033[0;102m";// GREEN
        /** High intensity yellow background. */
        public static final String YELLOW_BACKGROUND_BRIGHT = "\033[0;103m";// YELLOW
        /** High intensity blue background. */
        public static final String BLUE_BACKGROUND_BRIGHT = "\033[0;104m";// BLUE
        /** High intensity purple background. */
        public static final String PURPLE_BACKGROUND_BRIGHT = "\033[0;105m"; // PURPLE
        /** High intensity cyan background. */
        public static final String CYAN_BACKGROUND_BRIGHT = "\033[0;106m";  // CYAN
        /** High intensity white background. */
        public static final String WHITE_BACKGROUND_BRIGHT = "\033[0;107m";   // WHITE
    }

    /** Static list of asterisks inserted at the start and end of each module. */
    private static final String stars = ConsoleColors.BLUE+"\n****************************************\n"+ConsoleColors.RESET;

    /**
     * Method logging the start of a module (blue).
     * @param moduleName Module's name.
     */
    public static void printModuleStart(String moduleName){
        System.out.print(stars+" STARTING "+moduleName+" \n");
    }

    /**
     * Method logging the start of a sub-module (blue).
     * @param moduleName Sub-module's name.
     */
    public static void printSubModuleStart(String moduleName){
        System.out.print(ConsoleColors.BLUE+"\n| "+moduleName+ConsoleColors.RESET);
    }

    /**
     * Method logging the end of a sub-module (blue).
     */
    public static void printSubModuleEnd(){
        System.out.print(ConsoleColors.BLUE+"\n|====================="+ConsoleColors.RESET);
    }

    /**
     * Method logging the end of a module (blue).
     * @param moduleName Module's name.
     */
    public static void printModuleEnd(String moduleName){
        System.out.println("\n\n "+moduleName+" COMPLETED "+stars);
    }

    /**
     * Method logging a note (yellow) with no indentation.
     * @param msg Note to log.
     */
    public static void printNote(String msg){
        printNote(msg, 0);
    }

    /**
     * Method logging a note (yellow) at a given indentation.
     * @param msg Note to log.
     * @param depth Indentation level, insert a double space per level.
     */
    public static void printNote(String msg, int depth){
        String tab = "  ".repeat(depth);
        String arrow = " ➡ ";
        System.out.print("\n"+tab+ConsoleColors.YELLOW+arrow+msg+ConsoleColors.RESET);
    }

    /**
     * Method logging an error (red) with no indentation.
     * @param msg Error to log.
     */
    public static void printNoteError(String msg){
        printNoteError(msg, 0);
    }

    /**
     * Method logging an error (red) at a given indentation.
     * @param msg Error to log.
     * @param depth Indentation level, insert a double space per level.
     */
    public static void printNoteError(String msg, int depth){
        String tab = "  ".repeat(depth);
        String arrow = "➡ ";
        System.out.print("\n"+tab+ConsoleColors.RED+arrow+msg+ConsoleColors.RESET);
    }

    /**
     * Method logging the start of a new step in a module's process, ie log with a bullet, at a given indentation.
     * @param msg Step message.
     * @param depth Indentation level, insert a double space per level.
     */
    public static void printNewStep(String msg, int depth){
        String tab = "  ".repeat(depth);
        System.out.print("\n"+tab+" - "+msg);
    }

    /**
     * Method logging a simple message, at a given indentation.
     * @param msg Message.
     * @param depth Indentation level, insert a double space per level.
     */
    public static void printLog(String msg, int depth){
        String tab = "  ".repeat(depth);
        System.out.print("\n"+tab+"   "+msg);
    }

    /**
     * Method logging a message from a sub-process (purple), at a given indentation.
     * @param msg Message.
     * @param depth Indentation level, insert a double space per level.
     * @deprecated Was used to print logs from Node js.
     */
    @Deprecated
    public static void printExternalLog(String msg, int depth){
        String tab = "  ".repeat(depth);
        System.out.print("\n"+ConsoleColors.PURPLE+tab+"   "+msg+ConsoleColors.RESET);
    }

    /**
     * Method logging the completion of a step with a custom message (appended to previous log).
     * @param msg Completion message.
     */
    public static void printCompleteStep(String msg){
        System.out.print(ConsoleColors.GREEN_BRIGHT+" "+msg+ConsoleColors.RESET);
    }

    /**
     * Method logging the completion of a step with a tick mark (appended to previous log).
     */
    public static void printCompleteStep(){
        printCompleteStep("✔");
    }
}
