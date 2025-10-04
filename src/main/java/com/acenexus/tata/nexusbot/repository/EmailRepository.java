package com.acenexus.tata.nexusbot.repository;

import com.acenexus.tata.nexusbot.entity.Email;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Email 資料存取層
 */
@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {

    /**
     * 查詢聊天室所有啟用的 Email
     *
     * @param roomId 聊天室 ID
     * @return Email 列表
     */
    @Query("SELECT e FROM Email e WHERE e.roomId = :roomId AND e.isActive = true ORDER BY e.createdAt ASC")
    List<Email> findActiveEmailsByRoomId(@Param("roomId") String roomId);

    /**
     * 查詢聊天室所有已啟用通知的 Email
     *
     * @param roomId 聊天室 ID
     * @return Email 列表
     */
    @Query("SELECT e FROM Email e WHERE e.roomId = :roomId AND e.isActive = true AND e.isEnabled = true")
    List<Email> findEnabledEmailsByRoomId(@Param("roomId") String roomId);

    /**
     * 根據 ID 和聊天室 ID 查詢 Email
     *
     * @param id     Email ID
     * @param roomId 聊天室 ID
     * @return Email
     */
    @Query("SELECT e FROM Email e WHERE e.id = :id AND e.roomId = :roomId AND e.isActive = true")
    Optional<Email> findByIdAndRoomId(@Param("id") Long id, @Param("roomId") String roomId);

    /**
     * 軟刪除 Email
     *
     * @param id     Email ID
     * @param roomId 聊天室 ID
     */
    @Query("UPDATE Email e SET e.isActive = false WHERE e.id = :id AND e.roomId = :roomId")
    void softDeleteByIdAndRoomId(@Param("id") Long id, @Param("roomId") String roomId);
}
