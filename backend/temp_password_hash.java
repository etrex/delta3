import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class temp_password_hash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("password123: " + encoder.encode("password123"));
    }
}
