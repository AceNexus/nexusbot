package com.acenexus.tata.nexusbot.template;

import com.acenexus.tata.nexusbot.config.properties.OsmProperties;
import com.acenexus.tata.nexusbot.location.ToiletLocation;
import com.linecorp.bot.model.action.URIAction;
import com.linecorp.bot.model.message.FlexMessage;
import com.linecorp.bot.model.message.Message;
import com.linecorp.bot.model.message.flex.component.Box;
import com.linecorp.bot.model.message.flex.component.Button;
import com.linecorp.bot.model.message.flex.component.FlexComponent;
import com.linecorp.bot.model.message.flex.component.Text;
import com.linecorp.bot.model.message.flex.container.Bubble;
import com.linecorp.bot.model.message.flex.container.Carousel;
import com.linecorp.bot.model.message.flex.unit.FlexAlign;
import com.linecorp.bot.model.message.flex.unit.FlexFontSize;
import com.linecorp.bot.model.message.flex.unit.FlexLayout;
import com.linecorp.bot.model.message.flex.unit.FlexMarginSize;
import com.linecorp.bot.model.message.flex.unit.FlexPaddingSize;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.acenexus.tata.nexusbot.constants.Actions.MAIN_MENU;
import static com.acenexus.tata.nexusbot.template.UIConstants.Colors;

/**
 * 位置搜尋（廁所搜尋）相關的訊息範本建構器
 */
@Component
@RequiredArgsConstructor
public class LocationTemplateBuilder extends FlexMessageTemplateBuilder {

    private final OsmProperties osmProperties;

    /**
     * 找附近廁所的使用說明
     */
    public Message findToiletsInstruction() {
        return createCard(
                "找附近廁所",
                "請分享您的目前位置，我會為您搜尋附近的廁所。\n\n• 點擊 LINE 輸入框旁的「+」按鈕\n• 選擇「位置」\n• 選擇「目前位置」或手動選擇地點",
                Arrays.asList(
                        createNavigateButton("返回主選單", MAIN_MENU)
                )
        );
    }

    /**
     * 附近廁所搜尋結果
     */
    public Message nearbyToiletsResponse(List<ToiletLocation> toilets, double userLatitude, double userLongitude) {
        if (toilets == null || toilets.isEmpty()) {
            return createNoToiletsFoundMessage();
        }

        // 限制顯示數量，避免資訊過載（LINE Carousel 上限）
        int displayCount = Math.min(toilets.size(), osmProperties.getCarouselMaxItems());
        List<ToiletLocation> displayToilets = toilets.subList(0, displayCount);

        return createToiletCarousel(displayToilets);
    }

    /**
     * 創建廁所 Carousel 展示
     */
    private FlexMessage createToiletCarousel(List<ToiletLocation> toilets) {
        List<Bubble> bubbles = new ArrayList<>();

        for (int i = 0; i < toilets.size(); i++) {
            ToiletLocation toilet = toilets.get(i);
            bubbles.add(createToiletBubble(toilet, i + 1));
        }

        return FlexMessage.builder()
                .altText(String.format("找到 %d 個附近廁所", toilets.size()))
                .contents(Carousel.builder()
                        .contents(bubbles)
                        .build())
                .build();
    }

    /**
     * 創建單個廁所的 Bubble 卡片
     */
    private Bubble createToiletBubble(ToiletLocation toilet, int index) {
        // 標頭：序號和距離
        Box header = Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(Arrays.asList(
                        Text.builder()
                                .text(String.format("🚻 第 %d 選擇", index))
                                .size(FlexFontSize.SM)
                                .color("#FFFFFF")
                                .weight(Text.TextWeight.BOLD)
                                .align(FlexAlign.CENTER)
                                .build(),
                        Text.builder()
                                .text(toilet.getDistanceFormatted())
                                .size(FlexFontSize.XS)
                                .color("#E3F8FF")
                                .align(FlexAlign.CENTER)
                                .build()
                ))
                .paddingAll(FlexPaddingSize.MD)
                .backgroundColor(Colors.PRIMARY)
                .build();

        // 主體：名稱、地址、狀態資訊
        List<FlexComponent> bodyComponents = new ArrayList<>();

        // 名稱
        bodyComponents.add(Text.builder()
                .text(toilet.getName())
                .size(FlexFontSize.LG)
                .weight(Text.TextWeight.BOLD)
                .color("#1F2937")
                .wrap(true)
                .maxLines(2)
                .build());

        // 地址
        if (toilet.getVicinity() != null && !toilet.getVicinity().trim().isEmpty()) {
            bodyComponents.add(Text.builder()
                    .text("📍 " + toilet.getVicinity())
                    .size(FlexFontSize.SM)
                    .color(Colors.GRAY)
                    .wrap(true)
                    .maxLines(2)
                    .margin(FlexMarginSize.SM)
                    .build());
        }

        // 狀態：營業中/已關閉 + 無障礙
        String statusText = toilet.isOpen() ? "營業中" : "已關閉";
        String statusColor = toilet.isOpen() ? Colors.SUCCESS : Colors.ERROR;
        String statusEmoji = toilet.isOpen() ? "✅" : "❌";
        String wheelchairText = toilet.isHasWheelchairAccess() ? "♿ 有" : "♿ 無";
        String wheelchairColor = toilet.isHasWheelchairAccess() ? Colors.SUCCESS : Colors.GRAY;

        bodyComponents.add(Box.builder()
                .layout(FlexLayout.HORIZONTAL)
                .contents(Arrays.asList(
                        Text.builder()
                                .text(statusEmoji + " " + statusText)
                                .size(FlexFontSize.SM)
                                .color(statusColor)
                                .weight(Text.TextWeight.BOLD)
                                .flex(1)
                                .build(),
                        Text.builder()
                                .text(wheelchairText)
                                .size(FlexFontSize.SM)
                                .color(wheelchairColor)
                                .weight(Text.TextWeight.BOLD)
                                .flex(1)
                                .align(FlexAlign.END)
                                .build()
                ))
                .margin(FlexMarginSize.SM)
                .build());

        Box body = Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(bodyComponents)
                .spacing(FlexMarginSize.XS)
                .paddingAll(FlexPaddingSize.MD)
                .build();

        // 底部：導航按鈕
        Box footer = Box.builder()
                .layout(FlexLayout.VERTICAL)
                .contents(Arrays.asList(
                        Button.builder()
                                .style(Button.ButtonStyle.PRIMARY)
                                .action(createToiletNavigationAction(toilet))
                                .color(Colors.PRIMARY)
                                .build()
                ))
                .spacing(FlexMarginSize.SM)
                .paddingAll(FlexPaddingSize.MD)
                .build();

        return Bubble.builder()
                .header(header)
                .body(body)
                .footer(footer)
                .build();
    }

    /**
     * 創建廁所導航 Action
     */
    private URIAction createToiletNavigationAction(ToiletLocation toilet) {
        try {
            String url = String.format("https://www.google.com/maps/dir/?api=1&destination=%f,%f&travelmode=walking",
                    toilet.getLatitude(), toilet.getLongitude());
            URI uri = new URI(url);
            return new URIAction("🗺️ 導航", uri, null);
        } catch (URISyntaxException e) {
            return new URIAction("🗺️ 導航", URI.create("https://www.google.com/maps"), null);
        }
    }

    /**
     * 找不到廁所的訊息
     */
    private FlexMessage createNoToiletsFoundMessage() {
        return FlexMessage.builder()
                .altText("找不到附近廁所")
                .contents(Bubble.builder()
                        .body(Box.builder()
                                .layout(FlexLayout.VERTICAL)
                                .contents(Arrays.asList(
                                        Text.builder()
                                                .text("😔")
                                                .size(FlexFontSize.XXL)
                                                .align(FlexAlign.CENTER)
                                                .build(),
                                        Text.builder()
                                                .text("找不到附近廁所")
                                                .size(FlexFontSize.LG)
                                                .weight(Text.TextWeight.BOLD)
                                                .align(FlexAlign.CENTER)
                                                .color("#1F2937")
                                                .margin(FlexMarginSize.MD)
                                                .build(),
                                        Text.builder()
                                                .text("建議：\n• 移動到商業區或交通要道\n• 尋找便利商店、購物中心\n• 擴大搜尋範圍")
                                                .size(FlexFontSize.SM)
                                                .color(Colors.GRAY)
                                                .wrap(true)
                                                .margin(FlexMarginSize.MD)
                                                .build()
                                ))
                                .paddingAll(FlexPaddingSize.XL)
                                .spacing(FlexMarginSize.SM)
                                .build())
                        .build())
                .build();
    }
}
