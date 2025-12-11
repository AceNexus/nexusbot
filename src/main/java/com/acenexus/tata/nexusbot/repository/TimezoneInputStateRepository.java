package com.acenexus.tata.nexusbot.repository;

import com.acenexus.tata.nexusbot.entity.TimezoneInputState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 時區輸入狀態 Repository
 * 管理等待輸入時區的聊天室狀態
 */
@Repository
public interface TimezoneInputStateRepository extends JpaRepository<TimezoneInputState, String> {

    /**
     * 檢查聊天室是否正在等待時區輸入且未過期
     *
     * @param roomId 聊天室 ID
     * @param now    當前時間
     * @return 是否存在有效狀態
     */
    @Query("SELECT COUNT(t) > 0 FROM TimezoneInputState t WHERE t.roomId = :roomId AND t.expiresAt > :now")
    boolean existsByRoomIdAndNotExpired(@Param("roomId") String roomId, @Param("now") LocalDateTime now);

    /**
     * 根據聊天室 ID 查詢未過期的時區輸入狀態
     *
     * @param roomId 聊天室 ID
     * @param now    當前時間
     * @return 時區輸入狀態（可能為空）
     */
    @Query("SELECT t FROM TimezoneInputState t WHERE t.roomId = :roomId AND t.expiresAt > :now")
    Optional<TimezoneInputState> findByRoomIdAndNotExpired(@Param("roomId") String roomId, @Param("now") LocalDateTime now);
}
