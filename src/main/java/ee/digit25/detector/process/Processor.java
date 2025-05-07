package ee.digit25.detector.process;

import ee.digit25.detector.domain.account.external.AccountRequester;
import ee.digit25.detector.domain.account.external.api.Account;
import ee.digit25.detector.domain.device.external.DeviceRequester;
import ee.digit25.detector.domain.device.external.api.Device;
import ee.digit25.detector.domain.person.external.PersonRequester;
import ee.digit25.detector.domain.person.external.api.Person;
import ee.digit25.detector.domain.transaction.TransactionValidator;
import ee.digit25.detector.domain.transaction.external.TransactionRequester;
import ee.digit25.detector.domain.transaction.external.TransactionVerifier;
import ee.digit25.detector.domain.transaction.external.api.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class Processor {

    private final int TRANSACTION_BATCH_SIZE = 1600;

    private int threadCount = 64;

    private final TransactionRequester requester;

    private final TransactionValidator validator;

    private final TransactionVerifier verifier;
    private final PersonRequester personRequester;
    private final DeviceRequester deviceRequester;
    private final AccountRequester accountRequester;

    private final ExecutorService executorService = Executors.newFixedThreadPool(
        threadCount
    );

    @Scheduled(fixedDelay = 50) //Runs every x ms after the last run
    public void process() {
        log.info("Starting to process a batch of transactions of size {}", TRANSACTION_BATCH_SIZE);

        List<Transaction> transactions = requester.getUnverified(TRANSACTION_BATCH_SIZE);
        List<String> senderCodes = transactions.stream().map(Transaction::getSender).toList();
        List<String> recipientCodes = transactions.stream().map(Transaction::getRecipient).toList();
        List<String> allCodes = new ArrayList<>();
        allCodes.addAll(senderCodes);
        allCodes.addAll(recipientCodes);
        List<Person> persons = personRequester.get(allCodes);
        List<String> macs = transactions.stream().map(Transaction::getDeviceMac).toList();
        List<Device> devices = deviceRequester.get(macs).stream().filter(device -> !device.getIsBlacklisted()).toList();
        List<String> senderAccountCodes = transactions.stream().map(Transaction::getSenderAccount).toList();
        List<String> receipentAccountCodes = transactions.stream().map(Transaction::getRecipientAccount).toList();
        List<String> allAccountCodes = new ArrayList<>();
        allAccountCodes.addAll(senderAccountCodes);
        allAccountCodes.addAll(receipentAccountCodes);
        List<Account> accounts = accountRequester.get(allAccountCodes);

        List<CompletableFuture<Void>> futures = transactions
            .stream()
            .map(transaction -> CompletableFuture.runAsync(
                () -> {
                    try {
                        if (validator.isLegitimate(transaction, persons, devices, accounts)) {
                            log.info("Legitimate transaction {}", transaction.getId());
                            verifier.verify(transaction);
                        } else {
                            log.info("Not legitimate transaction {}", transaction.getId());
                            verifier.reject(transaction);
                        }
                    } catch (Exception e) {
                        log.error("Error processing transaction {}: {}", transaction.getId(), e.getMessage(), e);
                    }
                }, executorService
            ))
            .toList();

        // Wait for all transactions to be processed
        CompletableFuture
            .allOf(futures.toArray(new CompletableFuture[0]))
            .join();
    }
}
