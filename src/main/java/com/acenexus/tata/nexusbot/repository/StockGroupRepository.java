package com.acenexus.tata.nexusbot.repository;

import com.acenexus.tata.nexusbot.entity.StockGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 股票群組資料庫存取層
 */
@Repository
public interface StockGroupRepository extends JpaRepository<StockGroup, Long> {

    /**
     * 根據使用者 ID 查詢所有啟用的群組，按顯示順序排序
     */
    List<StockGroup> findByUserIdAndIsActiveTrueOrderByDisplayOrderAsc(String userId);

    /**
     * 取消該使用者所有群組的選中狀態
     */
    @Modifying
    @Query("UPDATE StockGroup g SET g.isSelected = false WHERE g.userId = :userId")
    void deselectAllByUserId(@Param("userId") String userId);

    /**
     * 根據群組 ID 和使用者 ID 查詢群組（確保權限）
     */
    Optional<StockGroup> findByIdAndUserIdAndIsActiveTrue(Long id, String userId);

    /**
     * 取得使用者最大的顯示順序
     */
    @Query("SELECT COALESCE(MAX(g.displayOrder), 0) FROM StockGroup g WHERE g.userId = :userId AND g.isActive = true")
    Integer findMaxDisplayOrderByUserId(@Param("userId") String userId);
}
