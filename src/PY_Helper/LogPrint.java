package PY_Helper;

public class LogPrint {

    public class ConsoleColors {
        // Reset
        public static final String RESET = "\033[0m";  // Text Reset

        // Regular Colors
        public static final String BLACK = "\033[0;30m";   // BLACK
        public static final String RED = "\033[0;31m";     // RED
        public static final String GREEN = "\033[0;32m";   // GREEN
        public static final String YELLOW = "\033[0;33m";  // YELLOW
        public static final String BLUE = "\033[0;34m";    // BLUE
        public static final String PURPLE = "\033[0;35m";  // PURPLE
        public static final String CYAN = "\033[0;36m";    // CYAN
        public static final String WHITE = "\033[0;37m";   // WHITE

        // Bold
        public static final String BLACK_BOLD = "\033[1;30m";  // BLACK
        public static final String RED_BOLD = "\033[1;31m";    // RED
        public static final String GREEN_BOLD = "\033[1;32m";  // GREEN
        public static final String YELLOW_BOLD = "\033[1;33m"; // YELLOW
        public static final String BLUE_BOLD = "\033[1;34m";   // BLUE
        public static final String PURPLE_BOLD = "\033[1;35m"; // PURPLE
        public static final String CYAN_BOLD = "\033[1;36m";   // CYAN
        public static final String WHITE_BOLD = "\033[1;37m";  // WHITE

        // Underline
        public static final String BLACK_UNDERLINED = "\033[4;30m";  // BLACK
        public static final String RED_UNDERLINED = "\033[4;31m";    // RED
        public static final String GREEN_UNDERLINED = "\033[4;32m";  // GREEN
        public static final String YELLOW_UNDERLINED = "\033[4;33m"; // YELLOW
        public static final String BLUE_UNDERLINED = "\033[4;34m";   // BLUE
        public static final String PURPLE_UNDERLINED = "\033[4;35m"; // PURPLE
        public static final String CYAN_UNDERLINED = "\033[4;36m";   // CYAN
        public static final String WHITE_UNDERLINED = "\033[4;37m";  // WHITE

        // Background
        public static final String BLACK_BACKGROUND = "\033[40m";  // BLACK
        public static final String RED_BACKGROUND = "\033[41m";    // RED
        public static final String GREEN_BACKGROUND = "\033[42m";  // GREEN
        public static final String YELLOW_BACKGROUND = "\033[43m"; // YELLOW
        public static final String BLUE_BACKGROUND = "\033[44m";   // BLUE
        public static final String PURPLE_BACKGROUND = "\033[45m"; // PURPLE
        public static final String CYAN_BACKGROUND = "\033[46m";   // CYAN
        public static final String WHITE_BACKGROUND = "\033[47m";  // WHITE

        // High Intensity
        public static final String BLACK_BRIGHT = "\033[0;90m";  // BLACK
        public static final String RED_BRIGHT = "\033[0;91m";    // RED
        public static final String GREEN_BRIGHT = "\033[0;92m";  // GREEN
        public static final String YELLOW_BRIGHT = "\033[0;93m"; // YELLOW
        public static final String BLUE_BRIGHT = "\033[0;94m";   // BLUE
        public static final String PURPLE_BRIGHT = "\033[0;95m"; // PURPLE
        public static final String CYAN_BRIGHT = "\033[0;96m";   // CYAN
        public static final String WHITE_BRIGHT = "\033[0;97m";  // WHITE

        // Bold High Intensity
        public static final String BLACK_BOLD_BRIGHT = "\033[1;90m"; // BLACK
        public static final String RED_BOLD_BRIGHT = "\033[1;91m";   // RED
        public static final String GREEN_BOLD_BRIGHT = "\033[1;92m"; // GREEN
        public static final String YELLOW_BOLD_BRIGHT = "\033[1;93m";// YELLOW
        public static final String BLUE_BOLD_BRIGHT = "\033[1;94m";  // BLUE
        public static final String PURPLE_BOLD_BRIGHT = "\033[1;95m";// PURPLE
        public static final String CYAN_BOLD_BRIGHT = "\033[1;96m";  // CYAN
        public static final String WHITE_BOLD_BRIGHT = "\033[1;97m"; // WHITE

        // High Intensity backgrounds
        public static final String BLACK_BACKGROUND_BRIGHT = "\033[0;100m";// BLACK
        public static final String RED_BACKGROUND_BRIGHT = "\033[0;101m";// RED
        public static final String GREEN_BACKGROUND_BRIGHT = "\033[0;102m";// GREEN
        public static final String YELLOW_BACKGROUND_BRIGHT = "\033[0;103m";// YELLOW
        public static final String BLUE_BACKGROUND_BRIGHT = "\033[0;104m";// BLUE
        public static final String PURPLE_BACKGROUND_BRIGHT = "\033[0;105m"; // PURPLE
        public static final String CYAN_BACKGROUND_BRIGHT = "\033[0;106m";  // CYAN
        public static final String WHITE_BACKGROUND_BRIGHT = "\033[0;107m";   // WHITE
    }

    private static String stars = ConsoleColors.BLUE+"\n****************************************\n"+ConsoleColors.RESET;

    public static void printModuleStart(String moduleName){
        System.out.print(stars+" STARTING "+moduleName+" \n");
    }

    public static void printSubModuleStart(String moduleName){
        System.out.print(ConsoleColors.BLUE+"\n| "+moduleName+ConsoleColors.RESET);
    }

    public static void printSubModuleEnd(){
        System.out.print(ConsoleColors.BLUE+"\n====================="+ConsoleColors.RESET);
    }

    public static void printModuleEnd(String moduleName){
        System.out.println("\n\n "+moduleName+" COMPLETED "+stars);
    }

    public static void printNote(String msg){
        // String arrow = "➡ ";
        // System.out.print("\n"+ConsoleColors.YELLOW+arrow+msg+ConsoleColors.RESET);
        printNote(msg, 0);
    }

    public static void printNote(String msg, int depth){
        String tab = "  ".repeat(depth);
        String arrow = " ➡ ";
        System.out.print("\n"+tab+ConsoleColors.YELLOW+arrow+msg+ConsoleColors.RESET);
    }

    public static void printNoteError(String msg){
        String arrow = "➡ ";
        System.out.print("\n"+ConsoleColors.RED+arrow+msg+ConsoleColors.RESET);
    }

    public static void printNewStep(String msg, int depth){
        String tab = "  ".repeat(depth);
        System.out.print("\n"+tab+" - "+msg);
    }

    public static void printStep(String msg, int depth){
        String tab = "  ".repeat(depth);
        System.out.print("\n"+tab+"   "+msg);
    }

    public static void printExternalStep(String msg, int depth){
        String tab = "  ".repeat(depth);
        System.out.print("\n"+ConsoleColors.PURPLE+tab+"   "+msg+ConsoleColors.RESET);
    }

    public static void printCompleteStep(String msg){
        System.out.print(" "+msg);
    }

    public static void printCompleteStep(){
        System.out.print(ConsoleColors.GREEN_BRIGHT+" ✔"+ConsoleColors.RESET);
    }
}
