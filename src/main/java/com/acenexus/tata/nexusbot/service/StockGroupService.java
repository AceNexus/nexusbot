package com.acenexus.tata.nexusbot.service;

import com.acenexus.tata.nexusbot.dto.StockGroupDto;
import com.acenexus.tata.nexusbot.dto.StockGroupItemDto;
import com.acenexus.tata.nexusbot.entity.StockGroup;
import com.acenexus.tata.nexusbot.entity.StockGroupItem;
import com.acenexus.tata.nexusbot.repository.StockGroupItemRepository;
import com.acenexus.tata.nexusbot.repository.StockGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 股票群組服務
 * 處理股票群組的 CRUD 操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockGroupService {

    private final StockGroupRepository stockGroupRepository;
    private final StockGroupItemRepository stockGroupItemRepository;
    private final StockSymbolService stockSymbolService;

    /**
     * 取得使用者的所有群組（含股票列表）
     */
    @Transactional(readOnly = true)
    public List<StockGroupDto> getGroupsByUserId(String userId) {
        List<StockGroup> groups = stockGroupRepository.findByUserIdAndIsActiveTrueOrderByDisplayOrderAsc(userId);

        if (groups.isEmpty()) {
            return new ArrayList<>();
        }

        // 批次查詢所有群組的股票
        List<Long> groupIds = groups.stream().map(StockGroup::getId).collect(Collectors.toList());
        List<StockGroupItem> allItems = stockGroupItemRepository.findByGroupIdIn(groupIds);

        // 按群組 ID 分組
        Map<Long, List<StockGroupItem>> itemsByGroupId = allItems.stream()
                .collect(Collectors.groupingBy(StockGroupItem::getGroupId));

        return groups.stream()
                .map(group -> toDto(group, itemsByGroupId.getOrDefault(group.getId(), new ArrayList<>())))
                .collect(Collectors.toList());
    }

    /**
     * 建立新群組
     */
    @Transactional
    public StockGroupDto createGroup(String userId, String name) {
        Integer maxOrder = stockGroupRepository.findMaxDisplayOrderByUserId(userId);

        StockGroup group = StockGroup.builder()
                .userId(userId)
                .name(name)
                .displayOrder(maxOrder + 1)
                .isActive(true)
                .isSelected(false)
                .build();

        StockGroup saved = stockGroupRepository.save(group);
        log.info("Created stock group - userId={}, groupId={}, name={}", userId, saved.getId(), name);

        return toDto(saved, new ArrayList<>());
    }

    /**
     * 重新命名群組
     */
    @Transactional
    public Optional<StockGroupDto> renameGroup(String userId, Long groupId, String newName) {
        return stockGroupRepository.findByIdAndUserIdAndIsActiveTrue(groupId, userId)
                .map(group -> {
                    group.setName(newName);
                    StockGroup saved = stockGroupRepository.save(group);
                    log.info("Renamed stock group - userId={}, groupId={}, newName={}", userId, groupId, newName);

                    List<StockGroupItem> items = stockGroupItemRepository.findByGroupIdOrderByDisplayOrderAsc(groupId);
                    return toDto(saved, items);
                });
    }

    /**
     * 刪除群組（軟刪除）
     */
    @Transactional
    public boolean deleteGroup(String userId, Long groupId) {
        return stockGroupRepository.findByIdAndUserIdAndIsActiveTrue(groupId, userId)
                .map(group -> {
                    group.setIsActive(false);
                    group.setIsSelected(false);
                    stockGroupRepository.save(group);
                    log.info("Deleted stock group - userId={}, groupId={}", userId, groupId);
                    return true;
                })
                .orElse(false);
    }

    /**
     * 選取群組
     */
    @Transactional
    public Optional<StockGroupDto> selectGroup(String userId, Long groupId) {
        return stockGroupRepository.findByIdAndUserIdAndIsActiveTrue(groupId, userId)
                .map(group -> {
                    // 取消所有群組的選中狀態
                    stockGroupRepository.deselectAllByUserId(userId);

                    // 選取指定群組
                    group.setIsSelected(true);
                    StockGroup saved = stockGroupRepository.save(group);
                    log.info("Selected stock group - userId={}, groupId={}", userId, groupId);

                    List<StockGroupItem> items = stockGroupItemRepository.findByGroupIdOrderByDisplayOrderAsc(groupId);
                    return toDto(saved, items);
                });
    }

    /**
     * 新增股票到群組
     */
    @Transactional
    public Optional<StockGroupItemDto> addStockToGroup(String userId, Long groupId, String stockSymbol, String stockName) {
        // 驗證群組存在且屬於該使用者
        Optional<StockGroup> groupOpt = stockGroupRepository.findByIdAndUserIdAndIsActiveTrue(groupId, userId);
        if (groupOpt.isEmpty()) {
            log.warn("Group not found or not owned by user - userId={}, groupId={}", userId, groupId);
            return Optional.empty();
        }

        // 檢查股票是否已存在於群組中
        if (stockGroupItemRepository.findByGroupIdAndStockSymbol(groupId, stockSymbol).isPresent()) {
            log.warn("Stock already exists in group - groupId={}, symbol={}", groupId, stockSymbol);
            return Optional.empty();
        }

        Integer maxOrder = stockGroupItemRepository.findMaxDisplayOrderByGroupId(groupId);

        // 取得市場資訊
        String market = stockSymbolService.getMarketBySymbol(stockSymbol);

        StockGroupItem item = StockGroupItem.builder()
                .groupId(groupId)
                .stockSymbol(stockSymbol)
                .stockName(stockName)
                .market(market)
                .displayOrder(maxOrder + 1)
                .build();

        StockGroupItem saved = stockGroupItemRepository.save(item);
        log.info("Added stock to group - groupId={}, symbol={}, name={}, market={}", groupId, stockSymbol, stockName, market);

        return Optional.of(toItemDto(saved));
    }

    /**
     * 從群組移除股票
     */
    @Transactional
    public boolean removeStockFromGroup(String userId, Long groupId, String stockSymbol) {
        // 驗證群組存在且屬於該使用者
        Optional<StockGroup> groupOpt = stockGroupRepository.findByIdAndUserIdAndIsActiveTrue(groupId, userId);
        if (groupOpt.isEmpty()) {
            log.warn("Group not found or not owned by user - userId={}, groupId={}", userId, groupId);
            return false;
        }

        return stockGroupItemRepository.findByGroupIdAndStockSymbol(groupId, stockSymbol)
                .map(item -> {
                    stockGroupItemRepository.delete(item);
                    log.info("Removed stock from group - groupId={}, symbol={}", groupId, stockSymbol);
                    return true;
                })
                .orElse(false);
    }

    private StockGroupDto toDto(StockGroup group, List<StockGroupItem> items) {
        return StockGroupDto.builder()
                .id(group.getId())
                .name(group.getName())
                .displayOrder(group.getDisplayOrder())
                .isSelected(group.getIsSelected())
                .stocks(items.stream().map(this::toItemDto).collect(Collectors.toList()))
                .build();
    }

    private StockGroupItemDto toItemDto(StockGroupItem item) {
        String market = item.getMarket();
        if (market == null || market.isEmpty()) {
            market = stockSymbolService.getMarketBySymbol(item.getStockSymbol());
        }
        
        return StockGroupItemDto.builder()
                .id(item.getId())
                .stockSymbol(item.getStockSymbol())
                .stockName(item.getStockName())
                .market(market)
                .displayOrder(item.getDisplayOrder())
                .build();
    }
}
