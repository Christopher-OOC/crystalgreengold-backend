package com.topnivo.backend.repository;

import com.topnivo.backend.model.entity.Package;
import com.topnivo.backend.model.entity.StorePackage;
import com.topnivo.backend.model.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StorePackageRepository extends JpaRepository<StorePackage, Integer> {

    List<StorePackage> findByStore(Member store);

    StorePackage findByStoreAndPac(Member store, Package pac);

}
