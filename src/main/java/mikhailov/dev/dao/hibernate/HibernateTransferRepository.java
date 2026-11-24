package mikhailov.dev.dao.hibernate;

import mikhailov.dev.domain.Transfer;
import mikhailov.dev.repository.TransferRepository;

import java.math.BigDecimal;
import java.util.List;

public class HibernateTransferRepository implements TransferRepository {
    @Override
    public Transfer save(Transfer transfer) {
        return null;
    }

    @Override
    public List<Transfer> findByAccountId(Long accountId) {
        return List.of();
    }

    @Override
    public List<Transfer> findOutgoingTransfers(Long accountId) {
        return List.of();
    }

    @Override
    public List<Transfer> findIncomingTransfers(Long accountId) {
        return List.of();
    }

    @Override
    public List<Transfer> findSuccessfulTransfers() {
        return List.of();
    }

    @Override
    public BigDecimal getTotalOutgoingAmount(Long accountId) {
        return null;
    }

    @Override
    public BigDecimal getTotalIncomingAmount(Long accountId) {
        return null;
    }

    @Override
    public List<Transfer> findTransfersWithoutComment() {
        return List.of();
    }

    @Override
    public List<Transfer> findLargeTransfers(BigDecimal minAmount) {
        return List.of();
    }
}
