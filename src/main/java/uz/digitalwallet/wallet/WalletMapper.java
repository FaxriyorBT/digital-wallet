package uz.digitalwallet.wallet;

import org.mapstruct.Mapper;
import uz.digitalwallet.wallet.dto.WalletResponse;

@Mapper
public interface WalletMapper {

    WalletResponse toResponse(Wallet wallet);
}
