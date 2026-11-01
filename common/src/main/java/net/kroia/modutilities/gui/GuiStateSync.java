package net.kroia.modutilities.gui;

import net.kroia.modutilities.gui.elements.base.GuiElement;

import java.util.List;

public class GuiStateSync {

    public static void syncState(Gui source, Gui target) {
        syncChildren(source.getElements(), target.getElements(), null);
    }

    public static void syncDisplayState(Gui source, Gui target) {
        syncChildren(source.getElements(), target.getElements(), GuiElement.SyncCategory.DISPLAY);
    }

    public static void syncInputState(Gui source, Gui target) {
        syncChildren(source.getElements(), target.getElements(), GuiElement.SyncCategory.INPUT);
    }

    public static void syncDirtyState(Gui source, Gui target) {
        syncChildrenDirty(source.getElements(), target.getElements(), null);
    }

    private static void syncChildren(List<GuiElement> srcList, List<GuiElement> tgtList,
                                      GuiElement.SyncCategory filter) {
        for (int i = 0; i < srcList.size(); i++) {
            GuiElement src = srcList.get(i);
            GuiElement tgt = findMatch(src, tgtList, i);
            if (tgt != null) {
                if (filter == null || src.getSyncCategory() == filter) {
                    tgt.deserializeState(src.serializeState());
                } else {
                    if (src.isEnabled() != tgt.isEnabled()) {
                        tgt.setEnabled(src.isEnabled());
                    }
                }
                syncChildren(src.getChilds(), tgt.getChilds(), filter);
            }
        }
    }

    private static void syncChildrenDirty(List<GuiElement> srcList, List<GuiElement> tgtList,
                                           GuiElement.SyncCategory filter) {
        for (int i = 0; i < srcList.size(); i++) {
            GuiElement src = srcList.get(i);
            GuiElement tgt = findMatch(src, tgtList, i);
            if (tgt != null) {
                if (src.isDirty() && (filter == null || src.getSyncCategory() == filter)) {
                    tgt.deserializeState(src.serializeState());
                } else if (src.isDirty()) {
                    if (src.isEnabled() != tgt.isEnabled()) {
                        tgt.setEnabled(src.isEnabled());
                    }
                }
                syncChildrenDirty(src.getChilds(), tgt.getChilds(), filter);
            }
        }
    }

    private static GuiElement findMatch(GuiElement src, List<GuiElement> tgtList, int fallbackIndex) {
        String srcId = src.getId();
        if (srcId != null) {
            for (GuiElement tgt : tgtList) {
                if (srcId.equals(tgt.getId())) return tgt;
            }
        }
        if (fallbackIndex < tgtList.size()) return tgtList.get(fallbackIndex);
        return null;
    }
}
