package life.qbic.datamanager.security;

import com.vaadin.flow.spring.annotation.SpringComponent;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@SpringComponent
public class DataManagerVault {

  public static final String UNEXPECTED_VAULT_EXCEPTION = "Unexpected vault exception";
  private static final String KEY_GENERATOR_ALGORITHM = "AES";
  private static final double MIN_ENTROPY = 100; // Shannon entropy * length of secret
  private final KeyStore keyStore;
  private final String envVarKeystorePassword;
  private final String envVarKeystoreEntryPassword;
  private final Path keystorePath;

  public DataManagerVault(@Value("${qbic.security.vault.key.env}") String vaultKeyEnvVar,
      @Value("${qbic.security.vault.path}") String vaultPathString,
      @Value("${qbic.security.vault.entry.password.env}") String vaultEntryPassword)
      throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
    if (System.getenv(vaultKeyEnvVar) == null) {
      throw new DataManagerVaultException(
          "Cannot find value for environment variable: %s".formatted(vaultKeyEnvVar));
    }
    if (System.getenv(vaultEntryPassword) == null) {
      throw new DataManagerVaultException(
          "Cannot find value for environment variable: %s".formatted(vaultEntryPassword)
      );
    }
    this.envVarKeystoreEntryPassword = vaultEntryPassword;
    this.envVarKeystorePassword = vaultKeyEnvVar;

    double entropy;
    if ((entropy = calculateEntropy(System.getenv(envVarKeystorePassword))) < MIN_ENTROPY) {
      throw new DataManagerVaultException(
          "Entry of password for keystore was to low: %f (min: %f)".formatted(entropy,
              MIN_ENTROPY));
    }
    if ((entropy = calculateEntropy(System.getenv(envVarKeystoreEntryPassword))) < MIN_ENTROPY) {
      throw new DataManagerVaultException(
          "Entry of password for keystore entries was to low: %f (min: %f)".formatted(entropy,
              MIN_ENTROPY));
    }

    this.keystorePath = fromString(vaultPathString);
    this.keyStore = createVault(vaultKeyEnvVar, keystorePath);
  }

  private static double calculateEntropy(String secret) {
    if (secret == null || secret.isEmpty()) {
      return 0.0;
    }

    Map<Character, Integer> frequencyMap = new HashMap<>();
    int length = secret.length();

    // Count character frequencies
    for (char c : secret.toCharArray()) {
      frequencyMap.put(c, frequencyMap.getOrDefault(c, 0) + 1);
    }

    // Compute entropy
    double entropy = 0.0;
    for (Integer count : frequencyMap.values()) {
      double probability = (double) count / length;
      entropy += probability * (Math.log(probability) / Math.log(2));
    }

    return -entropy * secret.length(); // Negate since log probabilities are negative
  }

  private static Path fromString(String path) {
    Path p = Paths.get(path);
    if (p.isAbsolute()) {
      return p;
    }
    return Path.of(System.getProperty("user.dir")).resolve(path);
  }

  private static KeyStore createVault(String vaultKeyEnvVar, Path vaultPath)
      throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {

    return KeyStore.getInstance(vaultPath.toFile(),
        System.getenv(vaultKeyEnvVar).toCharArray());
  }

  /**
   * Write adds a secret under a given alias to the vault and stores the vault content into the
   * configured file.
   * <p>
   * {@link DataManagerVault} applies an AES encryption on the provided secret.
   *
   * @param alias  the reference the entry can be retrieved again with
   *               {@link DataManagerVault#read(String)}.
   * @param secret the secret to store in the vault
   * @since 1.8.0
   */
  public void write(String alias, String secret) {
    try {
      this.keyStore.setEntry(alias, new SecretKeyEntry(new SecretKeySpec(secret.getBytes(
              StandardCharsets.UTF_8), KEY_GENERATOR_ALGORITHM)),
          new PasswordProtection(System.getenv(envVarKeystoreEntryPassword).toCharArray()));
    } catch (KeyStoreException e) {
      throw new DataManagerVaultException(UNEXPECTED_VAULT_EXCEPTION, e);
    }

    try (var bos = new BufferedOutputStream(new FileOutputStream(keystorePath.toFile()))) {
      keyStore.store(bos, System.getenv(envVarKeystorePassword).toCharArray());
      bos.flush();
    } catch (IOException e) {
      throw new DataManagerVaultException("Unexpected vault exception when writing to the keystore",
          e);
    } catch (CertificateException | KeyStoreException | NoSuchAlgorithmException e) {
      throw new DataManagerVaultException(UNEXPECTED_VAULT_EXCEPTION, e);
    }
  }

  /**
   * Read looks for a matching entry for the provided alias.
   * <p>
   * If no entry for the given alias is found, the vault returns an {@link Optional#empty()}.
   * <p>
   * If the decryption of the secret fails, an {@link DataManagerVaultException} is thrown.
   *
   * @param alias the reference for the entry to retrieve
   * @return an {@link Optional<String>} with the potential secret.
   * @throws DataManagerVaultException if the decryption fails.
   * @since 1.8.0
   */
  public Optional<String> read(String alias) throws DataManagerVaultException {
    try {
      return Optional.ofNullable(
              this.keyStore.getKey(alias, System.getenv(envVarKeystoreEntryPassword).toCharArray()))
          .map(k -> new String(k.getEncoded(), StandardCharsets.UTF_8));
    } catch (KeyStoreException | NoSuchAlgorithmException e) {
      throw new DataManagerVaultException(UNEXPECTED_VAULT_EXCEPTION, e);
    } catch (UnrecoverableKeyException e) {
      throw new DataManagerVaultException("Recovering alias entry failed", e);
    }
  }

  /**
   * Used for exceptions occurring during interactions with the {@link DataManagerVault}.
   * @since 1.8.0
   */
  public static class DataManagerVaultException extends RuntimeException {

    public DataManagerVaultException(String message) {
      super(message);
    }

    public DataManagerVaultException(String message, Throwable cause) {
      super(message, cause);
    }
  }

}
