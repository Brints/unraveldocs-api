package com.extractor.unraveldocs.credit.repository;

import com.extractor.unraveldocs.credit.datamodel.CreditPackName;
import com.extractor.unraveldocs.credit.model.CreditPack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreditPackRepository extends JpaRepository<CreditPack, String> {

    List<CreditPack> findByIsActiveTrue();

    Optional<CreditPack> findByName(CreditPackName name);
}
