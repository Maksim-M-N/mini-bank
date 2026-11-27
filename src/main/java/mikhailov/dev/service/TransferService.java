package mikhailov.dev.service;

import mikhailov.dev.domain.Account;
import mikhailov.dev.domain.Transfer;
import mikhailov.dev.domain.enums.AccountStatus;
import mikhailov.dev.exception.AccountNotFoundException;
import mikhailov.dev.repository.AccountRepository;
import mikhailov.dev.transaction.TransferTransaction;

import java.math.BigDecimal;

public class TransferService {
    private final TransferTransaction transaction;

    public TransferService(TransferTransaction transaction) {
        this.transaction = transaction;
    }

    public void transfer(String fromAccountNumber, String toAccountNumber,
                         BigDecimal amount, String comment) {
        if (fromAccountNumber == null || toAccountNumber == null) {
            throw new IllegalArgumentException(
                    "Номера счетов отправителя и получателя не должны быть null"
            );
        }

        transaction.inTransaction((accountRepository, transferRepository) -> {
            boolean fromFirst = fromAccountNumber.compareTo(toAccountNumber) <= 0;

            String firstNumber = fromFirst ? fromAccountNumber : toAccountNumber;
            String secondNumber = fromFirst ? toAccountNumber : fromAccountNumber;

            Account firstAccount = findAccount(accountRepository, firstNumber);
            Account secondAccount = findAccount(accountRepository, secondNumber);

            Account fromAccount = fromFirst ? firstAccount : secondAccount;
            Account toAccount = fromFirst ? secondAccount : firstAccount;

            validateTransfer(fromAccount, toAccount, amount);

            BigDecimal newFromBalance = fromAccount.getBalance().subtract(amount);
            BigDecimal newToBalance = toAccount.getBalance().add(amount);

            accountRepository.updateBalance(fromAccount.getId(), newFromBalance);
            accountRepository.updateBalance(toAccount.getId(), newToBalance);

            Transfer transfer = new Transfer(fromAccount, toAccount, amount, comment);

            transfer.markSuccessful();

            transferRepository.save(transfer);
        });
    }

    private Account findAccount(AccountRepository accounts, String accountNumber) {
        return accounts.findByAccountNumberForUpdate(accountNumber).orElseThrow(
                () -> new AccountNotFoundException("Счёт с номером " + accountNumber + " не найден"));
    }

    private void validateTransfer(Account fromAccount, Account toAccount, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Сумма перевода должна быть больше нуля");
        }

        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(
                    "Недопустимая точность суммы перевода"
            );
        }

        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new IllegalArgumentException(
                    "Нельзя выполнить перевод на тот же счёт"
            );
        }

        if (fromAccount.getStatus() != AccountStatus.ACTIVE ||
                toAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Оба счёта должны быть активными"
            );
        }

        if (amount.compareTo(fromAccount.getBalance()) > 0) {
            throw new IllegalStateException(
                    "Недостаточно средств на счёте отправителя"
            );
        }

        if (fromAccount.getCurrencyCode() != toAccount.getCurrencyCode()) {
            throw new IllegalArgumentException(
                    "Переводы между счетами с разными валютами не поддерживаются"
            );
        }
    }
}
