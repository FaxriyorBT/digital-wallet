package uz.digitalwallet.user;

import org.mapstruct.Mapper;
import uz.digitalwallet.user.dto.UserResponse;

@Mapper
public interface UserMapper {

    UserResponse toResponse(User user);
}
