package mikhailov.dev.service;

import mikhailov.dev.domain.Account;
import mikhailov.dev.domain.Transfer;
import mikhailov.dev.dto.ClientBalanceSummary;
import mikhailov.dev.exception.AccountNotFoundException;
import mikhailov.dev.repository.AccountRepository;
import mikhailov.dev.repository.TransferRepository;

import java.math.BigDecimal;
import java.util.List;

public class ReportService {
    private final AccountRepository accountRepository;
    private final TransferRepository transferRepository;

    public ReportService(AccountRepository accountRepository, TransferRepository transferRepository) {
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
    }

    public void printClientAccountsReport(Long clientId) {
        List<Account> accounts = accountRepository.findAllByClientId(clientId);

        if (accounts.isEmpty()) {
            System.out.println("У клиента с ID: " + clientId + " нет счетов или клиент не найден");
            return;
        }

        String clientName = accounts.getFirst().getClient().getFullName();

        System.out.println("Клиент: " + clientName);
        System.out.println("Список счетов:");

        for (Account account : accounts) {
            System.out.println("Номер счёта: " + account.getAccountNumber());
            System.out.println("Валюта: " + account.getCurrencyCode());
            System.out.println("Баланс: " + account.getBalance());
            System.out.println("Статус: " + account.getStatus());
            System.out.println("-------------------------");
        }
    }

    public void printAccountTransferHistory(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(
                () -> new AccountNotFoundException("Счёт с ID: " + accountNumber + " не найден"));

        List<Transfer> transfers = transferRepository.findByAccountId(account.getId());

        if (transfers.isEmpty()) {
            System.out.println("История переводов пуста");
            return;
        }

        System.out.println("История переводов по счёту: " + accountNumber);

        for (Transfer transfer : transfers) {
            String direction = transfer.getFromAccount().getId().equals(account.getId()) ? "OUTCOME" : "INCOME";

            System.out.println("Дата: " + transfer.getCreatedAt());
            System.out.println("Направление: " + direction);
            System.out.println("Отправитель: " + transfer.getFromAccount().getAccountNumber());
            System.out.println("Получатель: " + transfer.getToAccount().getAccountNumber());
            System.out.println("Сумма: " + transfer.getAmount());
            System.out.println("Статус: " + transfer.getStatus());
            System.out.println("Комментарий: " + transfer.getComment());
            System.out.println("-------------------------");
        }
    }

    public void printClientBalanceSummary() {
        List<ClientBalanceSummary> summaries =
                accountRepository.getClientBalanceSummary();

        if (summaries.isEmpty()) {
            System.out.println("Данные о клиентах отсутствуют");
            return;
        }

        for (ClientBalanceSummary summary : summaries) {
            System.out.println("Клиент: " + summary.fullName());
            System.out.println("Количество счетов: " + summary.accountCount());
            System.out.println("Общий баланс: " + summary.totalBalance());

            System.out.println("Средний баланс: " +
                    (summary.averageBalance() != null
                            ? summary.averageBalance()
                            : "—"));

            System.out.println("-------------------------");
        }
    }

    public void printRichClients(BigDecimal minTotalBalance) {
        List<ClientBalanceSummary> clients =
                accountRepository.getRichClients(minTotalBalance);

        if (clients.isEmpty()) {
            System.out.println("Клиентов с балансом выше " + minTotalBalance + " не найдено");
            return;
        }

        for (ClientBalanceSummary client : clients) {
            System.out.println("Клиент: " + client.fullName());
            System.out.println("Общий баланс: " + client.totalBalance());
            System.out.println("-------------------------");
        }
    }

    public void printAccountsWithTransfers() {
        List<Account> accounts = accountRepository.findAccountsWithTransfers();

        if (accounts.isEmpty()) {
            System.out.println("Счета с переводами не найдены");
            return;
        }

        for (Account account : accounts) {
            System.out.println("Клиент: " + account.getClient().getFullName());
            System.out.println("Номер счёта: " + account.getAccountNumber());
            System.out.println("Валюта: " + account.getCurrencyCode());
            System.out.println("Баланс: " + account.getBalance());
            System.out.println("Статус: " + account.getStatus());
            System.out.println("-------------------------");
        }
    }
}
