package com.topnivo.backend.service;

import com.topnivo.backend.exception.exception.BadRequestException;
import com.topnivo.backend.exception.exception.ErrorMessages;
import com.topnivo.backend.exception.exception.NoSuchResourceException;
import com.topnivo.backend.exception.exception.ResourceAlreadyExistException;
import com.topnivo.backend.mapper.PackageMapper;
import com.topnivo.backend.model.constant.PackageName;
import com.topnivo.backend.model.entity.Member;
import com.topnivo.backend.model.entity.Package;
import com.topnivo.backend.model.entity.StorePackage;
import com.topnivo.backend.model.request.PackageCreateRequest;
import com.topnivo.backend.repository.MemberRepository;
import com.topnivo.backend.repository.PackageRepository;
import com.topnivo.backend.repository.StorePackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PackageService {

    private final PackageRepository packageRepository;
    private final StorePackageRepository storePackageRepository;
    private final PackageMapper packageMapper;
    private final FileService fileService;
    private final MemberRepository memberRepository;

    public Package createAPackage(PackageCreateRequest request, MultipartFile multipartFile) throws IOException {
        checkIfAdminIsValid();

        List<String> packageNames = Arrays.stream(PackageName.values()).map(Enum::name).toList();
        if (!packageNames.contains(request.getName())) {
            throw new BadRequestException(ErrorMessages.INVALID_PACKAGE_NAME);
        }

        Package _package = packageMapper.requestToPackage(request);
        checkIfPackageExistsByName(_package);
        if (multipartFile.isEmpty()) {
            throw new BadRequestException(ErrorMessages.NO_IMAGE_IN_PRODUCT);
        }
        _package.setImage(fileService.fileBtyeToGenerateFileUrl(multipartFile.getBytes()));

        return packageRepository.save(_package);
    }

    public Package findPackageById(int id) {
        Package aPackage = packageRepository.findById(id);
        if (Objects.isNull(aPackage)) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_PACKAGE);
        }

        return aPackage;
    }

    private void checkIfPackageExistsByName(Package _package) {
        if (!Objects.isNull(packageRepository.findByName(_package.getName()))) {
            throw new ResourceAlreadyExistException(ErrorMessages.PACKAGE_ALREADY_EXISTS);
        }
    }

    public Package updateAPackage(int id, PackageCreateRequest request, MultipartFile multipartFile) throws IOException {
        checkIfAdminIsValid();

        Package _package = packageRepository.findById(id);
        if (Objects.isNull(_package)) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_PACKAGE);
        }
        Package updatedPackage = packageMapper.requestToPackage(request);
        updatedPackage.setId(id);
        updatedPackage.setImage(_package.getImage());

        if (!Objects.isNull(multipartFile)) {
            updatedPackage.setImage(fileService.fileBtyeToGenerateFileUrl(multipartFile.getBytes()));
        }

        return packageRepository.save(updatedPackage);
    }

    public void deleteAPackage(int packageId) {
        checkIfAdminIsValid();

        packageRepository.deleteById(packageId);
    }

    public List<Package> getAllPackages() {
        return packageRepository.findAll();
    }

    public StorePackage findStorePackageById(int packageId, String storeId) {
        Optional<StorePackage> optional = storePackageRepository.findById(packageId);
        if (optional.isEmpty()) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_PACKAGE);
        }

        return optional.get();
    }

    private void checkIfAdminIsValid() {
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        boolean isAdminSuperAdmin = authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        String adminUserName = SecurityContextHolder.getContext().getAuthentication().getName();
        Member admin = memberRepository.findByUsername(adminUserName);
        if (admin == null) {
            throw new NoSuchResourceException(ErrorMessages.NO_SUCH_MEMBER);
        }

        if (!isAdminSuperAdmin && admin.getSponsor() == null && admin.getPlacer() == null) {
            throw new BadRequestException(ErrorMessages.NO_SPONSOR_AND_PLACER);
        }
    }
}
