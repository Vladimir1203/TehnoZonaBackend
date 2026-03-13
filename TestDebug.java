import java.util.Base64;
public class TestDebug {
    public static void main(String[] args) {
        String masked = "bW9jLmxpYW1nQGFrc2Fyb25venNvbmgldA==";
        String reversed = new StringBuilder(masked).reverse().toString();
        System.out.println("Reversed: [" + reversed + "]");
        try {
            byte[] decoded = Base64.getDecoder().decode(reversed);
            System.out.println("Decoded: [" + new String(decoded) + "]");
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
