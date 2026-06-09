package com.topnivo.backend.service;

import com.topnivo.backend.exception.exception.ErrorMessages;
import com.topnivo.backend.exception.exception.NoSuchResourceException;
import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.model.entity.StorePackage;
import com.topnivo.backend.repository.StorePackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StorePackageService {

    private final StorePackageRepository storePackageRepository;
    private final MemberService memberService;

    public StorePackage findStorePackageById(int storePackageId) {
        Optional<StorePackage> optional = storePackageRepository.findById(storePackageId);

        if (optional.isEmpty()) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_PACKAGE);
        }

        return optional.get();
    }

    public List<StorePackage> getAllStorePackages(String storeId) {
        Member store = memberService.findMemberByMemberId(storeId);

        return storePackageRepository.findByStore(store);
    }
}
