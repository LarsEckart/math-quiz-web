package mathquiz.tts;

import mathquiz.domain.Operation;

import java.util.Map;

/**
 * Formats math problems and feedback as Estonian text for TTS.
 */
public class EstonianSpeechFormatter {
    
    private static final Map<Integer, String> NUMBERS_ET = Map.ofEntries(
        Map.entry(0, "null"), Map.entry(1, "üks"), Map.entry(2, "kaks"),
        Map.entry(3, "kolm"), Map.entry(4, "neli"), Map.entry(5, "viis"),
        Map.entry(6, "kuus"), Map.entry(7, "seitse"), Map.entry(8, "kaheksa"),
        Map.entry(9, "üheksa"), Map.entry(10, "kümme"), Map.entry(11, "üksteist"),
        Map.entry(12, "kaksteist"), Map.entry(13, "kolmteist"), Map.entry(14, "neliteist"),
        Map.entry(15, "viisteist"), Map.entry(16, "kuusteist"), Map.entry(17, "seitseteist"),
        Map.entry(18, "kaheksateist"), Map.entry(19, "üheksateist"), Map.entry(20, "kakskümmend")
    );
    
    private static final Map<Operation, String> OPERATIONS_ET = Map.of(
        Operation.ADDITION, "pluss",
        Operation.SUBTRACTION, "miinus",
        Operation.MULTIPLICATION, "korda",
        Operation.DIVISION, "jagatud"
    );
    
    /**
     * Convert a number (0-99) to Estonian words.
     */
    public static String numberToEstonian(int n) {
        if (n < 0) {
            return "miinus " + numberToEstonian(-n);
        }
        
        String cached = NUMBERS_ET.get(n);
        if (cached != null) {
            return cached;
        }
        
        if (n < 100) {
            int tens = n / 10;
            int ones = n % 10;
            if (ones == 0) {
                return NUMBERS_ET.get(tens) + "kümmend";
            } else {
                return NUMBERS_ET.get(tens) + "kümmend " + NUMBERS_ET.get(ones);
            }
        }
        
        // For 100+, just use the number (unlikely in our app)
        return String.valueOf(n);
    }
    
    /**
     * Format a math problem as Estonian speech text.
     * Example: "Kui palju on kaks pluss kolm?"
     */
    public static String formatProblem(int operand1, Operation operation, int operand2) {
        String num1 = numberToEstonian(operand1);
        String op = OPERATIONS_ET.get(operation);
        String num2 = numberToEstonian(operand2);
        
        return String.format("Kui palju on %s %s %s?", num1, op, num2);
    }
    
    /**
     * Format correct answer feedback.
     * Example: "Tubli, kaks pluss kolm on viis."
     */
    public static String formatCorrect(int operand1, Operation operation, int operand2, int answer) {
        String num1 = numberToEstonian(operand1);
        String op = OPERATIONS_ET.get(operation);
        String num2 = numberToEstonian(operand2);
        String ans = numberToEstonian(answer);
        
        // Use a fixed praise word for caching consistency
        return String.format("Tubli, %s %s %s on %s.", num1, op, num2, ans);
    }
    
    /**
     * Format incorrect answer feedback.
     * Example: "Kahjuks vale, kaks pluss kolm on viis."
     */
    public static String formatIncorrect(int operand1, Operation operation, int operand2, int correctAnswer) {
        String num1 = numberToEstonian(operand1);
        String op = OPERATIONS_ET.get(operation);
        String num2 = numberToEstonian(operand2);
        String ans = numberToEstonian(correctAnswer);
        
        return String.format("Kahjuks vale, %s %s %s on %s.", num1, op, num2, ans);
    }
}
