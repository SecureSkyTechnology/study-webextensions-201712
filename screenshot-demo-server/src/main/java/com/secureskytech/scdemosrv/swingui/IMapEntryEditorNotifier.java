package com.secureskytech.scdemosrv.swingui;

import com.secureskytech.scdemosrv.proxy.LFSMapEntry;

public interface IMapEntryEditorNotifier {
    LFSMapEntry getSourceEntry();

    void notifyNewEntry(LFSMapEntry newEntry);
}
