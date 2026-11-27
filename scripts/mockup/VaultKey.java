import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;

/**
 * Adds the AES master key the application demands at startup to a PKCS12 keystore.
 *
 * <p>{@code qbic.security.vault.external-credential.key-alias} names an entry that
 * {@code InvenioRdmConfiguration} reads before the context finishes starting. A
 * fresh {@code keytool} keystore does not have it, and the app refuses to boot
 * without it, so the mock-up provisions it here.</p>
 *
 * <p>The value is a base64-encoded 32-byte key, because the application base64-decodes
 * whatever it finds under that alias. PKCS12 forces the entry password to equal the
 * store password, which is why one password is passed rather than two.</p>
 */
public class VaultKey {

  public static void main(String[] args) throws Exception {
    if (args.length < 3) {
      System.err.println("usage: VaultKey <keystore.p12> <password> <alias>");
      System.exit(1);
    }
    String path = args[0];
    char[] password = args[1].toCharArray();
    String alias = args[2];

    KeyStore store = KeyStore.getInstance("PKCS12");
    try (InputStream in = new FileInputStream(path)) {
      store.load(in, password);
    }

    byte[] raw = new byte[32];
    new SecureRandom().nextBytes(raw);
    String encoded = Base64.getEncoder().encodeToString(raw);

    store.setEntry(alias,
        new KeyStore.SecretKeyEntry(new SecretKeySpec(encoded.getBytes(), "AES")),
        new KeyStore.PasswordProtection(password));

    try (OutputStream out = new FileOutputStream(path)) {
      store.store(out, password);
    }
    System.out.println("vault key '" + alias + "' provisioned");
  }
}
