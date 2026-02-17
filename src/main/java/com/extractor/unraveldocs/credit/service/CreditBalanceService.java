package com.extractor.unraveldocs.credit.service;

import com.extractor.unraveldocs.auth.datamodel.Role;
import com.extractor.unraveldocs.credit.datamodel.CreditTransactionType;
import com.extractor.unraveldocs.credit.dto.response.CreditBalanceData;
import com.extractor.unraveldocs.credit.dto.response.CreditTransferData;
import com.extractor.unraveldocs.credit.model.CreditTransaction;
import com.extractor.unraveldocs.credit.model.UserCreditBalance;
import com.extractor.unraveldocs.credit.repository.CreditTransactionRepository;
import com.extractor.unraveldocs.credit.repository.UserCreditBalanceRepository;
import com.extractor.unraveldocs.exceptions.custom.BadRequestException;
import com.extractor.unraveldocs.exceptions.custom.NotFoundException;
import com.extractor.unraveldocs.messaging.emailtemplates.UserEmailTemplateService;
import com.extractor.unraveldocs.pushnotification.datamodel.NotificationType;
import com.extractor.unraveldocs.pushnotification.interfaces.NotificationService;
import com.extractor.unraveldocs.user.model.User;
import com.extractor.unraveldocs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing user credit balances.
 * Handles adding, deducting, transferring, and querying credits
 * with full transaction logging.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditBalanceService {

    private static final int SIGNUP_BONUS_CREDITS = 5;
    private static final int MIN_BALANCE_AFTER_TRANSFER = 5;
    private static final int MONTHLY_TRANSFER_CAP = 30;

    private final UserCreditBalanceRepository creditBalanceRepository;
    private final CreditTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final UserEmailTemplateService emailTemplateService;

    /**
     * Get or create a credit balance for the user.
     */
    public UserCreditBalance getOrCreateBalance(String userId) {
        return creditBalanceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new BadRequestException("User not found: " + userId));
                    UserCreditBalance balance = UserCreditBalance.builder()
                            .user(user)
                            .balance(0)
                            .totalPurchased(0)
                            .totalUsed(0)
                            .build();
                    return creditBalanceRepository.save(balance);
                });
    }

    /**
     * Get the credit balance data for a user.
     */
    public CreditBalanceData getBalanceData(String userId) {
        UserCreditBalance balance = getOrCreateBalance(userId);
        return CreditBalanceData.builder()
                .balance(balance.getBalance())
                .totalPurchased(balance.getTotalPurchased())
                .totalUsed(balance.getTotalUsed())
                .build();
    }

    /**
     * Add credits to a user's balance and log the transaction.
     */
    @Transactional
    public void addCredits(User user, int credits, CreditTransactionType type, String referenceId, String description) {
        UserCreditBalance balance = getOrCreateBalance(user.getId());
        balance.setBalance(balance.getBalance() + credits);
        balance.setTotalPurchased(balance.getTotalPurchased() + credits);
        creditBalanceRepository.save(balance);

        CreditTransaction transaction = CreditTransaction.builder()
                .user(user)
                .type(type)
                .amount(credits)
                .balanceAfter(balance.getBalance())
                .description(description)
                .referenceId(referenceId)
                .build();
        transactionRepository.save(transaction);
    }

    /**
     * Deduct credits from a user's balance after successful OCR processing.
     *
     * @throws BadRequestException if the user does not have enough credits
     */
    @Transactional
    public void deductCredits(User user, int credits, String referenceId, String description) {
        UserCreditBalance balance = getOrCreateBalance(user.getId());

        if (balance.getBalance() < credits) {
            throw new BadRequestException(
                    String.format("Insufficient credits. Required: %d, Available: %d", credits, balance.getBalance()));
        }

        balance.setBalance(balance.getBalance() - credits);
        balance.setTotalUsed(balance.getTotalUsed() + credits);
        creditBalanceRepository.save(balance);

        CreditTransaction transaction = CreditTransaction.builder()
                .user(user)
                .type(CreditTransactionType.DEDUCTION)
                .amount(credits)
                .balanceAfter(balance.getBalance())
                .description(description)
                .referenceId(referenceId)
                .build();
        transactionRepository.save(transaction);
    }

    /**
     * Check if a user has enough credits.
     */
    public boolean hasEnoughCredits(String userId, int required) {
        UserCreditBalance balance = getOrCreateBalance(userId);
        return balance.getBalance() >= required;
    }

    /**
     * Grant sign-up bonus credits to a new user.
     */
    @Transactional
    public void grantSignupBonus(User user) {
        addCredits(
                user,
                SIGNUP_BONUS_CREDITS,
                CreditTransactionType.BONUS,
                null,
                "Welcome bonus: " + SIGNUP_BONUS_CREDITS + " free credits on sign-up");
    }

    /**
     * Transfer credits from one user to another.
     * <p>
     * Rules:
     * - Sender must retain at least 5 credits after transfer
     * - Regular users can transfer max 30 credits per calendar month
     * - Admin/Super Admin bypass the monthly cap
     * - Cannot transfer to self
     */
    @Transactional
    public CreditTransferData transferCredits(User sender, String recipientEmail, int amount) {
        // Cannot transfer to self
        if (sender.getEmail().equalsIgnoreCase(recipientEmail)) {
            throw new BadRequestException("Cannot transfer credits to yourself");
        }

        // Validate recipient exists
        User recipient = userRepository.findByEmail(recipientEmail)
                .orElseThrow(() -> new NotFoundException("Recipient not found with email: " + recipientEmail));

        UserCreditBalance senderBalance = getOrCreateBalance(sender.getId());

        // Sender must keep at least MIN_BALANCE_AFTER_TRANSFER credits
        int balanceAfterTransfer = senderBalance.getBalance() - amount;
        if (balanceAfterTransfer < MIN_BALANCE_AFTER_TRANSFER) {
            throw new BadRequestException(
                    String.format(
                            "You must retain at least %d credits after a transfer. Current balance: %d, Transfer amount: %d",
                            MIN_BALANCE_AFTER_TRANSFER, senderBalance.getBalance(), amount));
        }

        // Monthly cap check for regular users (not ADMIN/SUPER_ADMIN)
        if (!isAdminOrSuperAdmin(sender)) {
            OffsetDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay().atOffset(ZoneOffset.UTC);
            int transferredThisMonth = transactionRepository.sumTransfersSentInPeriod(sender.getId(), startOfMonth);
            int remaining = MONTHLY_TRANSFER_CAP - transferredThisMonth;

            if (amount > remaining) {
                throw new BadRequestException(
                        String.format(
                                "Monthly transfer limit exceeded. You can transfer %d more credits this month (cap: %d/month)",
                                Math.max(0, remaining), MONTHLY_TRANSFER_CAP));
            }
        }

        // Deduct from sender
        int remainingBalanceAfterTransfer = senderBalance.getBalance() - amount;
        senderBalance.setBalance(remainingBalanceAfterTransfer);

        int totalUsedAfterTransfer = senderBalance.getTotalUsed() + amount;
        senderBalance.setTotalUsed(totalUsedAfterTransfer);
        creditBalanceRepository.save(senderBalance);

        // Add to recipient
        UserCreditBalance recipientBalance = getOrCreateBalance(recipient.getId());
        int recipientNewBalance = recipientBalance.getBalance() + amount;
        recipientBalance.setBalance(recipientNewBalance);

        int recipientTotalPurchasedAfterTransfer = recipientBalance.getTotalPurchased() + amount;
        recipientBalance.setTotalPurchased(recipientTotalPurchasedAfterTransfer);
        creditBalanceRepository.save(recipientBalance);

        // Log sender transaction
        CreditTransaction senderTx = CreditTransaction.builder()
                .user(sender)
                .type(CreditTransactionType.TRANSFER_SENT)
                .amount(amount)
                .balanceAfter(senderBalance.getBalance())
                .description("Transferred " + amount + " credits to " + recipient.getEmail())
                .referenceId(recipient.getId())
                .build();
        transactionRepository.save(senderTx);

        // Log recipient transaction
        CreditTransaction recipientTx = CreditTransaction.builder()
                .user(recipient)
                .type(CreditTransactionType.TRANSFER_RECEIVED)
                .amount(amount)
                .balanceAfter(recipientBalance.getBalance())
                .description("Received " + amount + " credits from " + sender.getEmail())
                .referenceId(sender.getId())
                .sender(sender)
                .build();
        transactionRepository.save(recipientTx);

        // Send notifications to both parties
        sendTransferNotifications(sender, recipient, amount, senderBalance.getBalance(), recipientBalance.getBalance());

        return CreditTransferData.builder()
                .transferId(senderTx.getId())
                .creditsTransferred(amount)
                .senderBalanceAfter(senderBalance.getBalance())
                .recipientEmail(recipient.getEmail())
                .recipientName(recipient.getFirstName() + " " + recipient.getLastName())
                .build();
    }

    /**
     * Admin/Super Admin allocate credits to any user without restrictions.
     */
    @Transactional
    public void adminAllocateCredits(User admin, String targetUserId, int amount, String reason) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NotFoundException("User not found: " + targetUserId));

        UserCreditBalance balance = getOrCreateBalance(targetUserId);
        int newBalance = balance.getBalance() + amount;
        balance.setBalance(newBalance);

        int newTotalPurchased = balance.getTotalPurchased() + amount;
        balance.setTotalPurchased(newTotalPurchased);
        creditBalanceRepository.save(balance);

        String description = "Admin allocation of " + amount + " credits by " + admin.getEmail();
        if (reason != null && !reason.isBlank()) {
            description += ". Reason: " + reason;
        }

        CreditTransaction transaction = CreditTransaction.builder()
                .user(targetUser)
                .type(CreditTransactionType.ADMIN_ALLOCATION)
                .amount(amount)
                .balanceAfter(balance.getBalance())
                .description(description)
                .referenceId(admin.getId())
                .sender(admin)
                .build();
        transactionRepository.save(transaction);
    }

    /**
     * Grant unlimited credits (Integer.MAX_VALUE) to admin/super admin users.
     */
    @Transactional
    public void grantUnlimitedCredits(User adminUser) {
        UserCreditBalance balance = getOrCreateBalance(adminUser.getId());

        // Only grant if not already set to unlimited
        if (balance.getBalance() >= Integer.MAX_VALUE / 2) {
            return;
        }

        balance.setBalance(Integer.MAX_VALUE);
        balance.setTotalPurchased(Integer.MAX_VALUE);
        creditBalanceRepository.save(balance);

        CreditTransaction transaction = CreditTransaction.builder()
                .user(adminUser)
                .type(CreditTransactionType.ADMIN_ALLOCATION)
                .amount(Integer.MAX_VALUE)
                .balanceAfter(Integer.MAX_VALUE)
                .description("Unlimited credits assigned to admin/super admin")
                .build();
        transactionRepository.save(transaction);

        log.info("Unlimited credits granted to admin user: {}", adminUser.getEmail());
    }

    // ─── Private helpers ────────────────────────────────────────

    private boolean isAdminOrSuperAdmin(User user) {
        return user.getRole() == Role.ADMIN || user.getRole() == Role.SUPER_ADMIN;
    }

    private void sendTransferNotifications(User sender, User recipient, int amount,
            int senderNewBalance, int recipientNewBalance) {
        try {
            // Push notification to sender
            Map<String, String> senderData = new HashMap<>();
            senderData.put("amount", String.valueOf(amount));
            senderData.put("recipientEmail", recipient.getEmail());
            senderData.put("newBalance", String.valueOf(senderNewBalance));

            notificationService.sendToUser(
                    sender.getId(),
                    NotificationType.CREDIT_TRANSFER_SENT,
                    "Credits Sent Successfully",
                    String.format("You sent %d credits to %s. Your new balance: %d",
                            amount, recipient.getFirstName(), senderNewBalance),
                    senderData);

            // Push notification to recipient
            Map<String, String> recipientData = new HashMap<>();
            recipientData.put("amount", String.valueOf(amount));
            recipientData.put("senderEmail", sender.getEmail());
            recipientData.put("newBalance", String.valueOf(recipientNewBalance));

            notificationService.sendToUser(
                    recipient.getId(),
                    NotificationType.CREDIT_TRANSFER_RECEIVED,
                    "Credits Received!",
                    String.format("You received %d credits from %s. Your new balance: %d",
                            amount, sender.getFirstName(), recipientNewBalance),
                    recipientData);

            // Email to sender
            emailTemplateService.sendCreditTransferSentEmail(
                    sender.getEmail(), sender.getFirstName(), sender.getLastName(),
                    amount, recipient.getFirstName() + " " + recipient.getLastName(),
                    senderNewBalance);

            // Email to recipient
            emailTemplateService.sendCreditTransferReceivedEmail(
                    recipient.getEmail(), recipient.getFirstName(), recipient.getLastName(),
                    amount, sender.getFirstName() + " " + sender.getLastName(),
                    recipientNewBalance);

        } catch (Exception e) {
            log.error("Failed to send transfer notifications: {}", e.getMessage());
        }
    }
}
