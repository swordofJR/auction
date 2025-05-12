package com.copyright.repository;

import com.copyright.entity.AuctionItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CopyrightRepository extends JpaRepository<AuctionItems, Long> {
    List<AuctionItems> findByOwnerAddress(String ownerAddress);

    List<AuctionItems> findByStatus(String status);

    List<AuctionItems> findByStatusAndOwnerAddress(String status, String ownerAddress);
}