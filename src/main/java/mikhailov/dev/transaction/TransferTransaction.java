package mikhailov.dev.transaction;

import mikhailov.dev.repository.AccountRepository;
import mikhailov.dev.repository.TransferRepository;

import java.util.function.BiConsumer;

@FunctionalInterface
public interface TransferTransaction {
    void inTransaction(BiConsumer<AccountRepository, TransferRepository> action);
}
