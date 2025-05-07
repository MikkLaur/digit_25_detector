package ee.digit25.detector.domain.transaction;

import ee.digit25.detector.domain.account.AccountValidator;
import ee.digit25.detector.domain.account.external.api.Account;
import ee.digit25.detector.domain.device.DeviceValidator;
import ee.digit25.detector.domain.device.external.api.Device;
import ee.digit25.detector.domain.person.PersonValidator;
import ee.digit25.detector.domain.person.external.api.Person;
import ee.digit25.detector.domain.transaction.external.api.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionValidator {

    private final PersonValidator personValidator;
    private final DeviceValidator deviceValidator;
    private final AccountValidator accountValidator;

    public boolean isLegitimate(Transaction transaction, List<Person> persons, List<Device> devices, List<Account> accounts) {

        Person personRecipient = persons.stream()
                .filter(person -> person.getPersonCode().equals(transaction.getRecipient()))
                .findFirst()
                .orElse(null);

        Person personSender = persons.stream()
                .filter(person -> person.getPersonCode().equals(transaction.getSender()))
                .findFirst()
                .orElse(null);

        Device deviceCheck = devices.stream()
                .filter(device -> device.getMac().equals(transaction.getDeviceMac()))
                .findFirst()
                .orElse(null);

        Account accountSender = accounts.stream()
                .filter(account -> account.getNumber().equals(transaction.getSenderAccount()))
                .findFirst()
                .orElse(null);

        Account receipentAccount = accounts.stream()
                .filter(account -> account.getNumber().equals(transaction.getRecipientAccount()))
                .findFirst()
                .orElse(null);

        return deviceValidator.isValid(deviceCheck)
                && personValidator.isValid(personRecipient)
                && personValidator.isValid(personSender)
                && accountValidator.isValidSenderAccount(accountSender, transaction.getAmount(), transaction.getSender())
                && accountValidator.isValidRecipientAccount(receipentAccount, transaction.getRecipient());
    }
}
