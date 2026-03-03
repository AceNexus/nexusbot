package com.acenexus.tata.nexusbot.richmenu;

/**
 * Rich Menu 管理服務
 * 負責刪除舊選單、建立新選單、上傳圖片、設定為預設選單
 */
public interface RichMenuService {

    /**
     * 刪除舊 Rich Menu、建立新 Rich Menu、上傳圖片、設為 Default
     */
    void setupRichMenu();
}
