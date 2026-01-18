package com.acenexus.tata.nexusbot.controller;

import com.acenexus.tata.nexusbot.dto.StockGroupDto;
import com.acenexus.tata.nexusbot.dto.StockGroupItemDto;
import com.acenexus.tata.nexusbot.service.StockGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 股票群組 REST API
 * 處理股票群組的 CRUD 操作
 */
@Slf4j
@RestController
@RequestMapping("/api/stock-groups")
@RequiredArgsConstructor
public class StockGroupController {

    private final StockGroupService stockGroupService;

    /**
     * 取得使用者的所有群組
     */
    @GetMapping
    public ResponseEntity<List<StockGroupDto>> getGroups(
            @RequestHeader("X-User-Id") String userId) {
        List<StockGroupDto> groups = stockGroupService.getGroupsByUserId(userId);
        return ResponseEntity.ok(groups);
    }

    /**
     * 建立新群組
     */
    @PostMapping
    public ResponseEntity<StockGroupDto> createGroup(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, String> request) {
        String name = request.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        StockGroupDto group = stockGroupService.createGroup(userId, name.trim());
        return ResponseEntity.ok(group);
    }

    /**
     * 重新命名群組
     */
    @PutMapping("/{id}")
    public ResponseEntity<StockGroupDto> renameGroup(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String name = request.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return stockGroupService.renameGroup(userId, id, name.trim())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 刪除群組
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long id) {
        if (stockGroupService.deleteGroup(userId, id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 選取群組
     */
    @PostMapping("/{id}/select")
    public ResponseEntity<StockGroupDto> selectGroup(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long id) {
        return stockGroupService.selectGroup(userId, id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 新增股票到群組
     */
    @PostMapping("/{id}/stocks")
    public ResponseEntity<StockGroupItemDto> addStock(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String symbol = request.get("symbol");
        String name = request.get("name");
        if (symbol == null || symbol.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return stockGroupService.addStockToGroup(userId, id, symbol.trim(), name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().build());
    }

    /**
     * 從群組移除股票
     */
    @DeleteMapping("/{id}/stocks/{symbol}")
    public ResponseEntity<Void> removeStock(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable Long id,
            @PathVariable String symbol) {
        if (stockGroupService.removeStockFromGroup(userId, id, symbol)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
