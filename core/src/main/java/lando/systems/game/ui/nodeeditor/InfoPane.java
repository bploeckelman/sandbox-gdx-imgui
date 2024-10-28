package lando.systems.game.ui.nodeeditor;

import imgui.ImColor;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiKey;
import lando.systems.game.Util;
import lando.systems.game.shared.FontAwesomeIcons;

public class InfoPane {

    final CanvasNodeEditor editor;

    final ImVec2 size = new ImVec2();

    int selectionChangeCount = 0;

    public InfoPane(CanvasNodeEditor editor) {
        this.editor = editor;
    }

    public void render(float width) {
        var io = ImGui.getIO();

        ImGui.beginChild("info-pane", size.set(width, ImGui.getFrameHeight()));
        {
            // setup spacing for buttons in row
            width = ImGui.getContentRegionAvailX();

            ImGui.beginChild("menubar", size.set(width, 0));
            {
                float spacing = width / 3f;

                if (ImGui.button(STR."\{FontAwesomeIcons.SearchPlus}Zoom to content ")) {
                    editor.zoomToContent();
                }

                ImGui.sameLine(0, spacing);
                if (ImGui.button("Show flow")) {
                    for (var link : editor.links) {
                        editor.flow(link);
                    }
                }

                ImGui.sameLine(0, spacing);
                ImGui.checkbox("Show ordinals", editor.showOrdinals);
                editor.updateSelections();
            }
            ImGui.endChild();

            // setup some vars that will be used for the remaining layout
            var drawList = ImGui.getWindowDrawList();
            var lineHeight = ImGui.getTextLineHeight();
            var cursorScreenPos = ImGui.getCursorScreenPos();

            // node detail rows for each node
            var iconPanelPos = new ImVec2();
            var size = cursorScreenPos.plus(width, lineHeight);
            var activeHeaderColor = ImColor.rgba(ImGui.getStyle().getColors()[ImGuiCol.HeaderActive]);
            drawList.addRectFilled(cursorScreenPos, size, activeHeaderColor, lineHeight * 0.25f);
            ImGui.spacing(); ImGui.sameLine();
            ImGui.text("Nodes");
            ImGui.indent();
            for (var node : editor.nodes) {
                // start new node detail row
                ImGui.pushID(node.pointerId);
                var start = ImGui.getCursorScreenPos();

                // node row: show just touched indicator that fades over time
                float progress = editor.getTouchProgress(node);
                if (progress > 0) {
                    ImGui.getWindowDrawList().addLine(
                        start.x - 8f, start.y,
                        start.x - 8f, start.y + lineHeight,
                        ImColor.rgba(1f, 0f, 0f, 1f - progress), 4f);
                }

                // node row: name and handle selection input, including multi-select
                var isSelected = editor.isSelected(node);
                if (ImGui.selectable(node.toLabel(), isSelected)) {
                    if (io.getKeyCtrl()) {
                        if (isSelected) {
                            editor.deselect(node);
                        } else {
                            editor.select(node, true);
                        }
                    } else {
                        editor.select(node);
                    }
                    editor.navigateToSelection();
                }

                // node row: on hover - show node state in tooltip
                if (ImGui.isItemHovered() && node.hasState()) {
                    ImGui.setTooltip(STR."State: \{node.getState()}");
                }

                // node row: pointer id text
                var style = ImGui.getStyle();
                var iconSize = lineHeight;
                var pointerId = STR."(\{node.pointerId})";
                var textSize = ImGui.calcTextSize(pointerId);
                iconPanelPos.set(
                    start.x + width - style.getFramePaddingX() - style.getIndentSpacing() - iconSize - iconSize - style.getItemInnerSpacingX(),
                    start.y + (lineHeight - iconSize) / 2f);
                float posX = iconPanelPos.x - textSize.x - style.getItemInnerSpacingX();
                drawList.addText(posX, start.y, Util.IM_COLOR_WHITE, pointerId);

                // node row: icons
                ImVec2 iconTextSize;
                ImGui.setCursorScreenPos(iconPanelPos);
                // - save state icon and save handling
                {
                    var iconText = FontAwesomeIcons.Save;
                    iconTextSize = ImGui.calcTextSize(iconText);

                    ImGui.setItemAllowOverlap();
                    if (!node.hasSavedState()) {
                        // use an invisible button in order to show the icon differently depending on input state
                        if (ImGui.invisibleButton("save", iconTextSize)) {
                            node.updateSavedState();
                        }
                        // draw the actual icon, with color based on input state
                        var iconTextColor = ImColor.rgba("#ffffff88");
                        if      (ImGui.isItemActive())  iconTextColor = ImColor.rgba("#ffff00ff");
                        else if (ImGui.isItemHovered()) iconTextColor = ImColor.rgba("#ffff0088");
                        drawList.addText(iconPanelPos, iconTextColor, iconText);
                    } else {
                        // use placeholder to reserve space for icon, because its not clickable without saved state
                        ImGui.dummy(iconTextSize);
                        // draw the faded icon
                        var iconTextColor = ImColor.rgba("#ffffff33");
                        drawList.addText(iconPanelPos, iconTextColor, iconText);
                    }
                }
                ImGui.sameLine(0, style.getItemInnerSpacingX());
                // - restore state icon and restore handling
                {
                    var iconText = FontAwesomeIcons.Redo;
                    iconTextSize = ImGui.calcTextSize(iconText);

                    ImGui.setItemAllowOverlap();
                    if (node.hasSavedState()) {
                        // use an invisible button in order to show the icon differently depending on input state
                        if (ImGui.invisibleButton("restore", iconTextSize)) {
                            node.restoreSavedState();
                            editor.restoreNodeState(node);
                            node.clearSavedState();
                        }
                        // draw the actual icon, with color based on input state
                        var iconTextColor = ImColor.rgba("#ffffff88");
                        if      (ImGui.isItemActive())  iconTextColor = ImColor.rgba("#ffff00ff");
                        else if (ImGui.isItemHovered()) iconTextColor = ImColor.rgba("#ffff0088");
                        drawList.addText(iconPanelPos, iconTextColor, iconText);
                    } else {
                        // use placeholder to reserve space for icon, because its not clickable without state to restore
                        ImGui.dummy(iconTextSize);
                        // draw the faded icon
                        var iconTextColor = ImColor.rgba("#ffffff33");
                        drawList.addText(iconPanelPos, iconTextColor, iconText);
                    }
                }

                ImGui.sameLine();
                ImGui.setItemAllowOverlap();
                ImGui.dummy(0, iconTextSize.y);

                ImGui.popID();
            }
            ImGui.unindent();

            // selection change details
            cursorScreenPos = ImGui.getCursorScreenPos();
            size = cursorScreenPos.plus(width, lineHeight);
            drawList.addRectFilled(cursorScreenPos, size, activeHeaderColor, lineHeight * 0.25f);
            ImGui.spacing(); ImGui.sameLine();
            ImGui.text("Selection");
            ImGui.beginChild("Selection Stats", size.set(width, 0));
            {
                float spacing = width / 2f;

                var pluralSuffix = (selectionChangeCount == 1) ? "" : "s";
                ImGui.text(STR."Changed \{selectionChangeCount} time\{pluralSuffix}");

                ImGui.sameLine(0, spacing);
                if (ImGui.button("Deselect All")) {
                    editor.clearSelection();
                }
            }
            ImGui.endChild();

            // selected objects
            ImGui.indent();
            {
                editor.getSelectedNodes().forEach(node -> ImGui.text(node.toString()));
                editor.getSelectedLinks().forEach(link -> ImGui.text(link.toString()));
            }
            ImGui.unindent();

            // trigger flow viz on keypress
            if (ImGui.isKeyPressed(ImGuiKey.Z)) {
                for (var link : editor.links) {
                    editor.flow(link);
                }
            }

            // update selection change count
            if (editor.hasSelectionChanged()) {
                selectionChangeCount++;
            }
        }
        ImGui.endChild();
    }
}
