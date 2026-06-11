/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.FontRenderer
 *  net.minecraft.client.gui.Gui
 *  net.minecraft.client.gui.GuiScreen
 */
package me.ksyz.accountmanager.gui;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;

public class GuiTextArea
extends Gui {
    private final FontRenderer fontRenderer;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private String text = "";
    private int maxStringLength = 32;
    private int cursorCounter;
    private boolean isFocused;
    private int cursorPosition = 0;
    private int scrollOffset = 0;

    public GuiTextArea(int id, FontRenderer fontRenderer, int x, int y, int width, int height) {
        this.fontRenderer = fontRenderer;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setMaxStringLength(int length) {
        this.maxStringLength = length;
        if (this.text.length() > length) {
            this.text = this.text.substring(0, length);
            this.cursorPosition = Math.min(this.cursorPosition, this.text.length());
        }
    }

    public String getText() {
        return this.text;
    }

    public void setText(String text) {
        this.text = text.length() > this.maxStringLength ? text.substring(0, this.maxStringLength) : text;
        this.cursorPosition = this.text.length();
    }

    public void setFocused(boolean focused) {
        this.isFocused = focused;
    }

    public void updateCursorCounter() {
        ++this.cursorCounter;
    }

    public void textboxKeyTyped(char typedChar, int keyCode) {
        if (!this.isFocused) {
            return;
        }
        if (GuiScreen.func_175278_g((int)keyCode)) {
            this.cursorPosition = this.text.length();
            return;
        }
        if (GuiScreen.func_175280_f((int)keyCode)) {
            GuiScreen.func_146275_d((String)this.text);
            return;
        }
        if (GuiScreen.func_175279_e((int)keyCode)) {
            this.writeText(GuiScreen.func_146277_j());
            return;
        }
        if (GuiScreen.func_175277_d((int)keyCode)) {
            GuiScreen.func_146275_d((String)this.text);
            this.text = "";
            this.cursorPosition = 0;
            return;
        }
        switch (keyCode) {
            case 14: {
                if (this.text.isEmpty() || this.cursorPosition <= 0) break;
                this.text = this.text.substring(0, this.cursorPosition - 1) + this.text.substring(this.cursorPosition);
                --this.cursorPosition;
                break;
            }
            case 211: {
                if (this.text.isEmpty() || this.cursorPosition >= this.text.length()) break;
                this.text = this.text.substring(0, this.cursorPosition) + this.text.substring(this.cursorPosition + 1);
                break;
            }
            case 199: {
                this.cursorPosition = 0;
                break;
            }
            case 207: {
                this.cursorPosition = this.text.length();
                break;
            }
            case 203: {
                if (this.cursorPosition <= 0) break;
                --this.cursorPosition;
                break;
            }
            case 205: {
                if (this.cursorPosition >= this.text.length()) break;
                ++this.cursorPosition;
                break;
            }
            case 28: {
                this.writeText("\n");
                break;
            }
            case 200: {
                if (this.scrollOffset <= 0) break;
                --this.scrollOffset;
                break;
            }
            case 208: {
                String[] lines = this.text.split("\\r?\\n", -1);
                int lineHeight = this.fontRenderer.field_78288_b + 2;
                int maxVisibleLines = (this.height - 4) / lineHeight;
                if (this.scrollOffset >= Math.max(0, lines.length - maxVisibleLines)) break;
                ++this.scrollOffset;
                break;
            }
            default: {
                if (!ChatAllowedCharacters.isAllowedCharacter(typedChar)) break;
                this.writeText(Character.toString(typedChar));
            }
        }
    }

    private void writeText(String text) {
        StringBuilder filtered = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c != '\n' && c != '\r' && !ChatAllowedCharacters.isAllowedCharacter(c)) continue;
            filtered.append(c);
        }
        String filteredStr = filtered.toString();
        int availableSpace = this.maxStringLength - this.text.length();
        if (availableSpace > 0) {
            if (filteredStr.length() > availableSpace) {
                filteredStr = filteredStr.substring(0, availableSpace);
            }
            this.text = this.text.substring(0, this.cursorPosition) + filteredStr + this.text.substring(this.cursorPosition);
            this.cursorPosition += filteredStr.length();
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        boolean wasClicked = mouseX >= this.x && mouseX < this.x + this.width && mouseY >= this.y && mouseY < this.y + this.height;
        this.setFocused(wasClicked);
        if (wasClicked && mouseButton == 0) {
            this.cursorPosition = this.text.length();
        }
    }

    public void drawTextBox() {
        GuiTextArea.func_73734_a((int)(this.x - 1), (int)(this.y - 1), (int)(this.x + this.width + 1), (int)(this.y + this.height + 1), (int)-6250336);
        GuiTextArea.func_73734_a((int)this.x, (int)this.y, (int)(this.x + this.width), (int)(this.y + this.height), (int)-16777216);
        String[] lines = this.text.split("\\r?\\n", -1);
        int lineHeight = this.fontRenderer.field_78288_b + 2;
        int maxVisibleLines = (this.height - 4) / lineHeight;
        int totalLines = lines.length;
        if (this.scrollOffset > Math.max(0, totalLines - maxVisibleLines)) {
            this.scrollOffset = Math.max(0, totalLines - maxVisibleLines);
        }
        int maxCharsPerLine = (this.width - 8) / (this.fontRenderer.field_78288_b / 2);
        int displayLineIndex = 0;
        for (int i = this.scrollOffset; i < lines.length && displayLineIndex < maxVisibleLines; ++i) {
            String line = lines[i];
            int lineStart = 0;
            while (lineStart < line.length() && displayLineIndex < maxVisibleLines) {
                int lineEnd = Math.min(lineStart + maxCharsPerLine, line.length());
                String displayPart = line.substring(lineStart, lineEnd);
                while (this.fontRenderer.func_78256_a(displayPart) > this.width - 8 && displayPart.length() > 0) {
                    displayPart = displayPart.substring(0, displayPart.length() - 1);
                }
                this.fontRenderer.func_78276_b(displayPart, this.x + 4, this.y + 4 + displayLineIndex * lineHeight, 0xE0E0E0);
                lineStart = lineEnd;
                ++displayLineIndex;
            }
            if (!line.isEmpty()) continue;
            ++displayLineIndex;
        }
        if (this.isFocused && this.cursorCounter / 6 % 2 == 0) {
            String textBeforeCursor = this.text.substring(0, Math.min(this.cursorPosition, this.text.length()));
            String[] linesBeforeCursor = textBeforeCursor.split("\\r?\\n", -1);
            int cursorLine = linesBeforeCursor.length - 1;
            String currentLine = linesBeforeCursor[linesBeforeCursor.length - 1];
            int cursorX = this.x + 4 + this.fontRenderer.func_78256_a(currentLine);
            int cursorY = this.y + 4 + (cursorLine - this.scrollOffset) * lineHeight;
            if (cursorLine >= this.scrollOffset && cursorLine < this.scrollOffset + maxVisibleLines) {
                GuiTextArea.func_73734_a((int)cursorX, (int)(cursorY - 1), (int)(cursorX + 1), (int)(cursorY + this.fontRenderer.field_78288_b), (int)-3092272);
            }
        }
    }

    private static class ChatAllowedCharacters {
        private ChatAllowedCharacters() {
        }

        static boolean isAllowedCharacter(char character) {
            return character >= ' ' && character != '\u007f';
        }
    }
}
