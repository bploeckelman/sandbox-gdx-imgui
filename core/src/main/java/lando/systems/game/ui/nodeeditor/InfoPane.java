package lando.systems.game.ui.nodeeditor;

import imgui.ImColor;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiWindowFlags;
import lando.systems.game.Util;
import lando.systems.game.shared.FontAwesomeIcons;

public class InfoPane {

    final CanvasNodeEditor editor;

    final ImVec2 size = new ImVec2();

    int selectionChangeCount = 0;

    public InfoPane(CanvasNodeEditor editor) {
        this.editor = editor;
    }

    public void render() {
        var io = ImGui.getIO();
        var style = ImGui.getStyle();
        var iconPanelPos = new ImVec2();
        var activeHeaderColor = ImColor.rgba(style.getColors()[ImGuiCol.HeaderActive]);

        if (ImGui.begin("Information", ImGuiWindowFlags.MenuBar)) {
            var drawList = ImGui.getWindowDrawList();

            if (ImGui.beginMenuBar()) {
                float width = ImGui.getContentRegionAvailX();
                float spacing = width / 3f;

                if (ImGui.button(STR."\{FontAwesomeIcons.SearchPlus}Zoom to content ")) {
                    editor.zoomToContent();
                }

                ImGui.sameLine(style.getItemInnerSpacingX(), spacing);

                if (ImGui.button("Show flow")) {
                    for (var link : editor.links) {
                        editor.flow(link);
                    }
                }

                ImGui.sameLine(style.getItemInnerSpacingX(), spacing);

                ImGui.checkbox("Show ordinals", editor.showOrdinals);
            }
            ImGui.endMenuBar();

            // node detail rows for each node
            // - background highlight for header
            var lineHeight = ImGui.getTextLineHeight();
            var cursorScreenPos = ImGui.getCursorScreenPos();
            drawList.addRectFilled(
                cursorScreenPos.x, cursorScreenPos.y,
                cursorScreenPos.x + ImGui.getWindowWidth(),
                cursorScreenPos.y + lineHeight,
                activeHeaderColor, lineHeight * 0.25f);

            // - header text
            ImGui.spacing(); ImGui.sameLine();
            ImGui.text("Nodes");

            // - node detail rows
            ImGui.indent();
            for (var node : editor.nodes) {
                ImGui.pushID(node.pointerId);
                var start = ImGui.getCursorScreenPos();

                // node detail row: show 'just touched' indicator indicator that fades over time
                float progress = editor.getTouchProgress(node);
                if (progress > 0) {
                    ImGui.getWindowDrawList().addLine(
                        start.x - 8f, start.y,
                        start.x - 8f, start.y + lineHeight,
                        ImColor.rgba(1f, 0f, 0f, 1f - progress), 4f);
                }

                // node detail row: name and handle selection input, including multi-select
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

                    // focus on the selection in the editor
                    editor.navigateToSelection();
                }

                // node detail row: on hover - show node state in tooltip
                if (ImGui.isItemHovered() && node.hasState()) {
                    ImGui.setTooltip(STR."State: \{node.getState()}");
                }

                // node detail row: pointer id text
                int numIcons = 2;
                var iconSize = lineHeight;
                var pointerIdText = STR."(\{node.pointerId})";
                var textSize = ImGui.calcTextSize(pointerIdText);
                var edgeIconBottom = (lineHeight - iconSize) / 2f;
                var edgeIconLeft = ImGui.getWindowWidth()
                    - style.getFramePaddingX()
                    - style.getIndentSpacing()
                    - style.getItemInnerSpacingX()
                    - (numIcons * iconSize);
                iconPanelPos.set(start.x + edgeIconLeft, start.y + edgeIconBottom);

                float edgeTextLeft = iconPanelPos.x - textSize.x - style.getItemInnerSpacingX();
                drawList.addText(edgeTextLeft, start.y, Util.IM_COLOR_WHITE, pointerIdText);

                // node detail row: icons
                ImVec2 textIconSize;
                ImGui.setCursorScreenPos(iconPanelPos);
                // - save state icon and save handling
                {
                    var textIcon = FontAwesomeIcons.Save;
                    textIconSize = ImGui.calcTextSize(textIcon);

                    ImGui.setItemAllowOverlap();
                    if (!node.hasSavedState()) {
                        // use an invisible button in order to show the icon differently depending on input state
                        if (ImGui.invisibleButton("save", textIconSize)) {
                            node.updateSavedState();
                        }
                        // draw the actual 'icon', with color based on input state
                        var iconTextColor = ImColor.rgba("#ffffff88");
                        if      (ImGui.isItemActive())  iconTextColor = ImColor.rgba("#ffff00ff");
                        else if (ImGui.isItemHovered()) iconTextColor = ImColor.rgba("#ffff0088");
                        drawList.addText(iconPanelPos, iconTextColor, textIcon);
                    } else {
                        // use placeholder to reserve space for icon, because its not clickable without saved state
                        ImGui.dummy(textIconSize);
                        // draw the faded 'icon'
                        var iconTextColor = ImColor.rgba("#ffffff33");
                        drawList.addText(iconPanelPos, iconTextColor, textIcon);
                    }
                }
                // - restore state icon and restore handling
                ImGui.sameLine(0, style.getItemInnerSpacingX());
                {
                    var iconText = FontAwesomeIcons.Redo;
                    textIconSize = ImGui.calcTextSize(iconText);

                    ImGui.setItemAllowOverlap();
                    if (node.hasSavedState()) {
                        // use an invisible button in order to show the icon differently depending on input state
                        if (ImGui.invisibleButton("restore", textIconSize)) {
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
                        ImGui.dummy(textIconSize);
                        // draw the faded icon
                        var iconTextColor = ImColor.rgba("#ffffff33");
                        drawList.addText(iconPanelPos, iconTextColor, iconText);
                    }
                }

                ImGui.sameLine();
                ImGui.setItemAllowOverlap();
                ImGui.dummy(0, textIconSize.y);

                ImGui.popID();
            }
            ImGui.unindent();

            // selection change details
            lineHeight = ImGui.getTextLineHeight();
            cursorScreenPos = ImGui.getCursorScreenPos();
            drawList.addRectFilled(
                cursorScreenPos.x, cursorScreenPos.y,
                cursorScreenPos.x + ImGui.getWindowWidth(),
                cursorScreenPos.y + lineHeight,
                activeHeaderColor, lineHeight * 0.25f);

            // - header text
            ImGui.spacing(); ImGui.sameLine();
            ImGui.text("Selection");

            // - selection change details
            ImGui.indent();
            {
                float spacing = ImGui.getWindowWidth() / 2f;

                var pluralSuffix = (selectionChangeCount == 1) ? "" : "s";
                ImGui.text(STR."Changed \{selectionChangeCount} time\{pluralSuffix}");

                ImGui.sameLine(0, spacing);
                if (ImGui.button("Deselect All")) {
                    editor.clearSelection();
                }
            }
            ImGui.unindent();

            // - selected objects
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
        ImGui.end();
    }
}
