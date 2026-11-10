package uz.digitalwallet.transaction;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uz.digitalwallet.transaction.dto.TransactionResponse;

@Mapper
public interface TransactionMapper {

    @Mapping(target = "sourceWalletNumber", source = "sourceWallet.number")
    @Mapping(target = "targetWalletNumber", source = "targetWallet.number")
    @Mapping(target = "creditedCurrency", source = "targetWallet.currency")
    TransactionResponse toResponse(Transaction transaction);
}
