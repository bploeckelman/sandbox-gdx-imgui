package lando.systems.game.ui;

import imgui.ImGui;

public class ImGuiUtil {
    public static void textCentered(String text) {
        float widthAvailable = ImGui.getContentRegionAvailX();
        float widthText = ImGui.calcTextSize(text).x;
        ImGui.setCursorPosX((widthAvailable - widthText) / 2f);
        ImGui.text(text);
    }

    public static void rectFilled(float width, float height, int color, float roundingPercent) {
        var drawList = ImGui.getWindowDrawList();
        var cursor = ImGui.getCursorScreenPos();
        var lineHeight = ImGui.getTextLineHeight();

        drawList.addRectFilled(
            cursor.x, cursor.y,
            cursor.x + width,
            cursor.y + height,
            color, lineHeight * roundingPercent);
    }
}
