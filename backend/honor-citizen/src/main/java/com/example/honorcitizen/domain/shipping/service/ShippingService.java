package com.example.honorcitizen.domain.shipping.service;

import com.example.honorcitizen.common.enums.ApplicationStatus;
import com.example.honorcitizen.common.exception.CustomException;
import com.example.honorcitizen.common.exception.ErrorCode;
import com.example.honorcitizen.domain.application.repository.ApplicationRepository;
import com.example.honorcitizen.domain.shipping.dto.ShippingAddressRegisterResponse;
import com.example.honorcitizen.domain.shipping.dto.ShippingAddressRequest;
import com.example.honorcitizen.domain.shipping.dto.ShippingAddressUpdateResponse;
import com.example.honorcitizen.domain.shipping.entity.ShippingAddress;
import com.example.honorcitizen.domain.shipping.repository.ShippingAddressRepository;
import com.example.honorcitizen.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShippingService {

    private static final int SHIPPING_FEE = 3000;
    private static final String ESTIMATED_DELIVERY_DAYS = "1~3일";
    private static final List<ApplicationStatus> EDITABLE_STATUSES = List.of(
            ApplicationStatus.PENDING,
            ApplicationStatus.REVIEWING,
            ApplicationStatus.PHOTO_REJECTED);

    private final ShippingAddressRepository shippingAddressRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    @Transactional
    public ShippingAddressRegisterResponse register(Long applicationId, Long userId, ShippingAddressRequest request) {
        validateApplicationExists(applicationId);
        validateUserExists(userId);
        validateOwnership(applicationId, userId);
        validateNotExists(applicationId);

        ShippingAddress shippingAddress = ShippingAddress.create(
                applicationId,
                userId,
                request.getRecipientName(),
                request.getZipCode(),
                request.getAddress(),
                request.getAddressDetail(),
                request.getEmail(),
                request.getPhone());

        return ShippingAddressRegisterResponse.of(
                shippingAddressRepository.saveAndFlush(shippingAddress),
                ESTIMATED_DELIVERY_DAYS,
                SHIPPING_FEE);
    }

    @Transactional
    public ShippingAddressUpdateResponse update(Long applicationId, Long userId, ShippingAddressRequest request) {
        validateApplicationExists(applicationId);
        validateOwnership(applicationId, userId);
        validateEditableStatus(applicationId);

        ShippingAddress shippingAddress = findByApplicationId(applicationId);
        validateOwnership(shippingAddress, userId);
        shippingAddress.update(
                request.getRecipientName(),
                request.getZipCode(),
                request.getAddress(),
                request.getAddressDetail(),
                request.getEmail(),
                request.getPhone());

        return ShippingAddressUpdateResponse.from(shippingAddressRepository.saveAndFlush(shippingAddress));
    }

    @Transactional
    public void lock(Long applicationId) {
        ShippingAddress shippingAddress = findByApplicationId(applicationId);
        shippingAddress.lock();
    }

    @Transactional(readOnly = true)
    public ShippingAddress findByApplicationId(Long applicationId) {
        return shippingAddressRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.SHIPPING_NOT_FOUND));
    }

    private void validateApplicationExists(Long applicationId) {
        Optional.of(applicationId)
                .filter(applicationRepository::existsById)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));
    }

    private void validateUserExists(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateOwnership(Long applicationId, Long userId) {
        Optional.of(applicationId)
                .filter(target -> applicationRepository.existsByIdAndUserId(target, userId))
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED_ACCESS));
    }

    private void validateOwnership(ShippingAddress shippingAddress, Long userId) {
        Optional.of(shippingAddress)
                .filter(target -> target.isOwnedBy(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED_ACCESS));
    }

    private void validateNotExists(Long applicationId) {
        Optional.of(applicationId)
                .filter(target -> !shippingAddressRepository.existsByApplicationId(target))
                .orElseThrow(() -> new CustomException(ErrorCode.SHIPPING_ALREADY_EXISTS));
    }

    private void validateEditableStatus(Long applicationId) {
        ApplicationStatus status = applicationRepository.findStatusById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.APPLICATION_NOT_FOUND));
        Optional.of(status)
                .filter(EDITABLE_STATUSES::contains)
                .orElseThrow(() -> new CustomException(ErrorCode.EDIT_PERIOD_EXPIRED));
    }
}
