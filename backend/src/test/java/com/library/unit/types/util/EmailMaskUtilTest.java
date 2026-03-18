package com.library.unit.types.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.library.types.util.EmailMaskUtil;
import org.junit.jupiter.api.Test;

class EmailMaskUtilTest {

    @Test
    void mask_standardEmail_masksLocalPart() {
        String result = EmailMaskUtil.mask("alice@example.com");
        assertThat(result).isEqualTo("a***@example.com");
    }

    @Test
    void mask_singleCharLocal_masksCorrectly() {
        String result = EmailMaskUtil.mask("b@example.com");
        assertThat(result).isEqualTo("b***@example.com");
    }

    @Test
    void mask_longerLocalPart_masksAllButFirst() {
        String result = EmailMaskUtil.mask("testuser@domain.org");
        assertThat(result).isEqualTo("t***@domain.org");
    }

    @Test
    void mask_nullOrEmpty_returnsPlaceholder() {
        assertThat(EmailMaskUtil.mask(null)).isEqualTo("[invalid-email]");
        assertThat(EmailMaskUtil.mask("")).isEqualTo("[invalid-email]");
        assertThat(EmailMaskUtil.mask("   ")).isEqualTo("[invalid-email]");
    }

    @Test
    void mask_missingAtSign_returnsPlaceholder() {
        assertThat(EmailMaskUtil.mask("notanemail")).isEqualTo("[invalid-email]");
    }

    @Test
    void mask_atSignAtStart_returnsPlaceholder() {
        assertThat(EmailMaskUtil.mask("@domain.com")).isEqualTo("[invalid-email]");
    }
}
