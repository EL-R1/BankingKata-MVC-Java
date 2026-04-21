package com.banking.mapper;

import com.banking.dto.*;
import com.banking.model.BankAccount;
import com.banking.model.SavingsAccount;
import com.banking.model.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AccountMapper {

    AccountDTO toAccountDTO(BankAccount account);

    SavingsAccountDTO toSavingsAccountDTO(SavingsAccount account);

    OperationDTO toOperationDTO(Transaction transaction);

    List<OperationDTO> toOperationDTOList(List<Transaction> transactions);
}