package ee.digit25.detector.domain.transaction;

import ee.digit25.detector.domain.account.AccountValidator;
import ee.digit25.detector.domain.device.DeviceValidator;
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

    public boolean isLegitimate(Transaction transaction, List<Person> persons) {

        Person personRecipient = persons.stream()
                .filter(person -> person.getPersonCode().equals(transaction.getRecipient()))
                .findFirst()
                .orElse(null);

        Person personSender = persons.stream()
                .filter(person -> person.getPersonCode().equals(transaction.getSender()))
                .findFirst()
                .orElse(null);

        return personValidator.isValid(personRecipient) && personValidator.isValid(personSender) && deviceValidator.isValid(transaction.getDeviceMac()) && accountValidator.isValidSenderAccount(transaction.getSenderAccount(), transaction.getAmount(), transaction.getSender()) && accountValidator.isValidRecipientAccount(transaction.getRecipientAccount(), transaction.getRecipient());
    }
}
