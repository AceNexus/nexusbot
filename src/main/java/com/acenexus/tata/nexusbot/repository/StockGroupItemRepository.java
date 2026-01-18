package com.acenexus.tata.nexusbot.repository;

import com.acenexus.tata.nexusbot.entity.StockGroupItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 股票群組項目資料庫存取層
 */
@Repository
public interface StockGroupItemRepository extends JpaRepository<StockGroupItem, Long> {

    /**
     * 根據群組 ID 查詢所有股票，按顯示順序排序
     */
    List<StockGroupItem> findByGroupIdOrderByDisplayOrderAsc(Long groupId);

    /**
     * 根據群組 ID 和股票代號查詢
     */
    Optional<StockGroupItem> findByGroupIdAndStockSymbol(Long groupId, String stockSymbol);

    /**
     * 刪除群組內所有股票
     */
    void deleteByGroupId(Long groupId);

    /**
     * 取得群組內最大的顯示順序
     */
    @Query("SELECT COALESCE(MAX(i.displayOrder), 0) FROM StockGroupItem i WHERE i.groupId = :groupId")
    Integer findMaxDisplayOrderByGroupId(@Param("groupId") Long groupId);

    /**
     * 根據群組 ID 列表查詢所有股票
     */
    List<StockGroupItem> findByGroupIdIn(List<Long> groupIds);
}
