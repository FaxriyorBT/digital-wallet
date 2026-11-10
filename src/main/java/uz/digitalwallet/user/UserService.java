package uz.digitalwallet.user;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.digitalwallet.common.exception.BusinessException;
import uz.digitalwallet.common.exception.ErrorCode;
import uz.digitalwallet.common.web.PageResponse;
import uz.digitalwallet.user.dto.ChangePasswordRequest;
import uz.digitalwallet.user.dto.UpdateProfileRequest;
import uz.digitalwallet.user.dto.UserResponse;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final UserMapper userMapper;

    public UserResponse getProfile(UUID userId) {
        return userMapper.toResponse(findUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        if (request.fullName() != null) {
            user.changeFullName(request.fullName().trim());
        }
        if (request.phone() != null && !request.phone().equals(user.getPhone())) {
            if (users.existsByPhone(request.phone())) {
                throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED);
            }
            user.changePhone(request.phone());
        }
        return userMapper.toResponse(user);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = findUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.WRONG_CURRENT_PASSWORD);
        }
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        // sessions opened with the old password should not survive the change
        eventPublisher.publishEvent(new UserAccessRevokedEvent(userId));
    }

    public PageResponse<UserResponse> search(String query, UserStatus status, Pageable pageable) {
        String pattern = "%" + (query == null ? "" : query.trim().toLowerCase(Locale.ROOT)) + "%";
        Collection<UserStatus> statuses = status == null ? EnumSet.allOf(UserStatus.class) : EnumSet.of(status);
        return PageResponse.from(users.search(pattern, statuses, pageable).map(userMapper::toResponse));
    }

    @Transactional
    public UserResponse changeStatus(UUID userId, UserStatus status) {
        User user = findUser(userId);
        if (user.isAdmin()) {
            throw new BusinessException(ErrorCode.STATUS_CHANGE_NOT_ALLOWED, "Administrator accounts cannot be blocked");
        }
        user.changeStatus(status);
        if (status == UserStatus.BLOCKED) {
            eventPublisher.publishEvent(new UserAccessRevokedEvent(userId));
        }
        return userMapper.toResponse(user);
    }

    private User findUser(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
