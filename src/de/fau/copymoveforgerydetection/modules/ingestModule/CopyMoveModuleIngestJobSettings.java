package de.fau.copymoveforgerydetection.modules.ingestModule;

import org.sleuthkit.autopsy.ingest.IngestModuleIngestJobSettings;

public class CopyMoveModuleIngestJobSettings implements IngestModuleIngestJobSettings {

    private int regionMinSize = 437;

    CopyMoveModuleIngestJobSettings() {
        
    }

    CopyMoveModuleIngestJobSettings(int regionMinSize) {
        this.regionMinSize = regionMinSize;
    }

    @Override
    public long getVersionNumber() {
        return 1l;
    }

    public int getRegionMinSize() {
        return regionMinSize;
    }

    public void setRegionMinSize(int regionMinSize) {
        this.regionMinSize = regionMinSize;
    }
}
