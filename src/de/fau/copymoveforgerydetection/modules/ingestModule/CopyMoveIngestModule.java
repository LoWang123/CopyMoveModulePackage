package de.fau.copymoveforgerydetection.modules.ingestModule;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.casemodule.NoCurrentCaseException;
import org.sleuthkit.autopsy.coreutils.Logger;
import org.sleuthkit.autopsy.ingest.FileIngestModule;
import org.sleuthkit.autopsy.ingest.IngestModule;
import org.sleuthkit.autopsy.ingest.IngestJobContext;
import org.sleuthkit.autopsy.ingest.IngestServices;
import org.sleuthkit.datamodel.AbstractFile;
import org.sleuthkit.datamodel.Blackboard;
import org.sleuthkit.datamodel.BlackboardArtifact;
import org.sleuthkit.datamodel.BlackboardAttribute;
import org.sleuthkit.datamodel.TskCoreException;
import org.sleuthkit.datamodel.TskData;
import de.fau.copymoveforgerydetection.copymove.RadixSortDetection;
import java.util.ArrayList;
import org.sleuthkit.autopsy.ingest.IngestMessage;

/**
 * Sample file ingest module that doesn't do much. Demonstrates per ingest job
 * module settings, use of a subset of the available ingest services and
 * thread-safe sharing of per ingest job data.
 */
class CopyMoveIngestModule implements FileIngestModule {

    private static final Logger LOGGER
            = Logger.getLogger(CopyMoveIngestModuleFactory.getModuleName());
    private static final String SET_NAME = "Copy Move Forgeries";

    // Custom BYTE-typed attribute that holds the result image.
    // (TSK_VALUE is a numeric type and rejects byte[] values in current Sleuth Kit.)
    private static BlackboardAttribute.Type resultImageType = null;
    private IngestJobContext context = null;
    
    private final int blockSize = 10;    
    private final int regionMinSize;
    
    

    CopyMoveIngestModule(CopyMoveModuleIngestJobSettings settings) {
        this.regionMinSize = settings.getRegionMinSize();
    }

    @Override
    public void startUp(IngestJobContext context) throws IngestModuleException {
        this.context = context;

        synchronized (CopyMoveIngestModule.class) {
            if (resultImageType == null) {
                try {
                    Blackboard blackboard = Case.getCurrentCaseThrows()
                            .getSleuthkitCase().getBlackboard();
                    // Returns the existing type if already registered, otherwise creates it.
                    resultImageType = blackboard.getOrAddAttributeType(
                            CopyMoveIngestModuleFactory.getResultAttributeTypeString(),
                            BlackboardAttribute.TSK_BLACKBOARD_ATTRIBUTE_VALUE_TYPE.BYTE,
                            CopyMoveIngestModuleFactory.getResultAttributeDescription());
                } catch (NoCurrentCaseException | Blackboard.BlackboardException ex) {
                    LOGGER.log(Level.SEVERE, "Failed to create blackboard attribute", ex);
                    resultImageType = null;
                    throw new IngestModuleException("Failed to create blackboard attribute: "
                            + ex.getLocalizedMessage(), ex);
                }
            }
        }
    }

    @Override
    public IngestModule.ProcessResult process(AbstractFile file) {
        if (!isProcessExecutable()) {
            return IngestModule.ProcessResult.ERROR;
        }
        if (!isFileSupported(file)) {
            return IngestModule.ProcessResult.OK;
        }
        try {
            return processCore(file);
        } catch (Exception ex) {
            // Covers TskCoreException, IOException, BlackboardException,
            // NoCurrentCaseException and unexpected runtime errors.
            LOGGER.log(Level.SEVERE, "Error processing file (id = " + file.getId() + ")", ex);
            return IngestModule.ProcessResult.ERROR;
        } catch (OutOfMemoryError err) {
            LOGGER.log(Level.SEVERE, "Out of memory processing file (id = " + file.getId()
                    + "). Increase the Autopsy heap size or reduce the number of ingest threads.", err);
            return IngestModule.ProcessResult.ERROR;
        }
    }

    private boolean isProcessExecutable() {
        return resultImageType != null;
    }
    
    private boolean isFileSupported(AbstractFile file) {
         if ((file.getType() == TskData.TSK_DB_FILES_TYPE_ENUM.UNALLOC_BLOCKS)
                || (file.getType() == TskData.TSK_DB_FILES_TYPE_ENUM.UNUSED_BLOCKS)
                || (file.isFile() == false)
                || !ImageFileParser.isSupportedImageFile(file)) {
            return false;
        }
        return true;
    }
    
    private IngestModule.ProcessResult processCore(AbstractFile file)
            throws IOException, TskCoreException, NoCurrentCaseException, Blackboard.BlackboardException {
        BufferedImage image = ImageFileParser.parseAbstractFile(file);
        if (image == null) {
            // ImageIO could not decode the file (corrupt or unsupported variant).
            return IngestModule.ProcessResult.OK;
        }
        RadixSortDetection rsd = new RadixSortDetection();
        rsd.setBlockSideLength(this.blockSize);
        rsd.setRegionMinSize(this.regionMinSize);
        rsd.setParallelize(false);
        rsd.setInputImage(image);
        rsd.run();
        
        BufferedImage resultImage = rsd.getResultImage();
        int numberOfCopiedRegions = rsd.getResultList().size();
        
        
        // Only report files in which something was actually found.
        if (numberOfCopiedRegions > 0) {
            submitResult(file, image, resultImage);
            reportBlackboardCopiedRegionsCount(file.getName(), numberOfCopiedRegions);
        }

        return IngestModule.ProcessResult.OK;
    }
    
    @Override
    public void shutDown() {
    }
    
    private void submitResult(
            AbstractFile file,
            BufferedImage originalImage,
            BufferedImage resultImage)
            throws TskCoreException, IOException, NoCurrentCaseException, Blackboard.BlackboardException {

        String moduleName = CopyMoveIngestModuleFactory.getModuleName();
        byte[] resultImageBytes = convertImageToByteArray(resultImage, ImageFileParser.findImageType(file));

        ArrayList<BlackboardAttribute> attributes = new ArrayList<BlackboardAttribute>();
        attributes.add(new BlackboardAttribute(resultImageType, moduleName, resultImageBytes));
        attributes.add(new BlackboardAttribute(
                BlackboardAttribute.ATTRIBUTE_TYPE.TSK_COMMENT,
                moduleName,
                file.getName() + "_CopyMoveResult." + file.getNameExtension()));
        // Interesting-item artifacts are grouped by set name in the results tree;
        // without it they may not be displayed.
        attributes.add(new BlackboardAttribute(
                BlackboardAttribute.ATTRIBUTE_TYPE.TSK_SET_NAME,
                moduleName,
                SET_NAME));

        BlackboardArtifact artifact = file.newArtifact(BlackboardArtifact.ARTIFACT_TYPE.TSK_INTERESTING_FILE_HIT);
        artifact.addAttributes(attributes);

        // Indexes the artifact and notifies the UI (replaces manual ModuleDataEvent firing).
        Case.getCurrentCaseThrows().getSleuthkitCase().getBlackboard()
                .postArtifact(artifact, moduleName);
    }

    private byte[] convertImageToByteArray(BufferedImage image, String extension)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (!ImageIO.write(image, extension, baos)) {
            throw new IOException("No ImageIO writer available for format '" + extension + "'");
        }
        return baos.toByteArray();
    }

   
    synchronized static void reportBlackboardCopiedRegionsCount(String imageName, int numberOfCopiedRegions) {
        String messageString = "";
        
        if(numberOfCopiedRegions == 1) {
            messageString = "Found " + numberOfCopiedRegions + " copied region in " + imageName;
        }
        else {
            messageString = "Found " + numberOfCopiedRegions + " copied regions in " + imageName;
        }
        
        IngestMessage message = IngestMessage.createMessage(
                IngestMessage.MessageType.INFO,
                CopyMoveIngestModuleFactory.getModuleName(),
                messageString);
        IngestServices.getInstance().postMessage(message);
    }
}
