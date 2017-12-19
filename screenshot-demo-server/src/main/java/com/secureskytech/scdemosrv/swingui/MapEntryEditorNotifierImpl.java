package com.secureskytech.scdemosrv.swingui;

import java.awt.EventQueue;
import java.util.Objects;

import com.secureskytech.scdemosrv.proxy.LFSMapEntry;

public class MapEntryEditorNotifierImpl implements IMapEntryEditorNotifier {
    private final MainPanel mainPanelRef;
    private final int selectedIndex;
    private final LFSMapEntry selectedEntry;

    public MapEntryEditorNotifierImpl(MainPanel mainPanelRef) {
        this.mainPanelRef = mainPanelRef;
        this.selectedIndex = -1;
        this.selectedEntry = null;
    }

    public MapEntryEditorNotifierImpl(MainPanel mainPanelRef, int selectedIndex, LFSMapEntry selectedEntry) {
        this.mainPanelRef = mainPanelRef;
        this.selectedIndex = selectedIndex;
        this.selectedEntry = selectedEntry;
    }

    @Override
    public LFSMapEntry getSourceEntry() {
        return selectedEntry;
    }

    @Override
    public void notifyNewEntry(LFSMapEntry newEntry) {
        if (Objects.isNull(selectedEntry)) {
            // "add" button
            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    mainPanelRef.addLFSMapEntry(newEntry);
                }
            });
            return;
        }
        if (selectedIndex < 0) {
            // illegal state
        }
        // "edit" button
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                mainPanelRef.updateLFSMapEntry(newEntry, selectedIndex);
            }
        });
        return;
    }
}
