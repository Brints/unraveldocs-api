package com.extractor.unraveldocs.coupon.helpers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CouponCodeGenerator Tests")
class CouponCodeGeneratorTest {

    private final CouponCodeGenerator codeGenerator = new CouponCodeGenerator();

    @Nested
    @DisplayName("Generate Code Tests")
    class GenerateCodeTests {

        @Test
        @DisplayName("Should generate code with correct length (8 chars without prefix)")
        void generateCode_correctLength() {
            // Act
            String code = codeGenerator.generate(null);

            // Assert
            assertEquals(8, code.length());
        }

        @Test
        @DisplayName("Should generate code with prefix in correct format")
        void generateCode_withPrefix() {
            // Act
            String code = codeGenerator.generate("SAVE20");

            // Assert
            assertTrue(code.startsWith("SAVE20-"));
            assertEquals(15, code.length()); // prefix (6) + hyphen (1) + random (8)
        }

        @Test
        @DisplayName("Should generate code with only valid characters")
        void generateCode_validCharacters() {
            // Act
            String code = codeGenerator.generate(null);

            // Assert
            assertTrue(code.matches("^[A-Z0-9]+$"), "Code should only contain uppercase letters and digits");
        }

        @Test
        @DisplayName("Should generate unique codes")
        void generateCode_unique() {
            // Act
            Set<String> codes = new HashSet<>();
            for (int i = 0; i < 100; i++) {
                codes.add(codeGenerator.generate(null));
            }

            // Assert - with 8-char codes from 36 chars, collisions are very unlikely
            assertEquals(100, codes.size(), "All generated codes should be unique");
        }

        @Test
        @DisplayName("Should uppercase the prefix")
        void generateCode_uppercasesPrefix() {
            // Act
            String code = codeGenerator.generate("summer");

            // Assert
            assertTrue(code.startsWith("SUMMER-"));
        }

        @Test
        @DisplayName("Should handle empty prefix")
        void generateCode_emptyPrefix() {
            // Act
            String code = codeGenerator.generate("");

            // Assert
            assertNotNull(code);
            assertEquals(8, code.length());
        }
    }

    @Nested
    @DisplayName("Generate Random String Tests")
    class GenerateRandomStringTests {

        @ParameterizedTest
        @ValueSource(ints = { 6, 8, 10, 12, 15 })
        @DisplayName("Should generate random string with specified length")
        void generateRandomString_customLength(int length) {
            // Act
            String result = codeGenerator.generateRandomString(length);

            // Assert
            assertEquals(length, result.length());
        }

        @Test
        @DisplayName("Should generate alphanumeric string only")
        void generateRandomString_alphanumericOnly() {
            // Act
            String result = codeGenerator.generateRandomString(20);

            // Assert
            assertTrue(result.matches("^[A-Z0-9]+$"));
        }
    }

    @Nested
    @DisplayName("Validate Custom Code Tests")
    class ValidateCustomCodeTests {

        @Test
        @DisplayName("Should accept valid custom code")
        void validateCustomCode_valid() {
            // Act
            boolean result = codeGenerator.isValidCustomCode("SAVE50OFF");

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("Should accept code with hyphen")
        void validateCustomCode_withHyphen() {
            // Act
            boolean result = codeGenerator.isValidCustomCode("SAVE-50-OFF");

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("Should reject code with special characters")
        void validateCustomCode_invalidChars() {
            // Act
            boolean result = codeGenerator.isValidCustomCode("SAVE@50!");

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("Should reject code that is too short")
        void validateCustomCode_tooShort() {
            // Act
            boolean result = codeGenerator.isValidCustomCode("AB");

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("Should reject null code")
        void validateCustomCode_null() {
            // Act
            boolean result = codeGenerator.isValidCustomCode(null);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("Should reject empty code")
        void validateCustomCode_empty() {
            // Act
            boolean result = codeGenerator.isValidCustomCode("");

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("Should reject blank code")
        void validateCustomCode_blank() {
            // Act
            boolean result = codeGenerator.isValidCustomCode("   ");

            // Assert
            assertFalse(result);
        }
    }
}
