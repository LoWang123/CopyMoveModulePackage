package de.fau.copymoveforgerydetection.modules.ingestModule;
import org.openide.util.lookup.ServiceProvider;

import org.sleuthkit.autopsy.ingest.IngestModuleFactory;
import org.sleuthkit.autopsy.ingest.DataSourceIngestModule;
import org.sleuthkit.autopsy.ingest.FileIngestModule;
import org.sleuthkit.autopsy.ingest.IngestModuleGlobalSettingsPanel;
import org.sleuthkit.autopsy.ingest.IngestModuleIngestJobSettings;
import org.sleuthkit.autopsy.ingest.IngestModuleIngestJobSettingsPanel;

@ServiceProvider(service = IngestModuleFactory.class) // Sample is discarded at runtime 
public class CopyMoveIngestModuleFactory implements IngestModuleFactory {

    private static final String VERSION_NUMBER = "1.0.0";
    
    private static final String moduleName = "Copy Move Manipulation Detection";
    private static final String displayName = "Copy Move Manipulation Detection";
    private static final String description = "Detects Copy Move Manipulation in images. "
            + " This might take several minutes for a test on a 10+ megapixel image. "
            + "Please consider this _before_ starting the ingest process."
            + " If you run out of memory during the ingest operation. "
            + "Please adjust the heapsize of your JVM and consider "
            + "turning parallel mode off in both autopsy and this module.";
    
    private static final String resultAttributeTypeString = "ATTR_CM_RESULTIMAGE";
    private static final String resultAttributeDescription = "Copy Move Result Image";
    
    public static String getModuleName() {
        return moduleName;
    }

    @Override
    public String getModuleDisplayName() {
        return displayName;
    }

    @Override
    public String getModuleDescription() {
        return description;
    }

    @Override
    public String getModuleVersionNumber() {
        return VERSION_NUMBER;
    }

    @Override
    public boolean hasGlobalSettingsPanel() {
        return false;
    }

    @Override
    public IngestModuleGlobalSettingsPanel getGlobalSettingsPanel() {
        throw new UnsupportedOperationException();
    }

    @Override
    public IngestModuleIngestJobSettings getDefaultIngestJobSettings() {
        return new CopyMoveModuleIngestJobSettings();
    }

    @Override
    public boolean hasIngestJobSettingsPanel() {
        return true;
    }

    @Override
    public IngestModuleIngestJobSettingsPanel getIngestJobSettingsPanel(IngestModuleIngestJobSettings settings) {
        if (!(settings instanceof CopyMoveModuleIngestJobSettings)) {
            throw new IllegalArgumentException("Expected settings argument to be instanceof SampleModuleIngestJobSettings");
        }
        return new CopyMoveIngestModuleIngestJobSettingsPanel((CopyMoveModuleIngestJobSettings) settings);
    }

    @Override
    public boolean isDataSourceIngestModuleFactory() {
        return false;
    }

    @Override
    public DataSourceIngestModule createDataSourceIngestModule(IngestModuleIngestJobSettings settings) {
        throw new UnsupportedOperationException("Copy Move is not a Data Source Module.");
    }

    @Override
    public boolean isFileIngestModuleFactory() {
        return true;
    }

    @Override
    public FileIngestModule createFileIngestModule(IngestModuleIngestJobSettings settings) {
        if (!(settings instanceof CopyMoveModuleIngestJobSettings)) {
            throw new IllegalArgumentException("Expected settings argument to be instanceof SampleModuleIngestJobSettings");
        }
        return new CopyMoveIngestModule((CopyMoveModuleIngestJobSettings) settings);
    }

    public static String getResultAttributeTypeString() {
        return resultAttributeTypeString;
    }

    public static String getResultAttributeDescription() {
        return resultAttributeDescription;
    }
    
    
}
