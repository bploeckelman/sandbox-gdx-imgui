package lando.systems.game.ui.nodeeditor.panels;

import com.github.tommyettinger.textra.Font;
import imgui.ImColor;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiHoveredFlags;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiSelectableFlags;
import imgui.flag.ImGuiWindowFlags;
import lando.systems.game.Util;
import lando.systems.game.shared.FontAwesomeIcons;
import lando.systems.game.ui.ImGuiUtil;
import lando.systems.game.ui.nodeeditor.BlueprintEditor;
import lando.systems.game.ui.nodeeditor.objects.Link;
import lando.systems.game.ui.nodeeditor.objects.Node;
import lando.systems.game.ui.nodeeditor.objects.Pin;

import java.util.stream.Collectors;

public class InfoPane {

    private final BlueprintEditor editor;

    private int selectionChangeCount = 0;

    public InfoPane(BlueprintEditor editor) {
        this.editor = editor;
    }

    public void update() {
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

    public void render() {
        var style = ImGui.getStyle();
        var activeHeaderColor = ImColor.rgba(style.getColors()[ImGuiCol.HeaderActive]);

        int infoWindowFlags = ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoTitleBar;
        if (ImGui.begin("Information", infoWindowFlags)) {
            var drawList = ImGui.getWindowDrawList();

            // tool buttons section
            {
                float spacing = style.getItemInnerSpacingX();
                float padding = style.getFramePaddingX();
                float buttonWidth = (ImGui.getWindowWidth() - (3 * spacing) - (2 * padding)) / 2f;

                // new row for load/save buttons
                if (ImGui.button(STR."\{FontAwesomeIcons.FolderOpen}Load ", buttonWidth, 0)) {
                    editor.load();
                }
                ImGui.sameLine(0, spacing);
                if (ImGui.button(STR."\{FontAwesomeIcons.Save}Save ", buttonWidth, 0)) {
                    editor.save();
                }

                // new row for zoom and flow buttons
                if (ImGui.button(STR."\{FontAwesomeIcons.SearchLocation}Zoom ", buttonWidth, 0)) {
                    editor.zoomToContent();
                }
                ImGui.sameLine(0, spacing);
                if (ImGui.button(STR."\{FontAwesomeIcons.FillDrip}Show flow ", buttonWidth, 0)) {
                    for (var link : editor.links) {
                        editor.flow(link);
                    }
                }

                // new row for repo link
                float buttonFullWidth = ImGui.getWindowWidth() - (2 * spacing) - (2 * padding);
                var libraryRepoUrl = STR."\{FontAwesomeIcons.ExternalLinkAlt}\{FontAwesomeIcons.CodeBranch}\{BlueprintEditor.REPO} ";
                if (ImGui.button(libraryRepoUrl, buttonFullWidth, 0)) {
                    Util.openUrl(BlueprintEditor.URL);
                }

                // new rows for setting toggles
                ImGui.spacing();
                ImGui.checkbox("Show ordinals", editor.showOrdinals);
            } // end section: tool buttons

            ImGui.spacing();
            ImGui.separator();

            // node detail rows section
            {
                // heading
                ImGuiUtil.rectFilled(ImGui.getContentRegionAvailX(), ImGui.getTextLineHeight(), activeHeaderColor, 0.25f);
                ImGui.spacing();
                ImGui.sameLine();
                ImGuiUtil.textCentered("Nodes");

                ImGui.indent();
                for (var node : editor.nodes) {
//                    renderNodeDetailRow(node);
                    renderNodeDetailRow2(node);
                }
                ImGui.unindent();
            } // end section: node detail rows

            ImGui.spacing();
            ImGui.separator();

            // selection change details
            {
                // heading
                ImGuiUtil.rectFilled(ImGui.getContentRegionAvailX(), ImGui.getTextLineHeight(), activeHeaderColor, 0.25f);
                ImGui.spacing();
                ImGui.sameLine();
                ImGui.text("Selection");

                if (ImGui.button("Deselect All", ImGui.getContentRegionAvailX(), 0)) {
                    editor.clearSelection();
                }

                // selection change details
                var plural = (selectionChangeCount == 1) ? "" : "s";
                var text = STR."Current selection: (changed \{selectionChangeCount} time\{plural})";
                ImGui.text(text);

                ImGui.indent();
                {
                    editor.getSelectedNodes().forEach(node -> {
                        ImGui.bulletText(STR."\{node.toString()} (\{node.pointerId})");
                        if (ImGui.isItemHovered()) {
                            ImGui.setTooltip(STR."""
                            Node: '\{node.name}'_\{node.id}#\{node.pointerId}

                            Inputs:
                              \{node.inputs.stream().map(Pin::toString).collect(Collectors.joining("\r\n  "))}

                            Outputs:
                              \{node.outputs.stream().map(Pin::toString).collect(Collectors.joining("\r\n  "))}
                            """);
                        }
                    });
                    editor.getSelectedLinks().forEach(link -> {
                        ImGui.bulletText(STR."\{link.name()} (\{link.pointerId})");
                        if (ImGui.isItemHovered()) {
                            ImGui.setTooltip(STR."""
                            Link: '\{link.name()}'_\{link.id}#\{link.pointerId}

                            Source: \{link.source.toString()}
                            Target: \{link.target.toString()}
                            """);
                        }
                    });
                }
                ImGui.unindent();
            } // end section: selection change details

            // metrics window toggle
            {
                // vertical gap to bottom-align the checkbox
                float itemSpace = style.getItemSpacingY() + style.getItemInnerSpacingY();
                float frameSpace = style.getFramePaddingY() + style.getFrameBorderSize();
                ImGui.dummy(0, ImGui.getContentRegionAvailY() - (ImGui.getTextLineHeight() + itemSpace + frameSpace));

                ImGui.checkbox("Show metrics window", editor.showMetricsWindow);
                if (editor.showMetricsWindow.get()) {
                    ImGui.showMetricsWindow();
                }
            } // end section: metrics window toggle
        }
        ImGui.end();
    }

    private void renderNodeDetailRow(Node node) {
        ImGui.pushID(node.pointerId);
        var drawList = ImGui.getWindowDrawList();
        var start = ImGui.getCursorScreenPos();
        var width = ImGui.getContentRegionAvailX();
        var lineHeight = ImGui.getTextLineHeight();
        var style = ImGui.getStyle();
        var iconPanelPos = new ImVec2();

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
            if (ImGui.getIO().getKeyCtrl()) {
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
        var styleSpace = style.getIndentSpacing() + style.getItemInnerSpacingX();
        var edgeIconLeft = width - styleSpace - (numIcons * iconSize);
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
        ImGui.sameLine(0, style.getItemSpacingX());
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
                var iconTextColor = ImColor.rgba("#ffffff88");
                drawList.addText(iconPanelPos, iconTextColor, iconText);
            }
        }

        ImGui.sameLine();
        ImGui.setItemAllowOverlap();
        ImGui.dummy(0, textIconSize.y);

        ImGui.popID();
    }

    private void renderNodeDetailRow2(Node node) {
        ImGui.pushID(node.pointerId);
        var drawList = ImGui.getWindowDrawList();
        var start = ImGui.getCursorScreenPos();
        var width = ImGui.getContentRegionAvailX();
        var lineHeight = ImGui.getTextLineHeight();
        var style = ImGui.getStyle();
        var iconPanelPos = new ImVec2();

        // node detail row: show 'just touched' indicator indicator that fades over time
        float progress = editor.getTouchProgress(node);
        if (progress > 0) {
            drawList.addLine(
                start.x - 8f, start.y,
                start.x - 8f, start.y + lineHeight,
                ImColor.rgba(1f, 0f, 0f, 1f - progress), 4f);
        }

        // node detail row: name and handle selection input, including multi-select
        var isSelected = editor.isSelected(node);
        if (ImGui.selectable(node.toLabel(), isSelected, ImGuiSelectableFlags.SpanAllColumns, 0, 0)) {
            if (ImGui.getIO().getKeyCtrl()) {
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
        var pointerIdTextSize = ImGui.calcTextSize(pointerIdText);
        var edgeIconBottom = (lineHeight - iconSize) / 2f;
        var styleSpace = style.getIndentSpacing() + style.getItemInnerSpacingX();
        var edgeIconLeft = width - styleSpace - (numIcons * iconSize);
        iconPanelPos.set(start.x + edgeIconLeft, start.y + edgeIconBottom);

        float edgeTextLeft = iconPanelPos.x - pointerIdTextSize.x - style.getItemInnerSpacingX();
        drawList.addText(edgeTextLeft, start.y, Util.IM_COLOR_GRAY, pointerIdText);

        float iconLeftSpace = style.getItemSpacingX();
        float indentWidth = style.getIndentSpacing();
        float offsetFromX = edgeTextLeft + pointerIdTextSize.x + iconLeftSpace;
        ImGui.sameLine(offsetFromX, 0);
        iconPanelPos.set(ImGui.getCursorScreenPos());
        iconPanelPos.x -= indentWidth / 2f;
        renderIcon(FontAwesomeIcons.Save, "save", iconPanelPos, node, node::updateSavedState);

        float iconWidth = ImGui.calcTextSize(FontAwesomeIcons.Save).x;
        ImGui.sameLine(offsetFromX - indentWidth + iconWidth, iconWidth);
        iconPanelPos.set(ImGui.getCursorScreenPos());
        iconPanelPos.x -= indentWidth / 2f;
        renderIcon(FontAwesomeIcons.RedoAlt, "restore", iconPanelPos, node, () -> {
            node.restoreSavedState();
            editor.restoreNodeState(node);
            node.clearSavedState();
        });

        ImGui.popID();
    }

    private void renderIcon(String icon, String buttonId, ImVec2 iconPanelPos, Node node, Runnable onClick) {
        var drawList = ImGui.getWindowDrawList();
        var iconSize = ImGui.calcTextSize(icon);
        int color;

        ImGui.setItemAllowOverlap();
        if (node.hasSavedState()) {
            if (ImGui.invisibleButton(buttonId, iconSize)) {
                onClick.run();
            }

            color = ImColor.rgba("#ffffff88");
            if      (ImGui.isItemActive())  color = ImColor.rgba("#ffff00ff");
            else if (ImGui.isItemHovered()) color = ImColor.rgba("#ffff0088");

            drawList.addText(iconPanelPos, color, icon);
//            ImGui.sameLine();
//            ImGui.textColored(color, icon);
        } else {
            // Placeholder for non-clickable state
            ImGui.dummy(0, 0);

            color = ImColor.rgba("#ffffff33");
            drawList.addText(iconPanelPos, color, icon);
//            ImGui.sameLine();
//            ImGui.textColored(color, icon);
        }
    }
}
