import java.util.Base64;
public class TestUnmask {
    public static void main(String[] args) {
        String[] masked = {"bW9jLmxpYW1nQGFrc2Fyb25venNvbmgldA==", "NDIwMmlwYWFub3o="};
        for (String m : masked) {
            String reversed = new StringBuilder(m).reverse().toString();
            try {
                System.out.println(new String(Base64.getDecoder().decode(reversed)));
            } catch (Exception e) {
                System.out.println("Error for " + m + ": " + e.getMessage());
            }
        }
    }
}
