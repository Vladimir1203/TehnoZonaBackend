import java.util.Base64;
public class TestOriginal {
    public static void main(String[] args) {
        String masked = "bW9jLmxpYW1nQGFrc2Fyb25venNvbmgldA==";
        String reversed = new StringBuilder(masked).reverse().toString();
        try {
            System.out.println("Original unmasked: " + new String(Base64.getDecoder().decode(reversed)));
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
