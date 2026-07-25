package br.ufmt.periscope.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordHasherTest {

    @Test
    void hashAndVerifyRoundTrip() {
        String hash = PasswordHasher.hash("123456");
        assertThat(hash).startsWith(PasswordHasher.PREFIX);
        assertThat(PasswordHasher.verify("123456", hash)).isTrue();
        assertThat(PasswordHasher.verify("wrong", hash)).isFalse();
        assertThat(PasswordHasher.needsRehash(hash)).isFalse();
        assertThat(PasswordHasher.isHashed(hash)).isTrue();
    }

    @Test
    void differentSaltsProduceDifferentHashes() {
        String a = PasswordHasher.hash("same");
        String b = PasswordHasher.hash("same");
        assertThat(a).isNotEqualTo(b);
        assertThat(PasswordHasher.verify("same", a)).isTrue();
        assertThat(PasswordHasher.verify("same", b)).isTrue();
    }

    @Test
    void verifiesLegacyPlainTextAndFlagsRehash() {
        assertThat(PasswordHasher.verify("123456", "123456")).isTrue();
        assertThat(PasswordHasher.verify("123456", "other")).isFalse();
        assertThat(PasswordHasher.needsRehash("123456")).isTrue();
        assertThat(PasswordHasher.isHashed("123456")).isFalse();
    }

    @Test
    void rejectsLowIterationCount() {
        assertThatThrownBy(() -> PasswordHasher.hash("x", 1000))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
