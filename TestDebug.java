import java.util.Base64;

public class TestDebug {
    public static void main(String[] args) {
        System.out.println("USPON USER: " + unmask("bW9jLnNsaWNvdi5hbiU0MGFuem96b25oZXQ="));
        System.out.println("AVTERA PASS: " + unmask("NzA3bm96b25oZXQ="));
    }

    public static String unmask(String masked) {
        return new String(Base64.getDecoder().decode(new StringBuilder(masked).reverse().toString()));
    }
}
