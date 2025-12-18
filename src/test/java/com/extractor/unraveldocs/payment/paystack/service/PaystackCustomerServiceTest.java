package com.extractor.unraveldocs.payment.paystack.service;

import com.extractor.unraveldocs.payment.paystack.model.PaystackCustomer;
import com.extractor.unraveldocs.payment.paystack.repository.PaystackCustomerRepository;
import com.extractor.unraveldocs.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaystackCustomerService.
 * Tests focus on repository operations and customer management.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaystackCustomerService Tests")
class PaystackCustomerServiceTest {

    @Mock
    private PaystackCustomerRepository customerRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user-123");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
    }

    @Nested
    @DisplayName("Find Customer Tests")
    class FindCustomerTests {

        @Test
        @DisplayName("Should find existing customer by user ID")
        void shouldFindExistingCustomerByUserId() {
            // Given
            PaystackCustomer existingCustomer = PaystackCustomer.builder()
                    .id("customer-id-123")
                    .user(testUser)
                    .customerCode("CUS_existing123")
                    .email("test@example.com")
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            when(customerRepository.findByUserId(testUser.getId()))
                    .thenReturn(Optional.of(existingCustomer));

            // When
            Optional<PaystackCustomer> result = customerRepository.findByUserId(testUser.getId());

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getCustomerCode()).isEqualTo("CUS_existing123");
            assertThat(result.get().getEmail()).isEqualTo("test@example.com");
            verify(customerRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should return empty when customer not found by user ID")
        void shouldReturnEmptyWhenCustomerNotFound() {
            // Given
            when(customerRepository.findByUserId("unknown-user")).thenReturn(Optional.empty());

            // When
            Optional<PaystackCustomer> result = customerRepository.findByUserId("unknown-user");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should find customer by customer code")
        void shouldFindCustomerByCode() {
            // Given
            String customerCode = "CUS_test123";
            PaystackCustomer customer = PaystackCustomer.builder()
                    .customerCode(customerCode)
                    .email("test@example.com")
                    .build();

            when(customerRepository.findByCustomerCode(customerCode)).thenReturn(Optional.of(customer));

            // When
            Optional<PaystackCustomer> result = customerRepository.findByCustomerCode(customerCode);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getEmail()).isEqualTo("test@example.com");
        }
    }

    @Nested
    @DisplayName("Customer Model Tests")
    class CustomerModelTests {

        @Test
        @DisplayName("Should create customer with all fields")
        void shouldCreateCustomerWithAllFields() {
            // Given & When
            PaystackCustomer customer = PaystackCustomer.builder()
                    .user(testUser)
                    .customerCode("CUS_test123")
                    .paystackCustomerId(12345L)
                    .email("test@example.com")
                    .firstName("John")
                    .lastName("Doe")
                    .phone("+2348012345678")
                    .riskAction("default")
                    .build();

            // Then
            assertThat(customer.getCustomerCode()).isEqualTo("CUS_test123");
            assertThat(customer.getPaystackCustomerId()).isEqualTo(12345L);
            assertThat(customer.getEmail()).isEqualTo("test@example.com");
            assertThat(customer.getFirstName()).isEqualTo("John");
            assertThat(customer.getLastName()).isEqualTo("Doe");
            assertThat(customer.getPhone()).isEqualTo("+2348012345678");
            assertThat(customer.getUser()).isEqualTo(testUser);
        }

        @Test
        @DisplayName("Should update customer details")
        void shouldUpdateCustomerDetails() {
            // Given
            PaystackCustomer customer = PaystackCustomer.builder()
                    .customerCode("CUS_test123")
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            // When
            customer.setFirstName("Jane");
            customer.setLastName("Smith");
            customer.setPhone("+2348098765432");
            customer.setRiskAction("allow");

            when(customerRepository.save(any(PaystackCustomer.class))).thenReturn(customer);
            PaystackCustomer savedCustomer = customerRepository.save(customer);

            // Then
            assertThat(savedCustomer.getFirstName()).isEqualTo("Jane");
            assertThat(savedCustomer.getLastName()).isEqualTo("Smith");
            assertThat(savedCustomer.getPhone()).isEqualTo("+2348098765432");
            assertThat(savedCustomer.getRiskAction()).isEqualTo("allow");
        }
    }

    @Nested
    @DisplayName("Customer Exists Tests")
    class CustomerExistsTests {

        @Test
        @DisplayName("Should return true when customer exists for user")
        void shouldReturnTrueWhenCustomerExists() {
            // Given
            String userId = "user-123";
            when(customerRepository.existsByUserId(userId)).thenReturn(true);

            // When
            boolean exists = customerRepository.existsByUserId(userId);

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Should return false when customer does not exist for user")
        void shouldReturnFalseWhenCustomerDoesNotExist() {
            // Given
            String userId = "unknown-user";
            when(customerRepository.existsByUserId(userId)).thenReturn(false);

            // When
            boolean exists = customerRepository.existsByUserId(userId);

            // Then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Save Customer Tests")
    class SaveCustomerTests {

        @Test
        @DisplayName("Should save new customer")
        void shouldSaveNewCustomer() {
            // Given
            PaystackCustomer newCustomer = PaystackCustomer.builder()
                    .user(testUser)
                    .customerCode("CUS_new123")
                    .paystackCustomerId(54321L)
                    .email("test@example.com")
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            when(customerRepository.save(any(PaystackCustomer.class))).thenReturn(newCustomer);

            // When
            PaystackCustomer savedCustomer = customerRepository.save(newCustomer);

            // Then
            assertThat(savedCustomer.getCustomerCode()).isEqualTo("CUS_new123");
            assertThat(savedCustomer.getPaystackCustomerId()).isEqualTo(54321L);
            verify(customerRepository).save(newCustomer);
        }
    }
}
