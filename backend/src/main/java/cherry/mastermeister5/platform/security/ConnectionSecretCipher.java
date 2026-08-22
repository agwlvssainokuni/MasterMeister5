/*
 * Copyright 2026 agwlvssainokuni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cherry.mastermeister5.platform.security;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * component-methods.md: SecurityInfrastructureComponent#encryptConnectionSecret /
 * #decryptConnectionSecret. nfr-design-patterns.md: AES-256-GCM; a fresh 96-bit
 * IV is generated per encryption and prepended to the ciphertext+tag, then the
 * whole thing is Base64-encoded as a single opaque string (no separate IV
 * column).
 */
@Component
public class ConnectionSecretCipher {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ConnectionSecretProperties properties;
    private SecretKeySpec key;

    public ConnectionSecretCipher(ConnectionSecretProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void initKey() {
        var keyBytes = properties.connectionSecretKey().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "mastermeister5.security.connection-secret-key must be at least 32 bytes (256 bits) for AES-256-GCM");
        }
        this.key = new SecretKeySpec(Arrays.copyOf(keyBytes, 32), "AES");
    }

    public String encrypt(String plaintext) {
        try {
            var iv = new byte[IV_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(iv);
            var cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            var encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            var combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt connection secret", e);
        }
    }

    public String decrypt(String encoded) {
        try {
            var combined = Base64.getDecoder().decode(encoded);
            var iv = Arrays.copyOfRange(combined, 0, IV_LENGTH_BYTES);
            var encrypted = Arrays.copyOfRange(combined, IV_LENGTH_BYTES, combined.length);
            var cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt connection secret", e);
        }
    }
}
